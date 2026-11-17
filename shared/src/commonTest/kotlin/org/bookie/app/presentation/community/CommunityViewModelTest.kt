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
import org.bookie.app.domain.model.CreateCommunityInput
import org.bookie.app.domain.model.PostAuthor
import org.bookie.app.domain.repository.AuthRepository
import org.bookie.app.domain.repository.CommunityPostsRepository
import org.bookie.app.domain.repository.CommunityRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.CreateCommunity
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


    @Test
    fun guestCannotCreateCommunityOrSendPost() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.openCreate()
        vm.onCreateNameChanged("Grupo de libros")
        vm.onCreateDescriptionChanged("Una comunidad pública")
        vm.submitCreate()
        runCurrent()

        assertTrue(vm.create.value.signInRequired)
        assertEquals(0, repo.createCalls)
        assertEquals(null, vm.selected.value)
    }

    @Test
    fun formValidatesMissingFieldsWithoutSendingRequests() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo, FakeAuth())
        vm.openCreate()
        vm.onCreateNameChanged("   ")
        vm.onCreateDescriptionChanged("Descripción")
        vm.submitCreate()
        runCurrent()

        assertTrue(vm.create.value.error?.contains("nombre") == true)
        assertEquals(0, repo.createCalls)
    }

    @Test
    fun successfulCreateSelectsServerConfirmedCommunity() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo, FakeAuth())
        vm.openCreate()
        vm.onCreateNameChanged("  Nuevos lectores  ")
        vm.onCreateDescriptionChanged("  Compartimos recomendaciones  ")
        vm.submitCreate()
        assertTrue(vm.create.value.saving)
        assertEquals(null, vm.selected.value)
        runCurrent()

        assertEquals(1, repo.createCalls)
        assertEquals(
            CreateCommunityInput("Nuevos lectores", "Compartimos recomendaciones"),
            repo.lastCreateInput,
        )
        assertEquals("created-1", vm.selected.value?.id)
        assertEquals(false, vm.create.value.visible)
        assertIs<CommunityPostsState.Ready>(vm.posts.value)
    }

    @Test
    fun serverErrorsDoNotCloseFormOrInventCommunity() = runTest {
        val repo = FakeRepository()
        repo.createResult = Resource.Error("HTTP 409", ResourceErrorKind.CONFLICT)
        val vm = viewModel(repo, FakeAuth())
        vm.openCreate()
        vm.onCreateNameChanged("Duplicada")
        vm.onCreateDescriptionChanged("Comunidad existente")
        vm.submitCreate()
        runCurrent()

        assertEquals(null, vm.selected.value)
        assertTrue(vm.create.value.visible)
        assertEquals("Ya existe una comunidad con ese nombre.", vm.create.value.error)
        assertEquals(false, vm.create.value.saving)

        repo.createResult = Resource.Error("HTTP 401", ResourceErrorKind.UNAUTHORIZED)
        vm.submitCreate()
        runCurrent()
        assertTrue(vm.create.value.signInRequired)
        assertEquals(null, vm.selected.value)
    }

    @Test
    fun closingFormPreservesPublicCatalogAndResetsDraft() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        vm.loadCommunities()
        runCurrent()
        vm.openCreate()
        vm.onCreateNameChanged("Draft")
        vm.closeCreate()
        assertEquals(false, vm.create.value.visible)
        assertEquals("", vm.create.value.name)
        assertIs<CommunityCatalogState.Ready>(vm.catalog.value)
    }

    private fun viewModel(repo: FakeRepository, auth: AuthRepository? = null) =
        CommunityViewModel(
            getCommunities = GetCommunities(repo),
            createCommunity = CreateCommunity(repo),
            getRecommended = GetRecommendedCommunities(repo),
            getPosts = GetCommunityPosts(repo),
            authRepository = auth,
        )

    private fun community(id: String) = Community(
        id = id, name = "Lectores", description = "Sobre libros",
        tags = listOf("Fantasía"), imageUrl = null, ownerId = "user-1",
        createdAt = "2026-11-17T10:00:00Z",
    )

    private inner class FakeRepository : CommunityRepository, CommunityPostsRepository {
        val requestedPages = mutableListOf<Int>()
        var recommendationCalls = 0
        var failPosts = false
        var emptyPosts = false
        var createCalls = 0
        var lastCreateInput: CreateCommunityInput? = null
        var createResult: Resource<Community>? = null

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

        override suspend fun createCommunity(input: CreateCommunityInput): Resource<Community> {
            createCalls++
            lastCreateInput = input
            return createResult ?: Resource.Success(
                community("created-1").copy(name = input.name, description = input.description),
            )
        }

        override suspend fun getPosts(communityId: String, limit: Int): Resource<List<CommunityPost>> {
            if (failPosts) return Resource.Error("Sin conexión")
            if (emptyPosts) return Resource.Success(emptyList())
            return Resource.Success(listOf(CommunityPost(
                id = "p1", title = "Hola", content = "Contenido",
                communityId = communityId, authorId = "author-1",
                createdAt = "2026-11-17T11:00:00Z",
                author = PostAuthor("author-1", "Lectora", null),
            )))
        }
        override suspend fun createPost(
            communityId: String,
            input: org.bookie.app.domain.model.CreateCommunityPostInput,
        ): Resource<CommunityPost> = Resource.Error("No se usa")
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
