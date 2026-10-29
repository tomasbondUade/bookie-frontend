package org.bookie.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReadingProgressTest {
    @Test
    fun `accepts absent values and preserves notes exactly`() {
        val empty = ReadingProgress()
        assertNull(empty.currentPage)
        assertNull(empty.progressPercentage)
        assertNull(empty.notes)

        val withNotes = ReadingProgress(notes = "  Primera sesion\nSegundo capitulo  ")
        assertEquals("  Primera sesion\nSegundo capitulo  ", withNotes.notes)
    }

    @Test
    fun `accepts zero and the inclusive percentage boundaries`() {
        assertEquals(0, ReadingProgress(currentPage = 0).currentPage)
        assertEquals(0, ReadingProgress(progressPercentage = 0).progressPercentage)
        assertEquals(100, ReadingProgress(progressPercentage = 100).progressPercentage)
    }

    @Test
    fun `keeps page and percentage independent without a total page count`() {
        val progress = ReadingProgress(currentPage = 1000, progressPercentage = 15)
        assertEquals(1000, progress.currentPage)
        assertEquals(15, progress.progressPercentage)
        assertEquals(100, ReadingProgress(progressPercentage = 100).progressPercentage)
    }

    @Test
    fun `rejects negative pages`() {
        val error = assertFailsWith<IllegalArgumentException> {
            ReadingProgress(currentPage = -1)
        }
        assertEquals("Current page must be zero or greater.", error.message)
    }

    @Test
    fun `rejects percentages outside zero to one hundred`() {
        assertFailsWith<IllegalArgumentException> {
            ReadingProgress(progressPercentage = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            ReadingProgress(progressPercentage = 101)
        }
    }

    @Test
    fun `validates library snapshot input while preserving private notes`() {
        val input = LibraryStateInput(
            bookId = "book-1",
            currentPage = 20,
            progressPercentage = 40,
            privateNotes = "  Nota privada  ",
        )
        assertEquals("  Nota privada  ", input.privateNotes)
        assertEquals("book-1", LibraryStateInput(bookId = "book-1").bookId)

        assertFailsWith<IllegalArgumentException> {
            LibraryStateInput(bookId = "book-1", currentPage = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            input.copy(progressPercentage = 101)
        }
    }

    @Test
    fun `validates reading session input without mixing up its notes`() {
        val input = ReadingSessionInput(
            currentPage = 20,
            progressPercentage = 40,
            notes = "  Nota de sesion  ",
        )
        assertEquals("  Nota de sesion  ", input.notes)
        assertEquals("Solo notas", ReadingSessionInput(notes = "Solo notas").notes)

        assertFailsWith<IllegalArgumentException> {
            ReadingSessionInput(currentPage = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            input.copy(progressPercentage = -1)
        }
    }
}
