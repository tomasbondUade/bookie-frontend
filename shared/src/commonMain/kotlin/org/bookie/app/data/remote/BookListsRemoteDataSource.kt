package org.bookie.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import org.bookie.app.data.remote.dto.AddBookToListDto
import org.bookie.app.data.remote.dto.BookListDto
import org.bookie.app.data.remote.dto.BookListItemDto
import org.bookie.app.data.remote.dto.BookListMutationResultDto
import org.bookie.app.data.remote.dto.BookListPageDto
import org.bookie.app.data.remote.dto.CreateBookListDto
import org.bookie.app.data.remote.dto.UpdateBookListDto

interface BookListsRemoteDataSource {
    suspend fun getLists(page: Int, limit: Int): BookListPageDto
    suspend fun createList(input: CreateBookListDto): BookListDto
    suspend fun updateList(listId: String, input: UpdateBookListDto): BookListDto
    suspend fun deleteList(listId: String): BookListMutationResultDto
    suspend fun addBook(listId: String, bookId: String): BookListItemDto
    suspend fun removeBook(listId: String, bookId: String): BookListMutationResultDto
}

class KtorBookListsRemoteDataSource(
    private val httpClient: HttpClient,
) : BookListsRemoteDataSource {
    override suspend fun getLists(page: Int, limit: Int): BookListPageDto {
        require(page >= 1) { "La página debe ser mayor o igual a 1." }
        require(limit in 1..50) { "El límite debe estar entre 1 y 50." }
        return httpClient.get("lists") {
            parameter("page", page)
            parameter("limit", limit)
        }.body()
    }

    override suspend fun createList(input: CreateBookListDto): BookListDto {
        require(input.name.isNotBlank()) { "El nombre de la lista es obligatorio." }
        return httpClient.post("lists") {
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }

    override suspend fun updateList(listId: String, input: UpdateBookListDto): BookListDto {
        requireId(listId, "lista")
        require(input.name != null || input.description != null || input.isPublic != null) {
            "Indicá al menos un cambio."
        }
        require(input.name == null || input.name.isNotBlank()) {
            "El nombre de la lista no puede estar vacío."
        }
        return httpClient.patch {
            url { appendPathSegments("lists", listId) }
            contentType(ContentType.Application.Json)
            setBody(input)
        }.body()
    }

    override suspend fun deleteList(listId: String): BookListMutationResultDto {
        requireId(listId, "lista")
        return httpClient.delete {
            url { appendPathSegments("lists", listId) }
        }.body()
    }

    override suspend fun addBook(listId: String, bookId: String): BookListItemDto {
        requireId(listId, "lista")
        requireId(bookId, "libro")
        return httpClient.post {
            url { appendPathSegments("lists", listId, "books") }
            contentType(ContentType.Application.Json)
            setBody(AddBookToListDto(bookId))
        }.body()
    }

    override suspend fun removeBook(listId: String, bookId: String): BookListMutationResultDto {
        requireId(listId, "lista")
        requireId(bookId, "libro")
        return httpClient.delete {
            url { appendPathSegments("lists", listId, "books", bookId) }
        }.body()
    }

    private fun requireId(value: String, field: String) {
        require(value.isNotBlank()) { "El ID de $field es obligatorio." }
    }
}
