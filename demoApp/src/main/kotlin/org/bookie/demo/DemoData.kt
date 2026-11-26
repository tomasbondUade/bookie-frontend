package org.bookie.demo

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.bookie.app.domain.model.*
import org.bookie.app.domain.repository.*
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind

const val DEMO_EMAIL = "demo@bookie.app"
const val DEMO_PASSWORD = "bookie123"

/** Fictional, process-local state. This module has no network client or Internet permission. */
class DemoData : AuthRepository, BookRepository, MoodBookRepository, LibraryRepository,
    LibraryFeedRepository, ReadingStatsRepository, ReadingPreferencesRepository,
    BookListsRepository, BookReviewsRepository, CommunityRepository, CommunityPostsRepository,
    BookTinderRepository, SeriesReadingRepository, ReadingChallengesRepository, ReadingCompatibilityRepository {

    private var sequence = 100
    private fun id(prefix: String) = "$prefix-${sequence++}"
    private fun stamp(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
    private val revision = MutableStateFlow(0)
    private fun changed() { revision.value++ }
    private val authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val state: StateFlow<AuthState> = authState.asStateFlow()
    var onAccountChanged: () -> Unit = {}

    val books: List<Book> = listOf(
        Triple("El nombre del viento", "Patrick Rothfuss", "Fantasía"),
        Triple("Dune", "Frank Herbert", "Ciencia ficción"),
        Triple("La chica de la nieve", "Javier Castillo", "Misterio"),
        Triple("Sapiens", "Yuval Noah Harari", "Historia"),
        Triple("1984", "George Orwell", "Ficción"),
        Triple("El temor de un hombre sabio", "Patrick Rothfuss", "Fantasía"),
        Triple("Proyecto Hail Mary", "Andy Weir", "Ciencia ficción"),
        Triple("Orgullo y prejuicio", "Jane Austen", "Romance"),
        Triple("El hobbit", "J. R. R. Tolkien", "Fantasía"),
        Triple("La naranja mecánica", "Anthony Burgess", "Ficción"),
        Triple("Fundación", "Isaac Asimov", "Ciencia ficción"),
        Triple("Las mil y una noches", "Anónimo", "Aventura"),
    ).mapIndexed { index, (title, author, genre) -> Book(
        "book-${index + 1}", "demo-${index + 1}", title, listOf(author),
        "Ficha de prueba de «$title». Usá este libro para recorrer el detalle, leer reseñas, " +
            "guardarlo en tu biblioteca y registrar tu lectura. Todo funciona con datos locales de demostración.",
        listOf(genre), null, null, null, null,
    ) }

    private data class DemoAccount(val email: String, val password: String, val username: String, val userId: String)
    private class AccountData {
        var library = listOf<LibraryBookState>()
        var lists = listOf<BookList>()
        var preferences = ReadingPreferences(emptyList(), emptyList(), emptyList(), false)
        val sessions = mutableMapOf<String, List<ReadingSession>>()
        val swipes = mutableMapOf<String, SwipeType>()
        val joinedChallenges = mutableSetOf<String>()
    }
    private val accounts = mutableMapOf(DEMO_EMAIL to DemoAccount(DEMO_EMAIL, DEMO_PASSWORD, "Julieta Demo", "demo-reader"))
    private val accountData = mutableMapOf("demo-reader" to AccountData().apply {
        library = books.take(6).mapIndexed { index, book -> newEntry(book).copy(
            status = when (index) { 0, 1 -> LibraryStatus.READING; 3, 4 -> LibraryStatus.READ; else -> LibraryStatus.TO_READ },
            rating = when (index) { 0, 4 -> 5; 3 -> 4; else -> null },
            isFavorite = index == 0 || index == 3,
            currentPage = when (index) { 0 -> 120; 1 -> 97; else -> null },
            progressPercentage = when (index) { 0 -> 35; 1 -> 18; 3, 4 -> 100; else -> null },
            finishedAt = stamp().take(10).takeIf { index == 3 || index == 4 },
        ) }
        preferences = ReadingPreferences(listOf("Fantasía", "Ciencia ficción"), listOf("Patrick Rothfuss"), listOf(books.first().id), true)
        lists = listOf(BookList("list-seed", "Para una noche de lluvia", "Lecturas para disfrutar sin apuro.", false,
            stamp(), stamp(), listOf(BookListItem("item-seed", "list-seed", books[2].id, stamp(), books[2]))))
        sessions[library.first().id] = listOf(ReadingSession("session-seed", library.first().id,
            "seed", 120, 35, "Una buena noche de lectura. Nota de prueba.", stamp()))
    })
    private fun currentId() = (authState.value as? AuthState.SignedIn)?.userId
    private fun account() = currentId()?.let(accountData::get)
    private fun <T> unauthenticated(): Resource<T> = Resource.Error("Iniciá sesión con la cuenta de demostración.", ResourceErrorKind.UNAUTHORIZED)
    private fun <T> missing(): Resource<T> = Resource.Error("No se encontró este elemento de prueba.", ResourceErrorKind.NOT_FOUND)
    private fun newEntry(book: Book) = LibraryBookState(id("state"), book.id, book, LibraryStatus.TO_READ,
        null, null, false, null, null, null, null, false, null, stamp())

    override suspend fun restoreSession(): AuthState = authState.value
    override suspend fun signIn(email: String, password: String): AuthState {
        delay(150)
        val user = accounts[email.trim().lowercase(Locale.ROOT)]
        if (user == null || user.password != password) return AuthState.Error("En la demo usá $DEMO_EMAIL / $DEMO_PASSWORD, o registrá una cuenta local.")
        return AuthState.SignedIn(user.userId, user.email).also {
            authState.value = it; onAccountChanged(); changed()
        }
    }
    override suspend fun signUp(email: String, password: String, username: String): AuthState {
        delay(150)
        val key = email.trim().lowercase(Locale.ROOT)
        if (key in accounts) return AuthState.Error("Ese correo ya está registrado en esta demo.")
        if ('@' !in key || password.length < 6 || username.trim().length !in 3..30) return AuthState.Error("Revisá los datos de la cuenta local.")
        val user = DemoAccount(key, password, username.trim(), id("reader"))
        accounts[key] = user; accountData[user.userId] = AccountData()
        return AuthState.SignedIn(user.userId, user.email).also {
            authState.value = it; onAccountChanged(); changed()
        }
    }
    override suspend fun signOut(): AuthState = AuthState.SignedOut.also {
        authState.value = it; onAccountChanged(); changed()
    }
    private fun String.normalized() = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "").lowercase(Locale.ROOT)
    override suspend fun searchBooks(query: String, page: Int): Resource<List<Book>> {
        delay(120)
        val needle = query.trim().normalized()
        val found = if (needle == "todos") books else books.filter {
            listOf(it.title, it.authors.joinToString(), it.genres.joinToString(), it.googleBookId).any { value -> needle in value.normalized() }
        }
        return Resource.Success(found.drop((page.coerceAtLeast(1) - 1) * 10).take(10))
    }
    override suspend fun searchByMood(query: String): Resource<List<MoodBook>> {
        delay(150)
        val genre = when {
            query.normalized().let { "espacio" in it || "futuro" in it } -> "Ciencia ficción"
            query.normalized().let { "amor" in it || "romance" in it } -> "Romance"
            query.normalized().let { "misterio" in it || "suspenso" in it } -> "Misterio"
            else -> "Fantasía"
        }
        return Resource.Success(books.filter { genre in it.genres }.map { MoodBook(it, .9) })
    }
    private fun libraryPage(page: Int, limit: Int, status: LibraryStatus?, favorites: Boolean): Resource<LibraryPage> {
        val data = account() ?: return unauthenticated()
        val entries = data.library.filter { (status == null || it.status == status) && (!favorites || it.isFavorite) }
        return Resource.Success(LibraryPage(entries.drop((page - 1) * limit).take(limit), page, limit, entries.size,
            ((entries.size + limit - 1) / limit).coerceAtLeast(1)))
    }
    override suspend fun getLibrary(page: Int, limit: Int, status: LibraryStatus?, favoritesOnly: Boolean) = libraryPage(page, limit, status, favoritesOnly)
    override fun observeLibrary(page: Int, limit: Int, status: LibraryStatus?, favoritesOnly: Boolean): Flow<LibraryLoad> =
        combine(revision, authState) { _, _ ->
            when (val result = libraryPage(page, limit, status, favoritesOnly)) {
                is Resource.Success -> LibraryLoad.Fresh(result.data)
                is Resource.Error -> LibraryLoad.Failed(result.message)
            }
        }
    override suspend fun saveState(input: LibraryStateInput): Resource<LibraryBookState> {
        val data = account() ?: return unauthenticated()
        val book = books.find { it.id == input.bookId } ?: return missing()
        val old = data.library.find { it.bookId == book.id } ?: newEntry(book)
        val status = input.status ?: old.status
        val next = old.copy(status = status, rating = input.rating ?: old.rating,
            comment = input.comment ?: old.comment, isFavorite = input.isFavorite ?: old.isFavorite,
            currentPage = input.currentPage ?: old.currentPage,
            progressPercentage = if (status == LibraryStatus.READ) 100 else input.progressPercentage ?: old.progressPercentage,
            startedAt = input.startedAt ?: old.startedAt,
            finishedAt = input.finishedAt ?: old.finishedAt ?: stamp().take(10).takeIf { status == LibraryStatus.READ },
            isAbandoned = input.isAbandoned ?: old.isAbandoned, privateNotes = input.privateNotes ?: old.privateNotes,
            updatedAt = stamp())
        data.library = if (data.library.any { it.bookId == book.id })
            data.library.map { if (it.bookId == book.id) next else it }
        else listOf(next) + data.library
        changed(); return Resource.Success(next)
    }
    override suspend fun deleteState(stateId: String): Resource<Unit> {
        val data = account() ?: return unauthenticated()
        if (data.library.none { it.id == stateId }) return missing()
        data.library = data.library.filterNot { it.id == stateId }; data.sessions.remove(stateId)
        changed(); return Resource.Success(Unit)
    }
    override suspend fun createReadingSession(stateId: String, input: ReadingSessionInput, idempotencyKey: String): Resource<ReadingSession> {
        val data = account() ?: return unauthenticated()
        if (data.library.none { it.id == stateId }) return missing()
        val history = data.sessions[stateId].orEmpty()
        history.find { it.idempotencyKey == idempotencyKey }?.let { return Resource.Success(it) }
        val session = ReadingSession(id("session"), stateId, idempotencyKey, input.currentPage, input.progressPercentage, input.notes, stamp())
        data.sessions[stateId] = listOf(session) + history; changed(); return Resource.Success(session)
    }
    override suspend fun getReadingSessions(stateId: String): Resource<List<ReadingSession>> {
        val data = account() ?: return unauthenticated()
        if (data.library.none { it.id == stateId }) return missing()
        return Resource.Success(data.sessions[stateId].orEmpty().toList())
    }
    override suspend fun getPreferences(): Resource<ReadingPreferences> = account()?.let { Resource.Success(it.preferences) } ?: unauthenticated()
    override suspend fun savePreferences(input: ReadingPreferencesInput): Resource<ReadingPreferences> {
        val data = account() ?: return unauthenticated()
        data.preferences = ReadingPreferences(input.preferredGenres.toList(), input.preferredAuthors.toList(), input.favoriteBookIds.toList(), true)
        changed(); return Resource.Success(data.preferences)
    }
    override suspend fun getStats(): Resource<ReadingStats> {
        val data = account() ?: return unauthenticated()
        val read = data.library.filter { it.status == LibraryStatus.READ }
        val ratings = data.library.mapNotNull { it.rating }
        return Resource.Success(ReadingStats(
            mostReadGenres = read.flatMap { it.book.genres }.groupingBy { it }.eachCount().map { GenreReadingCount(it.key, it.value) },
            favoriteAuthors = data.library.filter { it.isFavorite }.flatMap { it.book.authors }.groupingBy { it }.eachCount().map { AuthorFavoriteCount(it.key, it.value) },
            averageRating = AverageRating(if (ratings.isEmpty()) 0.0 else ratings.average(), ratings.size),
            booksReadByMonth = read.groupBy { it.finishedAt?.take(7) ?: stamp().take(7) }.map { (date, entries) -> MonthlyReadingCount(date.take(4).toIntOrNull() ?: 2026, date.takeLast(2).toIntOrNull() ?: 11, entries.size) },
            booksReadByYear = read.groupBy { it.finishedAt?.take(4) ?: stamp().take(4) }.map { (year, entries) -> YearlyReadingCount(year.toIntOrNull() ?: 2026, entries.size) },
            readingStreak = ReadingStreak(if (data.sessions.values.any { it.isNotEmpty() }) 1 else 0, if (data.sessions.values.any { it.isNotEmpty() }) 1 else 0),
        ))
    }

    override suspend fun getLists(page: Int, limit: Int): Resource<BookListPage> {
        val data = account() ?: return unauthenticated()
        return Resource.Success(BookListPage(data.lists.drop((page - 1) * limit).take(limit).toList(), page, limit,
            data.lists.size, ((data.lists.size + limit - 1) / limit).coerceAtLeast(1)))
    }
    override suspend fun createList(input: CreateBookListInput): Resource<BookList> {
        val data = account() ?: return unauthenticated()
        if (input.name.isBlank()) return Resource.Error("La lista necesita un nombre.", ResourceErrorKind.VALIDATION)
        val list = BookList(id("list"), input.name.trim(), input.description, input.isPublic, stamp(), stamp(), emptyList())
        data.lists = data.lists + list; changed(); return Resource.Success(list)
    }
    override suspend fun updateList(listId: String, input: UpdateBookListInput): Resource<BookList> {
        val data = account() ?: return unauthenticated()
        val old = data.lists.find { it.id == listId } ?: return missing()
        val next = old.copy(name = input.name ?: old.name, description = input.description ?: old.description,
            isPublic = input.isPublic ?: old.isPublic, updatedAt = stamp())
        data.lists = data.lists.map { if (it.id == listId) next else it }; changed(); return Resource.Success(next)
    }
    override suspend fun deleteList(listId: String): Resource<Unit> {
        val data = account() ?: return unauthenticated()
        if (data.lists.none { it.id == listId }) return missing()
        data.lists = data.lists.filterNot { it.id == listId }; changed(); return Resource.Success(Unit)
    }
    override suspend fun addBook(listId: String, bookId: String): Resource<BookListItem> {
        val data = account() ?: return unauthenticated()
        val list = data.lists.find { it.id == listId } ?: return missing()
        val book = books.find { it.id == bookId } ?: return missing()
        list.items.orEmpty().find { it.bookId == bookId }?.let { return Resource.Success(it) }
        val item = BookListItem(id("item"), listId, bookId, stamp(), book)
        data.lists = data.lists.map { if (it.id == listId) it.copy(items = it.items.orEmpty() + item) else it }
        changed(); return Resource.Success(item)
    }
    override suspend fun removeBook(listId: String, bookId: String): Resource<Unit> {
        val data = account() ?: return unauthenticated()
        if (data.lists.none { it.id == listId }) return missing()
        data.lists = data.lists.map { if (it.id == listId) it.copy(items = it.items.orEmpty().filterNot { item -> item.bookId == bookId }) else it }
        changed(); return Resource.Success(Unit)
    }
    override suspend fun getReviews(bookId: String, page: Int, limit: Int, sort: BookReviewSort): Resource<BookReviewsPage> {
        val own = account()?.library?.find { it.bookId == bookId && (it.rating != null || !it.comment.isNullOrBlank()) }
        val username = accounts.values.find { it.userId == currentId() }?.username ?: "Lector Demo"
        val reviews = listOfNotNull(
            own?.let { BookReview("review-own", it.rating, it.comment, it.updatedAt, username, null) },
            BookReview("review-mica", 5, "Reseña de prueba: me atrapó desde el primer capítulo.", stamp(), "Mica Demo", null),
            BookReview("review-luna", 4, "Reseña de prueba: una lectura para recomendar.", stamp(), "Luna Demo", null),
        ).let { if (sort == BookReviewSort.TOP) it.sortedByDescending { review -> review.rating ?: 0 } else it }
        return Resource.Success(BookReviewsPage(reviews.drop((page - 1) * limit).take(limit), page, limit, reviews.size,
            ((reviews.size + limit - 1) / limit).coerceAtLeast(1)))
    }

    private var circles = listOf(
        Community("circle-fantasy", "Entre mundos", "Un espacio de prueba para compartir fantasía y nuevas aventuras.", listOf("Fantasía", "Aventura"), null, "reader-mica", stamp()),
        Community("circle-fiction", "Un capítulo más", "Reseñas, recomendaciones y lecturas en compañía. Comunidad de demostración.", listOf("Lectura", "Reseñas"), null, "reader-luna", stamp()),
    )
    private var posts = circles.map { circle -> CommunityPost("post-${circle.id}", "¿Cuál es tu próxima lectura?",
        "Terminé un libro y quiero descubrir algo nuevo. ¿Qué me recomiendan? Publicación de prueba.",
        circle.id, "reader-mica", stamp(), PostAuthor("reader-mica", "Mica Demo", null)) }
    private var comments = posts.map { CommunityComment("comment-${it.id}", "¡Probá una aventura de fantasía! Comentario de prueba.", it.id, "reader-luna", null, stamp()) }
    override suspend fun listCommunities(page: Int, limit: Int): Resource<CommunityPage> = Resource.Success(
        CommunityPage(circles.drop((page - 1) * limit).take(limit).toList(), page, limit, circles.size,
            ((circles.size + limit - 1) / limit).coerceAtLeast(1)))
    override suspend fun recommended(): Resource<List<Community>> = if (account() == null) unauthenticated() else Resource.Success(circles.take(2))
    override suspend fun createCommunity(input: CreateCommunityInput): Resource<Community> {
        val userId = currentId() ?: return unauthenticated()
        if (circles.any { it.name.equals(input.name.trim(), ignoreCase = true) })
            return Resource.Error("Ya existe una comunidad con ese nombre.", ResourceErrorKind.CONFLICT)
        val community = Community(id("circle"), input.name.trim(), input.description.trim(), listOf("Demo"), null, userId, stamp())
        circles = listOf(community) + circles; changed(); return Resource.Success(community)
    }
    override suspend fun getPosts(communityId: String, limit: Int): Resource<List<CommunityPost>> =
        Resource.Success(posts.filter { it.communityId == communityId }.take(limit).toList())
    override suspend fun getComments(postId: String, limit: Int): Resource<List<CommunityComment>> =
        Resource.Success(comments.filter { it.postId == postId }.take(limit).toList())
    override suspend fun createPost(communityId: String, input: CreateCommunityPostInput): Resource<CommunityPost> {
        val userId = currentId() ?: return unauthenticated()
        if (circles.none { it.id == communityId }) return missing()
        val username = accounts.values.find { it.userId == userId }?.username ?: "Lector Demo"
        val post = CommunityPost(id("post"), input.title, input.content, communityId, userId, stamp(), PostAuthor(userId, username, null))
        posts = listOf(post) + posts; changed(); return Resource.Success(post)
    }
    override suspend fun createComment(postId: String, input: CreateCommunityCommentInput): Resource<CommunityComment> {
        val userId = currentId() ?: return unauthenticated()
        if (posts.none { it.id == postId }) return missing()
        if (input.parentId != null && comments.none { it.id == input.parentId && it.postId == postId }) return missing()
        val comment = CommunityComment(id("comment"), input.content, postId, userId, input.parentId, stamp())
        comments = comments + comment; changed(); return Resource.Success(comment)
    }
    override suspend fun getFeed(limit: Int): Resource<BookTinderFeed> {
        val data = account() ?: return unauthenticated()
        val available = books.filterNot { it.id in data.swipes }.take(limit)
        return Resource.Success(BookTinderFeed(RecommendationSource.FALLBACK, available.size,
            available.map { BookRecommendation(it, listOf("Selección local de demostración.")) }))
    }
    override suspend fun recordSwipe(bookId: String, type: SwipeType): Resource<Unit> {
        val data = account() ?: return unauthenticated()
        if (books.none { it.id == bookId }) return missing()
        data.swipes[bookId] = type; changed(); return Resource.Success(Unit)
    }

    private val universe = UniverseReference("universe-demo", "Los Cuatro Rincones", "Universo de prueba para recorrer las sagas.", null, "Patrick Rothfuss")
    private val sagaBooks get() = listOf(books[0], books[5])
    private fun saga() = SeriesCatalogEntry("series-demo", universe.id, "Crónica del Asesino de Reyes",
        "Saga de demostración con dos libros para probar el mapa de lectura.", null, 2, 2, universe)
    override suspend fun getSeriesCatalog(page: Int, limit: Int, universeId: String?): Resource<SeriesCatalogPage> {
        val items = listOf(saga()).filter { universeId == null || it.universeId == universeId }
        return Resource.Success(SeriesCatalogPage(if (page == 1) items else emptyList(), page, limit, items.size, 1))
    }
    private fun seriesProgress(): SeriesProgress {
        val nodes = sagaBooks.mapIndexed { index, book ->
            val entry = account()?.library?.find { it.bookId == book.id }
            SeriesNodeProgress("node-${book.id}", book.id, book.title, "MAIN", index.toDouble(), when (entry?.status) {
                LibraryStatus.READ -> SeriesNodeStatus.COMPLETED
                LibraryStatus.READING -> SeriesNodeStatus.IN_PROGRESS
                else -> SeriesNodeStatus.PENDING
            }, entry?.progressPercentage, entry?.currentPage)
        }
        val completed = nodes.count { it.status == SeriesNodeStatus.COMPLETED }
        return SeriesProgress("series-demo", saga().title, 2, completed, completed * 50.0, completed == 2, nodes)
    }
    override suspend fun getUniverses(): Resource<List<UniverseCatalogEntry>> = Resource.Success(listOf(
        UniverseCatalogEntry(universe.id, universe.name, universe.description, null, universe.author, 1,
            currentId()?.let { seriesProgress() }?.let { UniverseProgress(universe.id, universe.name, 2, it.readBooks, it.completionPercentage, it.isCompleted) })))
    override suspend fun getUniverseDetail(universeId: String): Resource<UniverseDetail> = if (universeId == universe.id)
        Resource.Success(UniverseDetail(universe.id, universe.name, universe.description, null, universe.author, listOf(saga()))) else missing()
    override suspend fun getActiveSeries(): Resource<List<SeriesProgress>> = if (account() == null) unauthenticated() else Resource.Success(listOf(seriesProgress()))
    override suspend fun getProgress(seriesId: String): Resource<SeriesProgress> = when {
        account() == null -> unauthenticated()
        seriesId != "series-demo" -> missing()
        else -> Resource.Success(seriesProgress())
    }
    override suspend fun getMap(seriesId: String, path: ReadingPath): Resource<SeriesReadingMap> {
        if (account() == null) return unauthenticated()
        if (seriesId != "series-demo") return missing()
        val nodes = seriesProgress().nodes.map { SeriesMapNode(it.nodeId, it.bookId, it.title, it.nodeType,
            it.sequenceOrder, it.sequenceOrder, it.sequenceOrder, it.sequenceOrder, it.status, it.progressPercentage, it.currentPage) }
        return Resource.Success(SeriesReadingMap(seriesId, saga().title, path, nodes,
            listOf(SeriesMapEdge("edge-demo", nodes[0].nodeId, nodes[1].nodeId, "READ_BEFORE"))))
    }
    private val challengeSeeds = listOf(
        ReadingChallenge("challenge-books", "Un capítulo cada día", "Terminá tres libros y probá cómo cambia tu progreso.", "BOOK_COUNT", 3, null, stamp(), false),
        ReadingChallenge("challenge-genre", "Una nueva aventura", "Leé dos libros de fantasía.", "GENRE", 2, "Fantasía", stamp(), false),
    )
    override suspend fun getChallenges(): Resource<List<ReadingChallenge>> = Resource.Success(challengeSeeds.map { it.copy(joined = it.id in account()?.joinedChallenges.orEmpty()) })
    override suspend fun getMyProgress(): Resource<List<ReadingChallengeProgress>> {
        val data = account() ?: return unauthenticated()
        return Resource.Success(challengeSeeds.filter { it.id in data.joinedChallenges }.map { challenge ->
            val completed = data.library.count { it.status == LibraryStatus.READ && (challenge.genre == null || challenge.genre in it.book.genres) }
            ReadingChallengeProgress(challenge.id, challenge.title, challenge.description, challenge.type,
                challenge.target, challenge.genre, completed, (completed * 100.0 / challenge.target).coerceAtMost(100.0), stamp())
        })
    }
    override suspend fun joinChallenge(challengeId: String): Resource<Unit> {
        val data = account() ?: return unauthenticated()
        if (challengeSeeds.none { it.id == challengeId }) return missing()
        data.joinedChallenges += challengeId; changed(); return Resource.Success(Unit)
    }
    override suspend fun getCompatibility(targetUserId: String): Resource<ReadingCompatibility> {
        if (account() == null) return unauthenticated()
        return Resource.Success(ReadingCompatibility(CompatibilityUser(targetUserId, "Mica Demo", null), 75,
            sharedBooks = listOf(CompatibilityBook(books.first().id, books.first().title)), sharedGenres = listOf("Fantasía"),
            reasons = listOf("Resultado simulado para recorrer la pantalla de compatibilidad."), dataQuality = "limited"))
    }
}
