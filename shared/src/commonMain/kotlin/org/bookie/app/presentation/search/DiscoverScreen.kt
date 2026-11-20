package org.bookie.app.presentation.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bookie.app.domain.model.Book
import org.bookie.app.presentation.library.LibraryActionState
import org.bookie.app.presentation.tinder.BookTinderScreen
import org.bookie.app.presentation.tinder.BookTinderViewModel

@Composable
fun DiscoverScreen(
    searchViewModel: SearchViewModel,
    moodViewModel: MoodSearchViewModel,
    tinderViewModel: BookTinderViewModel?,
    reviewsViewModel: BookReviewsViewModel?,
    onSignIn: () -> Unit,
    onSaveBook: (Book) -> Unit,
    onRateBook: (Book, Int) -> Unit,
    libraryAction: LibraryActionState?,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val moodState by moodViewModel.uiState.collectAsStateWithLifecycle()
    val detailOpen = (searchState as? SearchUiState.Success)?.selectedBook != null ||
        (moodState as? MoodSearchUiState.Success)?.selectedBook != null

    Column(modifier.fillMaxSize()) {
        if (!detailOpen) {
            SecondaryTabRow(selectedTabIndex = tab) {
                listOf("Buscar", "Para vos", "Por ánimo").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }
        }
        when (tab) {
            0 -> BookSearchScreen(
                viewModel = searchViewModel,
                reviewsViewModel = reviewsViewModel,
                onAddToLibrary = onSaveBook,
                onRateBook = onRateBook,
                libraryAction = libraryAction,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            1 -> if (tinderViewModel != null) {
                BookTinderScreen(
                    viewModel = tinderViewModel,
                    onSignIn = onSignIn,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
                Text("Iniciá sesión para ver recomendaciones personalizadas.")
            }
            else -> MoodSearchScreen(
                viewModel = moodViewModel,
                reviewsViewModel = reviewsViewModel,
                onSignIn = onSignIn,
                onRateBook = onRateBook,
                libraryAction = libraryAction,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}
