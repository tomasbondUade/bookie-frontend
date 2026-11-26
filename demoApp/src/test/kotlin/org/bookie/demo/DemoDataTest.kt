package org.bookie.demo

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.bookie.app.domain.model.*
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import kotlin.test.*

class DemoDataTest {
    private fun <T> Resource<T>.value(): T = assertIs<Resource.Success<T>>(this).data
    private suspend fun signedIn() = DemoData().also { assertIs<AuthState.SignedIn>(it.signIn(DEMO_EMAIL, DEMO_PASSWORD)) }

    @Test fun loginAndLocalRegistrationAreIsolatedAndRestoreTheirOwnData() = runTest {
        val data = signedIn()
        val initial = data.getLibrary().value().books.size
        assertTrue(initial > 0)
        assertIs<AuthState.Error>(data.signIn(DEMO_EMAIL, "incorrecta"))
        data.signOut()
        assertEquals(ResourceErrorKind.UNAUTHORIZED, assertIs<Resource.Error>(data.getLibrary()).kind)
        assertIs<AuthState.SignedIn>(data.signUp("otra@example.com", "prueba123", "Otra Demo"))
        assertTrue(data.getLibrary().value().books.isEmpty())
        data.saveState(LibraryStateInput(data.books.last().id))
        assertEquals(1, data.getLibrary().value().books.size)
        assertIs<AuthState.Error>(data.signUp("otra@example.com", "prueba123", "Repetida"))
        data.signOut(); data.signIn(DEMO_EMAIL, DEMO_PASSWORD)
        assertEquals(initial, data.getLibrary().value().books.size)
    }

    @Test fun searchHandlesAccentsPaginationAndEmptyResults() = runTest {
        val data = DemoData()
        assertEquals(10, data.searchBooks("todos").value().size)
        assertEquals(2, data.searchBooks("todos", 2).value().size)
        assertEquals("La naranja mecánica", data.searchBooks("mecanica").value().single().title)
        assertTrue(data.searchBooks("no-existe-xyz").value().isEmpty())
    }

    @Test fun libraryEditsFiltersAndRemovalUpdateTheObservedState() = runTest {
        val data = signedIn()
        val book = data.books.last()
        val first = data.getLibrary().value().books.first()
        data.saveState(LibraryStateInput(first.bookId, isFavorite = !first.isFavorite)).value()
        assertEquals(first.id, data.getLibrary().value().books.first().id)
        val saved = data.saveState(LibraryStateInput(book.id, status = LibraryStatus.READING,
            currentPage = 42, progressPercentage = 17, isFavorite = true, privateNotes = "Privado")).value()
        val filtered = data.getLibrary(status = LibraryStatus.READING, favoritesOnly = true).value().books
        assertTrue(filtered.any { it.id == saved.id && it.currentPage == 42 })
        val observed = assertIs<LibraryLoad.Fresh>(data.observeLibrary().first()).page
        assertTrue(observed.books.any { it.id == saved.id })
        data.deleteState(saved.id).value()
        assertFalse(data.getLibrary().value().books.any { it.id == saved.id })
    }

    @Test fun readingSessionRetryIsIdempotentAndDoesNotSaveTheSnapshot() = runTest {
        val data = signedIn()
        val entry = data.getLibrary().value().books.first()
        val before = data.getReadingSessions(entry.id).value().size
        val input = ReadingSessionInput(140, 40, "Mi sesión")
        val first = data.createReadingSession(entry.id, input, "retry-key").value()
        val again = data.createReadingSession(entry.id, input, "retry-key").value()
        assertEquals(first.id, again.id)
        assertEquals(before + 1, data.getReadingSessions(entry.id).value().size)
        assertEquals(entry.currentPage, data.getLibrary().value().books.first { it.id == entry.id }.currentPage)
    }

    @Test fun listCreateEditAddRemoveAndDeleteAreStateful() = runTest {
        val data = signedIn()
        val list = data.createList(CreateBookListInput("Mis pruebas")).value()
        data.updateList(list.id, UpdateBookListInput(name = "Actualizada", isPublic = true)).value()
        data.addBook(list.id, data.books.first().id).value()
        data.addBook(list.id, data.books.first().id).value()
        val updated = data.getLists().value().lists.first { it.id == list.id }
        assertEquals("Actualizada", updated.name)
        assertTrue(updated.isPublic)
        assertEquals(1, updated.items!!.size)
        data.removeBook(list.id, data.books.first().id).value()
        assertTrue(data.getLists().value().lists.first { it.id == list.id }.items!!.isEmpty())
        data.deleteList(list.id).value()
        assertFalse(data.getLists().value().lists.any { it.id == list.id })
    }

    @Test fun communityCreationPostsAndRepliesAppearInTheirFeeds() = runTest {
        val data = signedIn()
        val circle = data.createCommunity(CreateCommunityInput("Prueba", "Descripción local")).value()
        assertEquals(ResourceErrorKind.CONFLICT, assertIs<Resource.Error>(data.createCommunity(CreateCommunityInput("PRUEBA", "Otra"))).kind)
        assertTrue(data.listCommunities().value().communities.any { it.id == circle.id })
        val post = data.createPost(circle.id, CreateCommunityPostInput("Hola", "Contenido")).value()
        val comment = data.createComment(post.id, CreateCommunityCommentInput("Comentario")).value()
        val reply = data.createComment(post.id, CreateCommunityCommentInput("Respuesta", comment.id)).value()
        assertEquals(post.id, data.getPosts(circle.id).value().single().id)
        assertEquals(comment.id, data.getComments(post.id).value().first { it.id == reply.id }.parentId)
    }

    @Test fun preferencesRatingsStatsAndPrivateNotesUseTheCorrectFields() = runTest {
        val data = signedIn()
        data.savePreferences(ReadingPreferencesInput(listOf("Misterio"), listOf("Autora"), listOf(data.books.last().id))).value()
        assertEquals(listOf("Misterio"), data.getPreferences().value().preferredGenres)
        assertTrue(data.getPreferences().value().onboardingCompleted)
        val book = data.books.last()
        data.saveState(LibraryStateInput(book.id, status = LibraryStatus.READ, rating = 5,
            comment = "Comentario público", privateNotes = "Nota que no va en reseñas")).value()
        val own = data.getReviews(book.id).value().reviews.first { it.id == "review-own" }
        assertEquals("Comentario público", own.comment)
        assertFalse(own.comment!!.contains("Nota que"))
        assertTrue(data.getStats().value().booksReadByYear.sumOf { it.count } >= 3)
    }

    @Test fun swipesChallengesAndSeriesProgressReflectLocalMutations() = runTest {
        val data = signedIn()
        val book = data.getFeed().value().books.first().book
        data.recordSwipe(book.id, SwipeType.DISLIKE).value()
        assertFalse(data.getFeed().value().books.any { it.book.id == book.id })
        val challenge = data.getChallenges().value().first()
        data.joinChallenge(challenge.id).value()
        assertTrue(data.getChallenges().value().first { it.id == challenge.id }.joined)
        assertEquals(1, data.getMyProgress().value().size)
        val series = data.getSeriesCatalog(1, 20).value().series.single()
        val completed = data.saveState(LibraryStateInput(data.books.first().id, status = LibraryStatus.READ)).value()
        assertEquals(100, completed.progressPercentage)
        assertEquals(1, data.getProgress(series.id).value().readBooks)
        assertEquals(2, data.getMap(series.id, ReadingPath.CHRONOLOGICAL).value().nodes.size)
    }
}
