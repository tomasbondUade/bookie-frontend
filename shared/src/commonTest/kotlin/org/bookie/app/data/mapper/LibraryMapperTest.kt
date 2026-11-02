package org.bookie.app.data.mapper

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.bookie.app.data.remote.dto.LibraryPageDto
import org.bookie.app.data.remote.dto.LibraryStatusDto
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LibraryMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `maps paginated library and all personal fields without losing book and state ids`() {
        val dto = json.decodeFromString<LibraryPageDto>(
            """{"data":[{"id":"state-7","bookId":"book-3","status":"READ","rating":5,"comment":"Great","isFavorite":true,"currentPage":250,"progressPercentage":100,"startedAt":"2026-10-01T00:00:00.000Z","finishedAt":"2026-10-10T00:00:00.000Z","isAbandoned":false,"privateNotes":"Personal","updatedAt":"2026-10-10T00:00:00.000Z","book":{"id":"book-3","googleBookId":"google-3","title":"Dune","authors":["Frank Herbert"],"genres":["Sci-Fi"]}}],"meta":{"page":2,"limit":10,"total":12,"totalPages":2}}"""
        )

        val page = dto.toDomain()
        val state = page.books.single()

        assertEquals(2, page.page)
        assertEquals(10, page.limit)
        assertEquals(12, page.total)
        assertEquals(2, page.totalPages)
        assertEquals("state-7", state.id)
        assertEquals("book-3", state.bookId)
        assertEquals("book-3", state.book.id)
        assertEquals("Dune", state.book.title)
        assertEquals(LibraryStatus.READ, state.status)
        assertEquals(5, state.rating)
        assertEquals("Great", state.comment)
        assertEquals(true, state.isFavorite)
        assertEquals(250, state.currentPage)
        assertEquals(100, state.progressPercentage)
        assertEquals("2026-10-01T00:00:00.000Z", state.startedAt)
        assertEquals("2026-10-10T00:00:00.000Z", state.finishedAt)
        assertEquals(false, state.isAbandoned)
        assertEquals("Personal", state.privateNotes)
        assertEquals("2026-10-10T00:00:00.000Z", state.updatedAt)
    }

    @Test
    fun `keeps optional fields nullable and ignores new API fields`() {
        val dto = json.decodeFromString<LibraryPageDto>(
            """{"data":[{"id":"state-1","bookId":"book-1","status":"TO_READ","updatedAt":"2026-10-10T00:00:00.000Z","futureField":"ignored","book":{"id":"book-1","googleBookId":"google-1","title":"Book","futureBookField":42}}],"meta":{"page":1,"limit":20,"total":1,"totalPages":1,"futureMetaField":true},"futurePageField":{}}"""
        )
        val state = dto.toDomain().books.single()

        assertEquals(LibraryStatus.TO_READ, state.status)
        assertNull(state.rating)
        assertNull(state.comment)
        assertNull(state.currentPage)
        assertNull(state.progressPercentage)
        assertNull(state.startedAt)
        assertNull(state.finishedAt)
        assertNull(state.privateNotes)
        assertEquals(false, state.isFavorite)
        assertEquals(false, state.isAbandoned)
        assertEquals(emptyList(), state.book.authors)
        assertEquals(emptyList(), state.book.genres)
    }

    @Test
    fun `maps all status values explicitly in both directions`() {
        for (status in LibraryStatus.entries) {
            val dto = status.toDto()
            assertEquals(status.name, dto.name)
        }
        assertEquals(LibraryStatusDto.TO_READ, LibraryStatus.TO_READ.toDto())
        assertEquals(LibraryStatusDto.READING, LibraryStatus.READING.toDto())
        assertEquals(LibraryStatusDto.READ, LibraryStatus.READ.toDto())
    }

    @Test
    fun `maps save input without including catalog metadata or user id`() {
        val dto = LibraryStateInput(
            bookId = "book-1",
            status = LibraryStatus.READING,
            rating = 4,
            comment = "Good",
            isFavorite = true,
            currentPage = 45,
            progressPercentage = 30,
            startedAt = "2026-10-01T00:00:00.000Z",
            finishedAt = null,
            isAbandoned = false,
            privateNotes = "Keep private",
        ).toDto()

        assertEquals("book-1", dto.bookId)
        assertEquals(LibraryStatusDto.READING, dto.status)
        assertEquals(4, dto.rating)
        assertEquals("Good", dto.comment)
        assertEquals(true, dto.isFavorite)
        assertEquals(45, dto.currentPage)
        assertEquals(30, dto.progressPercentage)
        assertEquals("2026-10-01T00:00:00.000Z", dto.startedAt)
        assertNull(dto.finishedAt)
        assertEquals(false, dto.isAbandoned)
        assertEquals("Keep private", dto.privateNotes)
    }

    @Test
    fun `rejects unknown status instead of silently changing reading state`() {
        assertFailsWith<Exception> {
            json.decodeFromString<LibraryPageDto>(
                """{"data":[{"id":"state-1","bookId":"book-1","status":"PAUSED","updatedAt":"2026-10-10T00:00:00.000Z","book":{"id":"book-1","googleBookId":"google-1","title":"Book"}}],"meta":{"page":1,"limit":20,"total":1,"totalPages":1}}"""
            )
        }
    }
}
