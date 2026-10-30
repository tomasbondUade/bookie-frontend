package org.bookie.app.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bookie.app.domain.model.*
import org.bookie.app.domain.result.Resource
import org.bookie.app.domain.usecase.CreateCommunityPost
import org.bookie.app.domain.usecase.GetCommunityPosts

data class CommunityPostsUiState(
    val posts: List<CommunityPost> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val title: String = "",
    val content: String = "",
    val publishing: Boolean = false,
    val publishError: String? = null,
    val publishSuccess: Boolean = false,
    val selectedPost: CommunityPost? = null,
)

class CommunityPostsViewModel(
    private val communityId: String,
    private val getPosts: GetCommunityPosts,
    private val createPost: CreateCommunityPost,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CommunityPostsUiState())
    val uiState = _uiState.asStateFlow()
    private var requestVersion = 0L

    fun load() {
        if (_uiState.value.loading) return
        val version = ++requestVersion
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val result = try { getPosts(communityId) }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { Resource.Error(error.message ?: "No se pudo cargar el feed.") }
            if (version != requestVersion) return@launch
            _uiState.value = when (result) {
                is Resource.Success -> _uiState.value.copy(posts = result.data, loading = false)
                is Resource.Error -> _uiState.value.copy(error = result.message, loading = false)
            }
        }
    }

    fun selectPost(post: CommunityPost?) {
        _uiState.value = _uiState.value.copy(selectedPost = post)
    }

    fun onTitleChanged(value: String) {
        if (!_uiState.value.publishing) _uiState.value =
            _uiState.value.copy(title = value, publishError = null, publishSuccess = false)
    }

    fun onContentChanged(value: String) {
        if (!_uiState.value.publishing) _uiState.value =
            _uiState.value.copy(content = value, publishError = null, publishSuccess = false)
    }

    fun publish() {
        val current = _uiState.value
        if (current.publishing) return
        val title = current.title.trim()
        val content = current.content.trim()
        if (title.isBlank() || content.isBlank()) {
            _uiState.value = current.copy(publishError = "Completá título y contenido.")
            return
        }
        _uiState.value = current.copy(publishing = true, publishError = null, publishSuccess = false)
        viewModelScope.launch {
            val result = try { createPost(communityId, CreateCommunityPostInput(title, content)) }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { Resource.Error(error.message ?: "No se pudo publicar.") }
            when (result) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        title = "", content = "", publishing = false, publishSuccess = true,
                    )
                    load() // Never insert an unconfirmed local post into the feed.
                }
                is Resource.Error -> _uiState.value =
                    _uiState.value.copy(publishing = false, publishError = result.message)
            }
        }
    }
}
