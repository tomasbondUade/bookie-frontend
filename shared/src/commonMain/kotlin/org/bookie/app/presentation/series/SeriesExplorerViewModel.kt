package org.bookie.app.presentation.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.SeriesCatalogPage
import org.bookie.app.domain.model.UniverseCatalogEntry
import org.bookie.app.domain.model.UniverseDetail
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetSeriesCatalog
import org.bookie.app.domain.usecase.GetUniverseCatalog
import org.bookie.app.domain.usecase.GetUniverseDetail

enum class SeriesExplorerTab { SERIES, UNIVERSES }

sealed interface CatalogLoad<out T> {
    data object Idle : CatalogLoad<Nothing>
    data object Loading : CatalogLoad<Nothing>
    data class Ready<T>(val value: T) : CatalogLoad<T>
    data class Failed(
        val message: String,
        val signInRequired: Boolean = false,
        val accessDenied: Boolean = false,
    ) : CatalogLoad<Nothing>
}

data class SeriesExplorerUiState(
    val tab: SeriesExplorerTab = SeriesExplorerTab.SERIES,
    val series: CatalogLoad<SeriesCatalogPage> = CatalogLoad.Idle,
    val universes: CatalogLoad<List<UniverseCatalogEntry>> = CatalogLoad.Idle,
    val universeFilterId: String? = null,
    val page: Int = 1,
    val selectedUniverseId: String? = null,
    val universeDetail: CatalogLoad<UniverseDetail> = CatalogLoad.Idle,
)

class SeriesExplorerViewModel(
    private val getSeriesCatalog: GetSeriesCatalog,
    private val getUniverseCatalog: GetUniverseCatalog,
    private val getUniverseDetail: GetUniverseDetail,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SeriesExplorerUiState())
    val uiState = _uiState.asStateFlow()

    private var seriesVersion = 0L
    private var universesVersion = 0L
    private var detailVersion = 0L

    fun load() {
        loadSeries(page = 1)
        loadUniverses()
    }

    fun selectTab(tab: SeriesExplorerTab) {
        _uiState.value = _uiState.value.copy(tab = tab)
        if (tab == SeriesExplorerTab.SERIES && _uiState.value.series is CatalogLoad.Idle) {
            loadSeries(page = 1)
        }
        if (tab == SeriesExplorerTab.UNIVERSES && _uiState.value.universes is CatalogLoad.Idle) {
            loadUniverses()
        }
    }

    fun loadSeries(page: Int = _uiState.value.page) {
        val current = _uiState.value
        val safePage = page.coerceAtLeast(1)
        val version = ++seriesVersion
        _uiState.value = current.copy(page = safePage, series = CatalogLoad.Loading)
        viewModelScope.launch {
            val result = safely { getSeriesCatalog(safePage, PAGE_SIZE, current.universeFilterId) }
            if (version == seriesVersion && _uiState.value.universeFilterId == current.universeFilterId) {
                _uiState.value = _uiState.value.copy(series = result.toLoad())
            }
        }
    }

    fun setUniverseFilter(universeId: String?) {
        val normalized = universeId?.takeIf(String::isNotBlank)
        if (_uiState.value.universeFilterId == normalized) return
        _uiState.value = _uiState.value.copy(universeFilterId = normalized, page = 1)
        loadSeries(page = 1)
    }

    fun loadUniverses() {
        if (_uiState.value.universes is CatalogLoad.Loading) return
        val version = ++universesVersion
        _uiState.value = _uiState.value.copy(universes = CatalogLoad.Loading)
        viewModelScope.launch {
            val result = safely { getUniverseCatalog() }
            if (version == universesVersion) {
                _uiState.value = _uiState.value.copy(universes = result.toLoad())
            }
        }
    }

    fun selectUniverse(universeId: String) {
        if (universeId.isBlank()) return
        val version = ++detailVersion
        _uiState.value = _uiState.value.copy(
            selectedUniverseId = universeId,
            universeDetail = CatalogLoad.Loading,
        )
        viewModelScope.launch {
            val result = safely { getUniverseDetail(universeId) }
            if (version == detailVersion && _uiState.value.selectedUniverseId == universeId) {
                _uiState.value = _uiState.value.copy(universeDetail = result.toLoad())
            }
        }
    }

    fun clearUniverseSelection() {
        ++detailVersion
        _uiState.value = _uiState.value.copy(
            selectedUniverseId = null,
            universeDetail = CatalogLoad.Idle,
        )
    }

    fun retryUniverseDetail() {
        _uiState.value.selectedUniverseId?.let(::selectUniverse)
    }

    private suspend fun <T> safely(block: suspend () -> Resource<T>): Resource<T> = try {
        block()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        Resource.Error(error.message ?: "No se pudo completar la solicitud.")
    }

    private fun <T> Resource<T>.toLoad(): CatalogLoad<T> = when (this) {
        is Resource.Success -> CatalogLoad.Ready(data)
        is Resource.Error -> CatalogLoad.Failed(
            message = if (kind == ResourceErrorKind.UNAUTHORIZED) {
                "Tu sesión venció o no está disponible. Iniciá sesión nuevamente."
            } else if (kind == ResourceErrorKind.FORBIDDEN) {
                "No tenés permiso para consultar estos universos."
            } else message,
            signInRequired = kind == ResourceErrorKind.UNAUTHORIZED,
            accessDenied = kind == ResourceErrorKind.FORBIDDEN,
        )
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
