package org.bookie.app.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.model.Community
import org.bookie.app.domain.model.CommunityPost
import org.bookie.app.domain.model.CreateCommunityInput
import org.bookie.app.domain.repository.AuthRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.CreateCommunity
import org.bookie.app.domain.usecase.GetCommunities
import org.bookie.app.domain.usecase.GetCommunityPosts
import org.bookie.app.domain.usecase.GetRecommendedCommunities

class CommunityViewModel(
    private val getCommunities: GetCommunities,
    private val createCommunity: CreateCommunity,
    private val getRecommended: GetRecommendedCommunities,
    private val getPosts: GetCommunityPosts,
    private val authRepository: AuthRepository? = null,
) : ViewModel() {
    private val mutableCatalog = MutableStateFlow<CommunityCatalogState>(CommunityCatalogState.Idle)
    val catalog = mutableCatalog.asStateFlow()

    private val mutableRecommendations =
        MutableStateFlow<CommunityRecommendationsState>(CommunityRecommendationsState.Idle)
    val recommendations = mutableRecommendations.asStateFlow()

    private val mutableSelected = MutableStateFlow<Community?>(null)
    val selected = mutableSelected.asStateFlow()

    private val mutablePosts = MutableStateFlow<CommunityPostsState>(CommunityPostsState.Idle)
    val posts = mutablePosts.asStateFlow()

    private val mutableCreate = MutableStateFlow(CommunityCreateUiState())
    val create = mutableCreate.asStateFlow()
    private var createVersion = 0L

    private var catalogVersion = 0L
    private var recommendationsVersion = 0L
    private var postsVersion = 0L

    fun openCreate() {
        mutableCreate.value = CommunityCreateUiState(visible = true)
    }

    fun closeCreate() {
        if (mutableCreate.value.saving) return
        createVersion++
        mutableCreate.value = CommunityCreateUiState()
    }

    fun onCreateNameChanged(value: String) {
        if (mutableCreate.value.saving) return
        mutableCreate.value = mutableCreate.value.copy(
            name = value, error = null, signInRequired = false,
        )
    }

    fun onCreateDescriptionChanged(value: String) {
        if (mutableCreate.value.saving) return
        mutableCreate.value = mutableCreate.value.copy(
            description = value, error = null, signInRequired = false,
        )
    }

    fun submitCreate() {
        val current = mutableCreate.value
        if (!current.visible || current.saving) return
        val name = current.name.trim()
        val description = current.description.trim()
        if (name.isBlank() || description.isBlank()) {
            mutableCreate.value = current.copy(
                error = "Completá el nombre y la descripción.",
                signInRequired = false,
            )
            return
        }
        // Public community browsing must not accidentally send an unauthorized POST.
        if (authRepository?.state?.value !is AuthState.SignedIn) {
            mutableCreate.value = current.copy(
                error = null, signInRequired = true,
            )
            return
        }
        val version = ++createVersion
        mutableCreate.value = current.copy(saving = true, error = null, signInRequired = false)
        viewModelScope.launch {
            val result = try {
                createCommunity(CreateCommunityInput(name, description))
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Resource.Error(error.message ?: "No se pudo crear la comunidad.")
            }
            if (version != createVersion) return@launch
            when (result) {
                is Resource.Success -> {
                    // Only a confirmed POST may alter the UI as if creation succeeded.
                    mutableCreate.value = CommunityCreateUiState()
                    catalogVersion++
                    mutableCatalog.value = CommunityCatalogState.Idle
                    loadCommunities()
                    selectCommunity(result.data)
                }
                is Resource.Error -> {
                    val requiresLogin = result.kind == ResourceErrorKind.UNAUTHORIZED ||
                        result.message.contains("HTTP 401")
                    val message = when (result.kind) {
                        ResourceErrorKind.CONFLICT -> "Ya existe una comunidad con ese nombre."
                        ResourceErrorKind.VALIDATION -> "Revisá el nombre y la descripción."
                        ResourceErrorKind.NETWORK -> "Sin conexión. Reintentá cuando tengas internet."
                        else -> result.message
                    }
                    mutableCreate.value = mutableCreate.value.copy(
                        saving = false,
                        error = if (requiresLogin) null else message,
                        signInRequired = requiresLogin,
                    )
                }
            }
        }
    }

    fun loadCommunities() {
        if (mutableCatalog.value !is CommunityCatalogState.Idle) return
        fetchCommunities(page = 1, append = false)
    }

    fun retryCommunities() = fetchCommunities(page = 1, append = false)

    fun loadMoreCommunities() {
        val current = mutableCatalog.value as? CommunityCatalogState.Ready ?: return
        if (current.loadingMore || !current.hasMore) return
        fetchCommunities(page = current.page + 1, append = true)
    }

    private fun fetchCommunities(page: Int, append: Boolean) {
        val prior = mutableCatalog.value as? CommunityCatalogState.Ready
        if (append && (prior == null || prior.loadingMore)) return
        val version = ++catalogVersion
        mutableCatalog.value = if (append) {
            prior!!.copy(loadingMore = true, pageError = null)
        } else CommunityCatalogState.Loading

        viewModelScope.launch {
            val result = try {
                getCommunities(page = page)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Resource.Error(error.message ?: "No se pudieron cargar las comunidades.")
            }
            if (version != catalogVersion) return@launch
            when (result) {
                is Resource.Success -> {
                    val data = result.data
                    val communities = if (append) {
                        (prior?.communities.orEmpty() + data.communities).distinctBy(Community::id)
                    } else data.communities
                    mutableCatalog.value = if (!append && data.total == 0) {
                        CommunityCatalogState.Empty
                    } else CommunityCatalogState.Ready(
                        communities = communities,
                        page = data.page,
                        totalPages = data.totalPages,
                    )
                }
                is Resource.Error -> mutableCatalog.value =
                    if (append && prior != null) prior.copy(pageError = result.message)
                    else CommunityCatalogState.Error(result.message)
            }
        }
    }

    fun loadRecommendations() {
        if (authRepository?.state?.value !is AuthState.SignedIn) {
            mutableRecommendations.value = CommunityRecommendationsState.SignInRequired
            return
        }
        val version = ++recommendationsVersion
        mutableRecommendations.value = CommunityRecommendationsState.Loading
        viewModelScope.launch {
            val result = try {
                getRecommended()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Resource.Error(error.message ?: "No se pudieron cargar las recomendaciones.")
            }
            if (version != recommendationsVersion) return@launch
            mutableRecommendations.value = when (result) {
                is Resource.Success -> if (result.data.isEmpty()) {
                    CommunityRecommendationsState.Empty
                } else {
                    CommunityRecommendationsState.Ready(result.data)
                }
                is Resource.Error -> if (
                    result.kind == ResourceErrorKind.UNAUTHORIZED ||
                    result.message.contains("HTTP 401")
                ) CommunityRecommendationsState.SignInRequired
                else CommunityRecommendationsState.Error(result.message)
            }
        }
    }

    fun selectCommunity(community: Community) {
        mutableSelected.value = community
        refreshPosts()
    }

    fun showCommunities() {
        postsVersion++
        mutableSelected.value = null
        mutablePosts.value = CommunityPostsState.Idle
    }

    fun refreshPosts() {
        val communityId = mutableSelected.value?.id ?: return
        val version = ++postsVersion
        mutablePosts.value = CommunityPostsState.Loading
        viewModelScope.launch {
            val result = try {
                getPosts(communityId)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Resource.Error(error.message ?: "No se pudieron cargar las publicaciones.")
            }
            if (version != postsVersion || mutableSelected.value?.id != communityId) return@launch
            mutablePosts.value = when (result) {
                is Resource.Success -> if (result.data.isEmpty()) CommunityPostsState.Empty
                else CommunityPostsState.Ready(result.data)
                is Resource.Error -> CommunityPostsState.Error(result.message)
            }
        }
    }
}

sealed interface CommunityCatalogState {
    data object Idle : CommunityCatalogState
    data object Loading : CommunityCatalogState
    data object Empty : CommunityCatalogState
    data class Error(val message: String) : CommunityCatalogState
    data class Ready(
        val communities: List<Community>,
        val page: Int,
        val totalPages: Int,
        val loadingMore: Boolean = false,
        val pageError: String? = null,
    ) : CommunityCatalogState {
        val hasMore: Boolean get() = page < totalPages
    }
}

sealed interface CommunityRecommendationsState {
    data object Idle : CommunityRecommendationsState
    data object Loading : CommunityRecommendationsState
    data object Empty : CommunityRecommendationsState
    data object SignInRequired : CommunityRecommendationsState
    data class Error(val message: String) : CommunityRecommendationsState
    data class Ready(val communities: List<Community>) : CommunityRecommendationsState
}

sealed interface CommunityPostsState {
    data object Idle : CommunityPostsState
    data object Loading : CommunityPostsState
    data object Empty : CommunityPostsState
    data class Error(val message: String) : CommunityPostsState
    data class Ready(val posts: List<CommunityPost>) : CommunityPostsState
}

data class CommunityCreateUiState(
    val visible: Boolean = false,
    val name: String = "",
    val description: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val signInRequired: Boolean = false,
)
