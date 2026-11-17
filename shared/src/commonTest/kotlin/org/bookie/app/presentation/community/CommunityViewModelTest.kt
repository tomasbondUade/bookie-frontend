package org.bookie.app.presentation.community

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.model.Community
import org.bookie.app.domain.model.CommunityPage
import org.bookie.app.domain.model.CommunityPost
import org.bookie.app.domain.repository.AuthRepository
import org.bookie.app.domain.repository.CommunityRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.GetCommunities
import org.bookie.app.domain.usecase.GetCommunityPosts
import org.bookie.app.domain.usecase.GetRecommendedCommunities
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityViewModelTest {
    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun publicListingCanLoadAndPaginate() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.loadCommunities()
        runCurrent()
        val first = assertIs<CommunityCatalogState.Ready>(vm.catalog.value)
        assertEquals(listOf("c1"), first.communities.map { it.id })
        assertTrue(first.hasMore)

        vm.loadMoreCommunities()
        runCurrent()
        val second = assertIs<CommunityCatalogState.Ready>(vm.catalog.value)
        assertEquals(listOf("c1", "c2"), second.communities.map { it.id })
        assertEquals(false, second.hasMore)
        assertEquals(listOf(1, 2), repo.requestedPages)
    }

    @Test
    fun publicModeNeverRequestsPersonalRecommendations() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.loadRecommendations()
        runCurrent()
        assertEquals(CommunityRecommendationsState.SignInRequired, vm.recommendations.value)
        assertEquals(0, repo.recommendationCalls)
    }

    @Test
    fun signedInLoadsPersonalRecommendationsAndShowsPosts() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo, FakeAuth())
        vm.loadRecommendations()
        runCurrent()
        val recommendations =
            assertIs<CommunityRecommendationsState.Ready>(vm.recommendations.value)
        assertEquals("c1", recommendations.communities.single().id)
        assertEquals(1, repo.recommendationCalls)

        vm.selectCommunity(recommendations.communities.single())
        runCurrent()
        val posts = assertIs<CommunityPostsState.Ready>(vm.posts.value)
        assertEquals("Hola", posts.posts.single().title)
        vm.showCommunities()
        assertEquals(null, vm.selected.value)
        assertEquals(CommunityPostsState.Idle, vm.posts.value)
    }

    @Test
    fun errorsAndEmptyStatesAreNotInventedContent() = runTest {
        val repo = FakeRepository()
        repo.failPosts = true
        val vm = viewModel(repo)
        vm.loadCommunities()
        runCurrent()
        val listing = assertIs<CommunityCatalogState.Ready>(vm.catalog.value)
        vm.selectCommunity(listing.communities.first())
        runCurrent()
        assertIs<CommunityPostsState.Error>(vm.posts.value)

        repo.failPosts = false
        repo.emptyPosts = true
        vm.refreshPosts()
        runCurrent()
        assertEquals(CommunityPostsState.Empty, vm.posts.value)
    }

    private fun viewModel(repo: CommunityRepository, auth: AuthRepository? = null) =
        CommunityViewModel(
            getCommunities = GetCommunities(repo),
            getRecommended = GetRecommendedCommunities(repo),
            getPosts = GetCommunityPosts(repo),
            authRepository = auth,
        )

    private fun community(id: String) = Community(
        id = id, name = "Lectores", description = "Sobre libros",
        tags = listOf("Fantasía"), imageUrl = null, ownerId = "user-1",
        createdAt = "2026-11-17T10:00:00Z",
    )

    private inner class FakeRepository : CommunityRepository {
        val requestedPages = mutableListOf<Int>()
        var recommendationCalls = 0
        var failPosts = false
        var emptyPosts = false

        override suspend fun listCommunities(page: Int, limit: Int): Resource<CommunityPage> {
            requestedPages += page
            val entries = if (page == 1) listOf(community("c1")) else listOf(community("c2"))
            return Resource.Success(
                CommunityPage(entries, page, limit, total = 2, totalPages = 2),
            )
        }

        override suspend fun recommended(): Resource<List<Community>> {
            recommendationCalls++
            return Resource.Success(listOf(community("c1")))
        }

        override suspend fun posts(communityId: String, limit: Int): Resource<List<CommunityPost>> {
            if (failPosts) return Resource.Error("Sin conexión")
            if (emptyPosts) return Resource.Success(emptyList())
            return Resource.Success(listOf(CommunityPost(
                id = "p1", title = "Hola", content = "Contenido",
                communityId = communityId, createdAt = "2026-11-17T11:00:00Z",
                authorName = "Lectora",
            )))
        }
    }

    private class FakeAuth : AuthRepository {
        override val state: StateFlow<AuthState> =
            MutableStateFlow(AuthState.SignedIn("user-1", "lectora@example.test"))
        override suspend fun restoreSession(): AuthState = state.value
        override suspend fun signIn(email: String, password: String): AuthState = state.value
        override suspend fun signUp(email: String, password: String, username: String): AuthState =
            state.value
        override suspend fun signOut(): AuthState = AuthState.SignedOut
    }
}
