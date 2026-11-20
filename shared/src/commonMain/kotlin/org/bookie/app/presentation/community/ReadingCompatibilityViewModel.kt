package org.bookie.app.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.ReadingCompatibility
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetReadingCompatibility

class ReadingCompatibilityViewModel(
    private val getReadingCompatibility: GetReadingCompatibility,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<ReadingCompatibilityUiState>(ReadingCompatibilityUiState.Idle)
    val uiState = mutableUiState.asStateFlow()

    private var requestVersion = 0L
    private var requestJob: Job? = null

    fun compareWith(targetUserId: String) {
        val normalizedId = targetUserId.trim()
        if (normalizedId.isBlank()) return
        load(normalizedId)
    }

    fun retry() {
        val targetUserId = when (val state = mutableUiState.value) {
            is ReadingCompatibilityUiState.Error -> state.targetUserId
            else -> return
        }
        load(targetUserId)
    }

    fun dismiss() {
        requestVersion++
        requestJob?.cancel()
        mutableUiState.value = ReadingCompatibilityUiState.Idle
    }

    private fun load(targetUserId: String) {
        val expectedVersion = ++requestVersion
        requestJob?.cancel()
        mutableUiState.value = ReadingCompatibilityUiState.Loading(targetUserId)

        requestJob = viewModelScope.launch {
            val result = try {
                getReadingCompatibility(targetUserId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Resource.Error(exception.message ?: "No se pudo consultar la compatibilidad.")
            }
            if (expectedVersion != requestVersion) return@launch

            mutableUiState.value = when (result) {
                is Resource.Success -> ReadingCompatibilityUiState.Ready(targetUserId, result.data)
                is Resource.Error -> ReadingCompatibilityUiState.Error(
                    targetUserId = targetUserId,
                    message = if (result.kind == ResourceErrorKind.UNAUTHORIZED) {
                        "Tu sesión venció o no está disponible. Iniciá sesión nuevamente."
                    } else {
                        result.message
                    },
                    sessionExpired = result.kind == ResourceErrorKind.UNAUTHORIZED,
                )
            }
        }
    }
}

sealed interface ReadingCompatibilityUiState {
    data object Idle : ReadingCompatibilityUiState
    data class Loading(val targetUserId: String) : ReadingCompatibilityUiState
    data class Ready(
        val targetUserId: String,
        val compatibility: ReadingCompatibility,
    ) : ReadingCompatibilityUiState
    data class Error(
        val targetUserId: String,
        val message: String,
        val sessionExpired: Boolean = false,
    ) : ReadingCompatibilityUiState
}
