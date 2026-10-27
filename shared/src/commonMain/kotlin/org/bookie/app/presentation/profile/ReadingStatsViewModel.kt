package org.bookie.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetReadingStats

class ReadingStatsViewModel(
    private val getReadingStats: GetReadingStats,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReadingStatsUiState>(ReadingStatsUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun load() {
        if (_uiState.value is ReadingStatsUiState.Loading) return

        _uiState.value = ReadingStatsUiState.Loading
        viewModelScope.launch {
            val nextState = try {
                when (val result = getReadingStats()) {
                    is Resource.Success -> {
                        if (result.data.hasData) {
                            ReadingStatsUiState.Success(result.data)
                        } else {
                            ReadingStatsUiState.Empty
                        }
                    }

                    is Resource.Error -> ReadingStatsUiState.Error(
                        message = if (result.kind == ResourceErrorKind.UNAUTHORIZED) {
                            "Tu sesión venció o no está disponible. Iniciá sesión nuevamente."
                        } else {
                            result.message
                        },
                        sessionExpired = result.kind == ResourceErrorKind.UNAUTHORIZED,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ReadingStatsUiState.Error(
                    message = exception.message ?: "No se pudieron cargar las estadísticas.",
                )
            }

            _uiState.value = nextState
        }
    }

    fun retry() = load()
}
