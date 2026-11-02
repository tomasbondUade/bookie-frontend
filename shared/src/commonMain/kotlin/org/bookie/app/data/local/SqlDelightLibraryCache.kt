package org.bookie.app.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.bookie.app.data.local.db.BookieCacheDatabase
import org.bookie.app.data.local.db.CachedLibraryState
import org.bookie.app.domain.model.Book
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStatus

class SqlDelightLibraryCache(
    driver: SqlDriver,
    private val json: Json = Json,
) : LibraryCache {
    private val database = BookieCacheDatabase(driver)
    private val queries = database.libraryCacheQueries

    override fun observePage(
        accountId: String,
        page: Int,
        limit: Int,
        status: LibraryStatus?,
        favoritesOnly: Boolean,
    ): Flow<LibraryPage?> {
        validate(accountId, page, limit)
        return queries.observePageChanges(
            accountId,
            status?.name.orEmpty(),
            favoritesOnly.asSqlLong(),
            page.toLong(),
            limit.toLong(),
        ).asFlow()
            .mapToList(Dispatchers.Default)
            .map { readPage(accountId, page, limit, status, favoritesOnly) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override fun observeState(accountId: String, stateId: String): Flow<LibraryBookState?> {
        require(accountId.isNotBlank() && stateId.isNotBlank())
        return queries.selectState(accountId, stateId).asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override suspend fun readPage(
        accountId: String,
        page: Int,
        limit: Int,
        status: LibraryStatus?,
        favoritesOnly: Boolean,
    ): LibraryPage? {
        validate(accountId, page, limit)
        val filter = status?.name.orEmpty()
        val favorite = favoritesOnly.asSqlLong()
        val meta = queries.selectPage(accountId, filter, favorite, page.toLong(), limit.toLong())
            .executeAsOneOrNull() ?: return null
        val ids = queries.selectPageStateIds(accountId, filter, favorite, page.toLong(), limit.toLong())
            .executeAsList()
        val books = mutableListOf<LibraryBookState>()
        for (id in ids) {
            val cached = queries.selectState(accountId, id).executeAsOneOrNull() ?: return null
            books.add(cached.toDomain())
        }
        return LibraryPage(
            books = books,
            page = meta.pageNumber.toInt(),
            limit = meta.pageSize.toInt(),
            total = meta.total.toInt(),
            totalPages = meta.totalPages.toInt(),
        )
    }

    override suspend fun replacePage(
        accountId: String,
        page: LibraryPage,
        status: LibraryStatus?,
        favoritesOnly: Boolean,
    ) {
        validate(accountId, page.page, page.limit)
        require(page.total >= 0 && page.totalPages >= 0)
        val filter = status?.name.orEmpty()
        val favorite = favoritesOnly.asSqlLong()
        queries.transaction {
            queries.deletePageItems(accountId, filter, favorite, page.page.toLong(), page.limit.toLong())
            page.books.forEachIndexed { index, state ->
                queries.upsertState(
                    accountId = accountId,
                    stateId = state.id,
                    bookId = state.bookId,
                    googleBookId = state.book.googleBookId,
                    bookTitle = state.book.title,
                    authorsJson = json.encodeToString(state.book.authors),
                    bookDescription = state.book.description,
                    genresJson = json.encodeToString(state.book.genres),
                    coverUrl = state.book.coverUrl,
                    previewLink = state.book.previewLink,
                    infoLink = state.book.infoLink,
                    buyLink = state.book.buyLink,
                    status = state.status.name,
                    rating = state.rating?.toLong(),
                    comment = state.comment,
                    isFavorite = state.isFavorite.asSqlLong(),
                    currentPage = state.currentPage?.toLong(),
                    progressPercentage = state.progressPercentage?.toLong(),
                    startedAt = state.startedAt,
                    finishedAt = state.finishedAt,
                    isAbandoned = state.isAbandoned.asSqlLong(),
                    updatedAt = state.updatedAt,
                )
                queries.insertPageItem(
                    accountId, filter, favorite, page.page.toLong(), page.limit.toLong(),
                    index.toLong(), state.id,
                )
            }
            queries.upsertPage(
                accountId, filter, favorite, page.page.toLong(), page.limit.toLong(),
                page.total.toLong(), page.totalPages.toLong(),
            )
        }
    }

    override suspend fun readState(accountId: String, stateId: String): LibraryBookState? {
        require(accountId.isNotBlank() && stateId.isNotBlank())
        return queries.selectState(accountId, stateId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun clearAccount(accountId: String) {
        require(accountId.isNotBlank())
        queries.transaction {
            queries.deleteAccountPageItems(accountId)
            queries.deleteAccountPages(accountId)
            queries.deleteAccountStates(accountId)
        }
    }

    private fun CachedLibraryState.toDomain() = LibraryBookState(
        id = stateId,
        bookId = bookId,
        book = Book(
            id = bookId,
            googleBookId = googleBookId,
            title = bookTitle,
            authors = json.decodeFromString(authorsJson),
            description = bookDescription,
            genres = json.decodeFromString(genresJson),
            coverUrl = coverUrl,
            previewLink = previewLink,
            infoLink = infoLink,
            buyLink = buyLink,
        ),
        status = LibraryStatus.valueOf(status),
        rating = rating?.toInt(),
        comment = comment,
        isFavorite = isFavorite != 0L,
        currentPage = currentPage?.toInt(),
        progressPercentage = progressPercentage?.toInt(),
        startedAt = startedAt,
        finishedAt = finishedAt,
        isAbandoned = isAbandoned != 0L,
        privateNotes = null,
        updatedAt = updatedAt,
    )

    private fun validate(accountId: String, page: Int, limit: Int) {
        require(accountId.isNotBlank()) { "Account ID is required." }
        require(page >= 1) { "Page must be positive." }
        require(limit in 1..50) { "Page size must be between 1 and 50." }
    }

    private fun Boolean.asSqlLong(): Long = if (this) 1L else 0L
}
