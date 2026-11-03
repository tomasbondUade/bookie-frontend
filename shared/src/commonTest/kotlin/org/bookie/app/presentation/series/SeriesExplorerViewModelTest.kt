package org.bookie.app.presentation.series

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bookie.app.domain.model.ReadingPath
import org.bookie.app.domain.model.SeriesCatalogEntry
import org.bookie.app.domain.model.SeriesCatalogPage
import org.bookie.app.domain.model.SeriesProgress
import org.bookie.app.domain.model.SeriesReadingMap
import org.bookie.app.domain.model.UniverseCatalogEntry
import org.bookie.app.domain.model.UniverseDetail
import org.bookie.app.domain.repository.SeriesReadingRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.GetSeriesCatalog
import org.bookie.app.domain.usecase.GetUniverseCatalog
import org.bookie.app.domain.usecase.GetUniverseDetail
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesExplorerViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads series and universes then applies filter and pagination`() = runTest {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)

        viewModel.load()
        runCurrent()
        assertIs<CatalogLoad.Ready<SeriesCatalogPage>>(viewModel.uiState.value.series)
        assertIs<CatalogLoad.Ready<List<UniverseCatalogEntry>>>(viewModel.uiState.value.universes)

        viewModel.setUniverseFilter("universe-1")
        runCurrent()
        viewModel.loadSeries(page = 2)
        runCurrent()

        assertEquals(
            listOf(null, "universe-1", "universe-1"),
            repository.seriesRequests.map { it.third },
        )
        assertEquals(listOf(1, 1, 2), repository.seriesRequests.map { it.first })
        assertEquals(2, viewModel.uiState.value.page)
    }

    @Test
    fun `maps expired session and forbidden access for protected universe routes`() = runTest {
        val unauthorized = FakeRepository(
            universes = Resource.Error("HTTP 401", ResourceErrorKind.UNAUTHORIZED),
        )
        val unauthorizedViewModel = viewModel(unauthorized)
        unauthorizedViewModel.loadUniverses()
        runCurrent()
        val loginError = assertIs<CatalogLoad.Failed>(unauthorizedViewModel.uiState.value.universes)
        assertEquals(true, loginError.signInRequired)

        val forbidden = FakeRepository(
            universes = Resource.Error("HTTP 403", ResourceErrorKind.FORBIDDEN),
        )
        val forbiddenViewModel = viewModel(forbidden)
        forbiddenViewModel.loadUniverses()
        runCurrent()
        val accessError = assertIs<CatalogLoad.Failed>(forbiddenViewModel.uiState.value.universes)
        assertEquals(true, accessError.accessDenied)
        assertEquals("No tenés permiso para consultar estos universos.", accessError.message)
    }

    @Test
    fun `selecting a universe loads its detail and supports returning to the catalog`() = runTest {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)

        viewModel.selectUniverse("universe-1")
        runCurrent()

        assertEquals("universe-1", viewModel.uiState.value.selectedUniverseId)
        assertEquals(
            "Universo",
            assertIs<CatalogLoad.Ready<UniverseDetail>>(viewModel.uiState.value.universeDetail).value.name,
        )
        viewModel.clearUniverseSelection()
        assertEquals(null, viewModel.uiState.value.selectedUniverseId)
        assertIs<CatalogLoad.Idle>(viewModel.uiState.value.universeDetail)
    }

    private fun viewModel(repository: FakeRepository) = SeriesExplorerViewModel(
        GetSeriesCatalog(repository),
        GetUniverseCatalog(repository),
        GetUniverseDetail(repository),
    )

    private class FakeRepository(
        private val series: Resource<SeriesCatalogPage> = Resource.Success(
            SeriesCatalogPage(emptyList(), page = 1, limit = 20, total = 0, totalPages = 0),
        ),
        private val universes: Resource<List<UniverseCatalogEntry>> = Resource.Success(emptyList()),
        private val detail: Resource<UniverseDetail> = Resource.Success(
            UniverseDetail("universe-1", "Universo", null, null, null, emptyList()),
        ),
    ) : SeriesReadingRepository {
        val seriesRequests = mutableListOf<Triple<Int, Int, String?>>()

        override suspend fun getSeriesCatalog(
            page: Int,
            limit: Int,
            universeId: String?,
        ): Resource<SeriesCatalogPage> {
            seriesRequests += Triple(page, limit, universeId)
            return series
        }

        override suspend fun getUniverses() = universes
        override suspend fun getUniverseDetail(universeId: String) = detail
        override suspend fun getActiveSeries(): Resource<List<SeriesProgress>> = Resource.Success(emptyList())
        override suspend fun getProgress(seriesId: String): Resource<SeriesProgress> =
            Resource.Error("Not used")
        override suspend fun getMap(seriesId: String, path: ReadingPath): Resource<SeriesReadingMap> =
            Resource.Error("Not used")
    }
}
