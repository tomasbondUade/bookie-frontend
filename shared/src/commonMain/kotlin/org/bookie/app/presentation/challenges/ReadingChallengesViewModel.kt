package org.bookie.app.presentation.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.ReadingChallenge
import org.bookie.app.domain.model.ReadingChallengeProgress
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetMyReadingChallengeProgress
import org.bookie.app.domain.usecase.GetReadingChallenges
import org.bookie.app.domain.usecase.JoinReadingChallenge

class ReadingChallengesViewModel(
    private val getChallenges: GetReadingChallenges,
    private val getMyProgress: GetMyReadingChallengeProgress,
    private val joinChallenge: JoinReadingChallenge,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<ReadingChallengesUiState>(ReadingChallengesUiState.Idle)
    val uiState = mutableUiState.asStateFlow()
    private var requestVersion = 0L

    fun load() {
        val current = mutableUiState.value
        if (current is ReadingChallengesUiState.Loading ||
            current is ReadingChallengesUiState.Ready && (current.refreshing || current.joiningChallengeId != null)
        ) return
        fetch()
    }

    fun join(challengeId: String) {
        val current = mutableUiState.value as? ReadingChallengesUiState.Ready ?: return
        val challenge = current.challenges.firstOrNull { it.id == challengeId } ?: return
        if (challenge.joined || current.joiningChallengeId != null || current.refreshing) return

        val expectedVersion = ++requestVersion
        mutableUiState.value = current.copy(
            joiningChallengeId = challengeId,
            message = null,
            actionError = null,
            sessionExpired = false,
        )
        viewModelScope.launch {
            val result = try {
                joinChallenge(challengeId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Resource.Error(exception.message ?: "No se pudo enviar la solicitud.")
            }
            if (expectedVersion != requestVersion) return@launch

            when (result) {
                is Resource.Success -> fetch("Te uniste al reto.")
                is Resource.Error -> when (result.kind) {
                    ResourceErrorKind.CONFLICT -> fetch("Ya participás en este reto.")
                    ResourceErrorKind.NOT_FOUND -> fetch("El reto ya no está disponible.")
                    else -> mutableUiState.value = current.copy(
                        joiningChallengeId = null,
                        actionError = if (result.kind == ResourceErrorKind.UNAUTHORIZED) {
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

    fun retry() = load()

    fun dismissMessage() {
        val current = mutableUiState.value as? ReadingChallengesUiState.Ready ?: return
        mutableUiState.value = current.copy(message = null, actionError = null, sessionExpired = false)
    }

    private fun fetch(message: String? = null) {
        val previous = mutableUiState.value as? ReadingChallengesUiState.Ready
        val expectedVersion = ++requestVersion
        mutableUiState.value = if (previous == null) {
            ReadingChallengesUiState.Loading
        } else {
            previous.copy(
                refreshing = true,
                joiningChallengeId = null,
                message = null,
                actionError = null,
                sessionExpired = false,
            )
        }

        viewModelScope.launch {
            val result = try {
                val challenges = getChallenges()
                if (challenges is Resource.Error) return@launch resultState(challenges, previous, expectedVersion)
                val progress = getMyProgress()
                if (progress is Resource.Error) return@launch resultState(progress, previous, expectedVersion)
                ReadingChallengesLoad.Success(
                    challenges = (challenges as Resource.Success).data,
                    progress = (progress as Resource.Success).data,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ReadingChallengesLoad.Failure(
                    message = exception.message ?: "No se pudieron cargar los retos.",
                    kind = ResourceErrorKind.GENERAL,
                )
            }

            if (expectedVersion != requestVersion) return@launch
            mutableUiState.value = when (result) {
                is ReadingChallengesLoad.Success -> if (result.challenges.isEmpty()) {
                    ReadingChallengesUiState.Empty
                } else {
                    ReadingChallengesUiState.Ready(
                        challenges = result.challenges,
                        progress = result.progress,
                        message = message,
                    )
                }
                is ReadingChallengesLoad.Failure -> if (previous != null) {
                    previous.copy(
                        refreshing = false,
                        joiningChallengeId = null,
                        actionError = result.message,
                        sessionExpired = result.kind == ResourceErrorKind.UNAUTHORIZED,
                    )
                } else {
                    ReadingChallengesUiState.Error(
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

    private fun resultState(
        error: Resource.Error,
        previous: ReadingChallengesUiState.Ready?,
        expectedVersion: Long,
    ) {
        if (expectedVersion != requestVersion) return
        mutableUiState.value = if (previous != null) {
            previous.copy(
                refreshing = false,
                joiningChallengeId = null,
                actionError = error.message,
                sessionExpired = error.kind == ResourceErrorKind.UNAUTHORIZED,
            )
        } else {
            ReadingChallengesUiState.Error(
                message = if (error.kind == ResourceErrorKind.UNAUTHORIZED) {
                    "Tu sesión venció o no está disponible. Iniciá sesión nuevamente."
                } else {
                    error.message
                },
                sessionExpired = error.kind == ResourceErrorKind.UNAUTHORIZED,
            )
        }
    }
}

sealed interface ReadingChallengesUiState {
    data object Idle : ReadingChallengesUiState
    data object Loading : ReadingChallengesUiState
    data object Empty : ReadingChallengesUiState
    data class Error(val message: String, val sessionExpired: Boolean = false) : ReadingChallengesUiState
    data class Ready(
        val challenges: List<ReadingChallenge>,
        val progress: List<ReadingChallengeProgress>,
        val refreshing: Boolean = false,
        val joiningChallengeId: String? = null,
        val message: String? = null,
        val actionError: String? = null,
        val sessionExpired: Boolean = false,
    ) : ReadingChallengesUiState
}

private sealed interface ReadingChallengesLoad {
    data class Success(
        val challenges: List<ReadingChallenge>,
        val progress: List<ReadingChallengeProgress>,
    ) : ReadingChallengesLoad
    data class Failure(val message: String, val kind: ResourceErrorKind) : ReadingChallengesLoad
}
