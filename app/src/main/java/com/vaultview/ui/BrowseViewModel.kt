package com.vaultview.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultview.data.MediaRepository
import com.vaultview.model.MediaItem
import com.vaultview.model.MediaType
import com.vaultview.providers.fake.FakeStorageProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowseUiState(
    val providerName: String = "",
    val currentPath: String = "/",
    val breadcrumbs: List<String> = listOf("Home"),
    val items: List<MediaItem> = emptyList(),
    val selectedItem: MediaItem? = null,
    val streamUrl: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class BrowseViewModel(
    private val repository: MediaRepository = MediaRepository(FakeStorageProvider())
) : ViewModel() {
    private val _uiState = MutableStateFlow(BrowseUiState(providerName = repository.providerName))
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        openFolder("/")
    }

    fun openFolder(path: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, selectedItem = null, streamUrl = null)
            }

            runCatching {
                repository.ensureAuthenticated()
                repository.listFolder(path)
            }.onSuccess { media ->
                _uiState.update {
                    it.copy(
                        currentPath = path,
                        breadcrumbs = path.toBreadcrumbs(),
                        items = media,
                        isLoading = false
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = throwable.message ?: "Unable to load folder")
                }
            }
        }
    }

    fun openItem(item: MediaItem) {
        when (item.type) {
            MediaType.Folder -> openFolder(item.path)
            MediaType.Image -> _uiState.update { it.copy(selectedItem = item, streamUrl = item.thumbnailUrl) }
            MediaType.Video -> loadVideo(item)
        }
    }

    fun closeViewer() {
        _uiState.update { it.copy(selectedItem = null, streamUrl = null) }
    }

    fun showPreviousImage() {
        moveImageSelection(-1)
    }

    fun showNextImage() {
        moveImageSelection(1)
    }

    private fun loadVideo(item: MediaItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedItem = item, streamUrl = null, errorMessage = null) }

            runCatching { repository.streamUrl(item) }
                .onSuccess { url -> _uiState.update { it.copy(streamUrl = url) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            selectedItem = null,
                            streamUrl = null,
                            errorMessage = throwable.message ?: "Unable to open video"
                        )
                    }
                }
        }
    }

    private fun moveImageSelection(offset: Int) {
        val state = _uiState.value
        val current = state.selectedItem ?: return
        val images = state.items.filter { it.type == MediaType.Image }
        val currentIndex = images.indexOfFirst { it.id == current.id }
        if (currentIndex == -1 || images.isEmpty()) return

        val nextIndex = (currentIndex + offset).floorMod(images.size)
        val next = images[nextIndex]
        _uiState.update { it.copy(selectedItem = next, streamUrl = next.thumbnailUrl) }
    }

    private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor

    private fun String.toBreadcrumbs(): List<String> {
        if (this == "/") return listOf("Home")
        return listOf("Home") + trim('/').split('/').filter { it.isNotBlank() }
    }
}
