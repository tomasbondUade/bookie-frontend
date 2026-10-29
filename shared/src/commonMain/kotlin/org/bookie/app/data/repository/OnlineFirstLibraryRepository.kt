package org.bookie.app.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.bookie.app.data.local.LibraryCache
import org.bookie.app.data.mapper.toDomain
import org.bookie.app.data.mapper.toDto
import org.bookie.app.data.remote.LibraryRemoteDataSource
import org.bookie.app.data.remote.dto.LibraryStatusDto
import org.bookie.app.data.remote.toApiError
import org.bookie.app.data.remote.toMessage
import org.bookie.app.domain.model.LibraryBookState
import org.bookie.app.domain.model.LibraryLoad
import org.bookie.app.domain.model.LibraryPage
import org.bookie.app.domain.model.LibraryStateInput
import org.bookie.app.domain.model.LibraryStatus
import org.bookie.app.domain.model.ReadingSession
import org.bookie.app.domain.model.ReadingSessionInput
import org.bookie.app.domain.repository.LibraryRepository
import org.bookie.app.domain.result.Resource

/**
 * Remote writes only; cached reads are account-scoped and always marked stale.
 * Authentication integration supplies the current account ID via AccountIdProvider.
 */
class OnlineFirstLibraryRepository(
    private val remote: LibraryRemoteDataSource,
    private val cache: LibraryCache,
    private val accounts: AccountIdProvider,
) : LibraryRepository {
    fun observeLibrary(
        page: Int = 1,
        limit: Int = 20,
        status: LibraryStatus? = null,
        favoritesOnly: Boolean = false,
    ): Flow<LibraryLoad> = flow {
        val account = accounts.currentAccountId()?.takeIf { it.isNotBlank() }
        if (account == null) {
            emit(LibraryLoad.Failed("Iniciá sesión para consultar tu biblioteca."))
            return@flow
        }
        val cached = try {
            cache.readPage(account, page, limit, status, favoritesOnly)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            null
        }
        if (cached != null) emit(LibraryLoad.Cached(cached))
        try {
            val fresh = remote.getLibrary(
                page, limit, status?.let { LibraryStatusDto.valueOf(it.name) }, favoritesOnly,
            ).toDomain()
            if (accounts.currentAccountId() != account) return@flow
            cache.replacePage(account, fresh, status, favoritesOnly)
            if (accounts.currentAccountId() != account) {
                cache.clearAccount(account)
                return@flow
            }
            emit(LibraryLoad.Fresh(fresh))
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (exception: Exception) {
            if (accounts.currentAccountId() == account) {
                emit(LibraryLoad.Failed(exception.toApiError().toMessage(), cached))
            }
        }
    }

    override suspend fun getLibrary(
        page: Int,
        limit: Int,
        status: LibraryStatus?,
        favoritesOnly: Boolean,
    ): Resource<LibraryPage> {
        var result: Resource<LibraryPage> = Resource.Error("No se pudo cargar la biblioteca.")
        observeLibrary(page, limit, status, favoritesOnly).collect { state ->
            when (state) {
                is LibraryLoad.Cached -> result = Resource.Success(state.page)
                is LibraryLoad.Fresh -> result = Resource.Success(state.page)
                is LibraryLoad.Failed -> {
                    result = state.cachedPage?.let { Resource.Success(it) }
                        ?: Resource.Error(state.message)
                }
            }
        }
        return result
    }

    override suspend fun saveState(input: LibraryStateInput): Resource<LibraryBookState> =
        remoteWrite {
            remote.saveState(input.toDto()).toDomain()
        }

    override suspend fun createReadingSession(
        stateId: String,
        input: ReadingSessionInput,
        idempotencyKey: String,
    ): Resource<ReadingSession> = remoteWrite {
        remote.createReadingSession(stateId, input.toDto(), idempotencyKey).toDomain()
    }

    override suspend fun getReadingSessions(stateId: String): Resource<List<ReadingSession>> =
        try {
            requireAccount()
            Resource.Success(remote.getReadingSessions(stateId).map { it.toDomain() })
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (exception: Exception) {
            Resource.Error(exception.toApiError().toMessage())
        }

    suspend fun clearAccountCache(accountId: String) {
        require(accountId.isNotBlank())
        cache.clearAccount(accountId)
    }

    private suspend fun <T> remoteWrite(block: suspend () -> T): Resource<T> {
        val account = try {
            requireAccount()
        } catch (_: IllegalStateException) {
            return Resource.Error("Iniciá sesión para modificar tu biblioteca.")
        }
        return try {
            val confirmed = block()
            if (accounts.currentAccountId() != account) {
                Resource.Error("La cuenta cambió durante la operación. Volvé a cargar tu biblioteca.")
            } else {
                cache.clearAccount(account)
                Resource.Success(confirmed)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (exception: Exception) {
            Resource.Error(exception.toApiError().toMessage())
        }
    }

    private suspend fun requireAccount(): String =
        accounts.currentAccountId()?.takeIf { it.isNotBlank() }
            ?: error("No authenticated account")
}
