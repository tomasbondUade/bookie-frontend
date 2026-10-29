package org.bookie.app.presentation.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.*
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetActiveSeries
import org.bookie.app.domain.usecase.GetSeriesProgress
import org.bookie.app.domain.usecase.GetSeriesReadingMap

sealed interface SeriesLoad<out T> {
    data object Idle : SeriesLoad<Nothing>
    data object Loading : SeriesLoad<Nothing>
    data class Ready<T>(val value: T) : SeriesLoad<T>
    data class Failed(val message: String, val signInRequired: Boolean = false) : SeriesLoad<Nothing>
}

data class SeriesReadingUiState(
    val active: SeriesLoad<List<SeriesProgress>> = SeriesLoad.Idle,
    val selectedSeriesId: String? = null,
    val selectedPath: ReadingPath = ReadingPath.RECOMMENDED,
    val progress: SeriesLoad<SeriesProgress> = SeriesLoad.Idle,
    val map: SeriesLoad<SeriesReadingMap> = SeriesLoad.Idle,
)

class SeriesReadingViewModel(
    private val getActive: GetActiveSeries,
    private val getProgress: GetSeriesProgress,
    private val getMap: GetSeriesReadingMap,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SeriesReadingUiState())
    val uiState = _uiState.asStateFlow()
    private var activeVersion = 0L
    private var detailVersion = 0L
    private var mapVersion = 0L

    fun loadActive() {
        val version = ++activeVersion
        _uiState.value = _uiState.value.copy(active = SeriesLoad.Loading)
        viewModelScope.launch {
            val result = safely { getActive() }
            if (version == activeVersion) {
                _uiState.value = _uiState.value.copy(active = result.toLoad())
            }
        }
    }

    fun selectSeries(seriesId: String) {
        if (seriesId.isBlank()) return
        ++detailVersion
        ++mapVersion
        _uiState.value = _uiState.value.copy(
            selectedSeriesId = seriesId,
            selectedPath = ReadingPath.RECOMMENDED,
            progress = SeriesLoad.Loading,
            map = SeriesLoad.Loading,
        )
        loadProgress()
        loadMap()
    }

    fun clearSelection() {
        ++detailVersion
        ++mapVersion
        _uiState.value = _uiState.value.copy(
            selectedSeriesId = null, progress = SeriesLoad.Idle, map = SeriesLoad.Idle,
        )
    }

    fun changePath(path: ReadingPath) {
        if (_uiState.value.selectedSeriesId == null || _uiState.value.selectedPath == path) return
        _uiState.value = _uiState.value.copy(selectedPath = path)
        loadMap()
    }

    fun retryDetail() {
        loadProgress()
        loadMap()
    }

    fun loadProgress() {
        val id = _uiState.value.selectedSeriesId ?: return
        val version = ++detailVersion
        _uiState.value = _uiState.value.copy(progress = SeriesLoad.Loading)
        viewModelScope.launch {
            val result = safely { getProgress(id) }
            if (version == detailVersion && _uiState.value.selectedSeriesId == id) {
                _uiState.value = _uiState.value.copy(progress = result.toLoad())
            }
        }
    }

    fun loadMap() {
        val state = _uiState.value
        val id = state.selectedSeriesId ?: return
        val path = state.selectedPath
        val version = ++mapVersion
        _uiState.value = state.copy(map = SeriesLoad.Loading)
        viewModelScope.launch {
            val result = safely { getMap(id, path) }
            if (version == mapVersion && _uiState.value.selectedSeriesId == id &&
                _uiState.value.selectedPath == path) {
                _uiState.value = _uiState.value.copy(map = result.toLoad())
            }
        }
    }

    private suspend fun <T> safely(block: suspend () -> Resource<T>): Resource<T> = try {
        block()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        Resource.Error(error.message ?: "No se pudieron cargar los datos de la saga.")
    }

    private fun <T> Resource<T>.toLoad(): SeriesLoad<T> = when (this) {
        is Resource.Success -> SeriesLoad.Ready(data)
        is Resource.Error -> SeriesLoad.Failed(
            message = if (kind == ResourceErrorKind.UNAUTHORIZED) {
                "Tu sesión venció o no está disponible. Iniciá sesión nuevamente."
            } else message,
            signInRequired = kind == ResourceErrorKind.UNAUTHORIZED,
        )
    }
}
