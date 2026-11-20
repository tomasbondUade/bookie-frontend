package org.bookie.app.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.AuthState
import org.bookie.app.domain.model.Community
import org.bookie.app.domain.model.CommunityComment
import org.bookie.app.domain.model.CommunityPost
import org.bookie.app.domain.model.CreateCommunityCommentInput
import org.bookie.app.domain.model.CreateCommunityInput
import org.bookie.app.domain.repository.AuthRepository
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.result.ResourceErrorKind
import org.bookie.app.domain.usecase.CreateCommunity
import org.bookie.app.domain.usecase.CreateCommunityComment
import org.bookie.app.domain.usecase.GetCommunities
import org.bookie.app.domain.usecase.GetCommunityComments
import org.bookie.app.domain.usecase.GetCommunityPosts
import org.bookie.app.domain.usecase.GetRecommendedCommunities

class CommunityViewModel(
    private val getCommunities: GetCommunities,
    private val createCommunity: CreateCommunity,
    private val getRecommended: GetRecommendedCommunities,
    private val getPosts: GetCommunityPosts,
    private val getComments: GetCommunityComments,
    private val createComment: CreateCommunityComment,
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

    private val mutableCommentThreads = MutableStateFlow<Map<String, CommunityCommentThreadState>>(emptyMap())
    val commentThreads = mutableCommentThreads.asStateFlow()

    private val mutableCreate = MutableStateFlow(CommunityCreateUiState())
    val create = mutableCreate.asStateFlow()
    private var createVersion = 0L

    private var catalogVersion = 0L
    private var recommendationsVersion = 0L
    private var postsVersion = 0L
    private val commentLoadVersions = mutableMapOf<String, Long>()

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

    fun toggleCommentComposer(postId: String) {
        if (!isPostVisible(postId)) return
        val expanded = !(mutableCommentThreads.value[postId]?.expanded ?: false)
        updateCommentThread(postId) {
            it.copy(expanded = expanded, error = null, notice = null)
        }
        if (expanded) loadComments(postId)
    }

    fun retryComments(postId: String) {
        val thread = mutableCommentThreads.value[postId] ?: return
        if (!thread.expanded || thread.loading) return
        loadComments(postId)
    }

    private fun loadComments(postId: String) {
        val current = mutableCommentThreads.value[postId] ?: CommunityCommentThreadState()
        if (!isPostVisible(postId) || current.loading) return

        val version = (commentLoadVersions[postId] ?: 0L) + 1L
        commentLoadVersions[postId] = version
        updateCommentThread(postId) { it.copy(loading = true, loadError = null) }

        viewModelScope.launch {
            val result = try {
                getComments(postId)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Resource.Error(error.message ?: "No se pudieron cargar los comentarios.")
            }
            if (commentLoadVersions[postId] != version || !isPostVisible(postId)) return@launch

            val latest = mutableCommentThreads.value[postId] ?: CommunityCommentThreadState()
            when (result) {
                is Resource.Success -> {
                    if (result.data.any { it.postId != postId }) {
                        mutableCommentThreads.value = mutableCommentThreads.value + (postId to latest.copy(
                            loading = false,
                            loadError = "El servidor devolvió comentarios de otra publicación.",
                        ))
                    } else {
                        mutableCommentThreads.value = mutableCommentThreads.value + (postId to latest.copy(
                            comments = mergeComments(result.data, latest.comments),
                            loading = false,
                            loaded = true,
                            loadError = null,
                        ))
                    }
                }
                is Resource.Error -> {
                    mutableCommentThreads.value = mutableCommentThreads.value + (postId to latest.copy(
                        loading = false,
                        loadError = result.message,
                    ))
                }
            }
        }
    }

    fun onCommentDraftChanged(postId: String, value: String) {
        updateCommentThread(postId) {
            if (it.sending) it else it.copy(draft = value, error = null, notice = null, signInRequired = false)
        }
    }

    fun replyToComment(postId: String, comment: CommunityComment) {
        if (comment.postId != postId || !isPostVisible(postId)) return
        val thread = mutableCommentThreads.value[postId] ?: CommunityCommentThreadState()
        if (thread.comments.none { it.id == comment.id }) return
        updateCommentThread(postId) {
            it.copy(
                expanded = true,
                replyTo = comment,
                error = null,
                notice = null,
                signInRequired = false,
            )
        }
    }

    fun cancelReply(postId: String) {
        updateCommentThread(postId) { it.copy(replyTo = null, error = null, signInRequired = false) }
    }

    fun dismissCommentFeedback(postId: String) {
        updateCommentThread(postId) { it.copy(error = null, notice = null, signInRequired = false) }
    }

    fun sendComment(postId: String) {
        if (!isPostVisible(postId)) return
        val current = mutableCommentThreads.value[postId] ?: CommunityCommentThreadState()
        if (current.sending) return
        if (authRepository?.state?.value !is AuthState.SignedIn) {
            updateCommentThread(postId) {
                it.copy(expanded = true, signInRequired = true, error = null, notice = null)
            }
            return
        }
        val content = current.draft.trim()
        if (content.isEmpty() || content.length > 5000) {
            updateCommentThread(postId) {
                it.copy(
                    expanded = true,
                    error = "El comentario debe tener entre 1 y 5000 caracteres.",
                    notice = null,
                )
            }
            return
        }
        current.replyTo?.let { parent ->
            if (parent.postId != postId || current.comments.none { it.id == parent.id }) {
                updateCommentThread(postId) {
                    it.copy(replyTo = null, error = "No se pudo validar el comentario al que respondés.")
                }
                return
            }
        }

        updateCommentThread(postId) {
            it.copy(sending = true, error = null, notice = null, signInRequired = false)
        }
        viewModelScope.launch {
            val result = try {
                createComment(postId, CreateCommunityCommentInput(content, current.replyTo?.id))
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Resource.Error(error.message ?: "No se pudo enviar el comentario.")
            }
            val latest = mutableCommentThreads.value[postId] ?: CommunityCommentThreadState()
            when (result) {
                is Resource.Success -> {
                    val comment = result.data
                    if (comment.postId != postId || comment.parentId != current.replyTo?.id) {
                        mutableCommentThreads.value = mutableCommentThreads.value + (postId to latest.copy(
                            sending = false,
                            error = "El servidor devolvió una respuesta que no coincide con esta publicación.",
                        ))
                        return@launch
                    }
                    mutableCommentThreads.value = mutableCommentThreads.value + (postId to latest.copy(
                        comments = mergeComments(latest.comments, listOf(comment)),
                        draft = "",
                        replyTo = null,
                        sending = false,
                        signInRequired = false,
                        error = null,
                        notice = "Comentario confirmado por el servidor.",
                    ))
                }
                is Resource.Error -> {
                    val requiresLogin = result.kind == ResourceErrorKind.UNAUTHORIZED
                    val message = when (result.kind) {
                        ResourceErrorKind.VALIDATION -> "Revisá el comentario; debe tener entre 1 y 5000 caracteres."
                        ResourceErrorKind.NOT_FOUND -> "La publicación o el comentario al que respondés ya no existe."
                        ResourceErrorKind.NETWORK -> "Sin conexión. Revisá tu conexión e intentá de nuevo."
                        else -> result.message
                    }
                    mutableCommentThreads.value = mutableCommentThreads.value + (postId to latest.copy(
                        sending = false,
                        error = if (requiresLogin) "Iniciá sesión para comentar." else message,
                        signInRequired = requiresLogin,
                        notice = null,
                    ))
                }
            }
        }
    }

    private fun isPostVisible(postId: String): Boolean =
        (mutablePosts.value as? CommunityPostsState.Ready)?.posts?.any { it.id == postId } == true

    private fun mergeComments(
        first: List<CommunityComment>,
        second: List<CommunityComment>,
    ): List<CommunityComment> = (first + second)
        .distinctBy(CommunityComment::id)
        .sortedWith(compareBy<CommunityComment> { it.createdAt }.thenBy { it.id })

    private fun updateCommentThread(
        postId: String,
        transform: (CommunityCommentThreadState) -> CommunityCommentThreadState,
    ) {
        val current = mutableCommentThreads.value[postId] ?: CommunityCommentThreadState()
        mutableCommentThreads.value = mutableCommentThreads.value + (postId to transform(current))
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

data class CommunityCommentThreadState(
    val expanded: Boolean = false,
    val comments: List<CommunityComment> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val loadError: String? = null,
    val draft: String = "",
    val replyTo: CommunityComment? = null,
    val sending: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val signInRequired: Boolean = false,
)
