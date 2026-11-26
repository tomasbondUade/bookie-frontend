package org.bookie.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.*
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.usecase.*
import org.bookie.app.presentation.auth.AuthViewModel
import org.bookie.app.presentation.challenges.ReadingChallengesViewModel
import org.bookie.app.presentation.community.*
import org.bookie.app.presentation.library.LibraryExperienceViewModel
import org.bookie.app.presentation.lists.BookListsViewModel
import org.bookie.app.presentation.navigation.AppFeatureViewModels
import org.bookie.app.presentation.preferences.ReadingPreferencesViewModel
import org.bookie.app.presentation.profile.ReadingStatsViewModel
import org.bookie.app.presentation.progress.ReadingProgressViewModel
import org.bookie.app.presentation.search.*
import org.bookie.app.presentation.series.*
import org.bookie.app.presentation.tinder.BookTinderViewModel

/** Uses production navigation, screens, use cases and ViewModels with local repositories. */
class DemoRuntime {
    val data = DemoData()
    private val rootStore = ViewModelStore()
    private var accountStore = ViewModelStore()
    private val progressStore = ViewModelStore()
    private val restoreScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var restoreJob: Job? = null
    private val auth = model(rootStore, "auth") { AuthViewModel(data) }
    val overlaySearch = model(rootStore, "overlay-search") { SearchViewModel(SearchBooks(data)) }
    private val mutableOverlay = MutableStateFlow<String?>(null)
    val overlayBook = mutableOverlay.asStateFlow()
    private val mutableFeatures = MutableStateFlow(buildFeatures())
    val features = mutableFeatures.asStateFlow()

    init {
        data.onAccountChanged = {
            val old = mutableFeatures.value
            val moodQuery = old.moodSearch?.query?.value.orEmpty()
            val moodBookId = (old.moodSearch?.uiState?.value as? MoodSearchUiState.Success)?.selectedBook?.book?.id
            val seriesId = old.seriesReading.uiState.value.selectedSeriesId
            val path = old.seriesReading.uiState.value.selectedPath
            restoreJob?.cancel()
            accountStore.clear()
            progressStore.clear()
            accountStore = ViewModelStore()
            mutableOverlay.value = null
            val next = buildFeatures()
            mutableFeatures.value = next
            // Public browsing survives the sign-in gate; private ViewModels are rebuilt per account.
            if (moodQuery.isNotBlank()) {
                val mood = requireNotNull(next.moodSearch)
                mood.onQueryChanged(moodQuery); mood.search()
                restoreJob = restoreScope.launch {
                    val ready = withTimeoutOrNull(3000) { mood.uiState.first { it is MoodSearchUiState.Success } }
                        as? MoodSearchUiState.Success
                    ready?.books?.find { it.book.id == moodBookId }?.let(mood::selectBook)
                }
            }
            seriesId?.let { next.seriesReading.selectSeries(it); next.seriesReading.changePath(path) }
        }
    }

    private inline fun <reified T : ViewModel> model(store: ViewModelStore, key: String, crossinline create: () -> T): T {
        val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
        return ViewModelProvider(owner, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
        })[key, T::class.java]
    }

    private fun buildFeatures(): AppFeatureViewModels {
        val library = model(accountStore, "library") {
            LibraryExperienceViewModel(ObserveLibraryOnlineFirst(data), SaveLibraryState(data), DeleteLibraryState(data))
        }
        return AppFeatureViewModels(
            auth = auth,
            bookSearch = model(rootStore, "search") { SearchViewModel(SearchBooks(data)) },
            moodSearch = model(accountStore, "mood") { MoodSearchViewModel(SearchBooksByMood(data), SaveLibraryState(data), data) },
            bookReviews = model(accountStore, "reviews") { BookReviewsViewModel(GetBookReviews(data)) },
            libraryExperience = library,
            bookLists = model(accountStore, "lists") { BookListsViewModel(GetBookLists(data), CreateBookList(data),
                UpdateBookList(data), DeleteBookList(data), AddBookToList(data), RemoveBookFromList(data), SearchBooks(data)) },
            readingPreferences = model(accountStore, "preferences") {
                ReadingPreferencesViewModel(GetReadingPreferences(data), SaveReadingPreferences(data), SearchBooks(data))
            },
            readingStats = model(accountStore, "stats") { ReadingStatsViewModel(GetReadingStats(data)) },
            community = model(rootStore, "community") { CommunityViewModel(GetCommunities(data), CreateCommunity(data),
                GetRecommendedCommunities(data), GetCommunityPosts(data), GetCommunityComments(data), CreateCommunityComment(data), data) },
            createCommunityPostsViewModel = { communityId -> model(accountStore, "posts-$communityId") {
                CommunityPostsViewModel(communityId, GetCommunityPosts(data), CreateCommunityPost(data))
            } },
            readingCompatibility = model(accountStore, "compatibility") { ReadingCompatibilityViewModel(GetReadingCompatibility(data)) },
            createReadingProgressViewModel = ::progress,
            bookTinder = model(accountStore, "tinder") { BookTinderViewModel(GetBookTinderFeed(data), RecordBookSwipe(data)) },
            readingChallenges = model(accountStore, "challenges") { ReadingChallengesViewModel(GetReadingChallenges(data), GetMyReadingChallengeProgress(data), JoinReadingChallenge(data)) },
            seriesExplorer = model(rootStore, "explorer") { SeriesExplorerViewModel(GetSeriesCatalog(data), GetUniverseCatalog(data), GetUniverseDetail(data)) },
            seriesReading = model(accountStore, "series") { SeriesReadingViewModel(GetActiveSeries(data), GetSeriesProgress(data), GetSeriesReadingMap(data)) },
            onOpenBook = { mutableOverlay.value = it },
        )
    }

    private fun progress(entry: LibraryBookState): ReadingProgressViewModel {
        progressStore.clear()
        return model(progressStore, "progress") { ReadingProgressViewModel(entry, SaveLibraryState(data), CreateReadingSession(data), GetReadingSessions(data)) }
    }
    fun closeBook() { mutableOverlay.value = null }
    fun close() {
        data.onAccountChanged = {}
        restoreScope.cancel()
        accountStore.clear(); progressStore.clear(); rootStore.clear()
    }
}
