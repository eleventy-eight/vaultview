package com.vaultview.ui

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultview.data.MediaRepository
import com.vaultview.model.MediaItem
import com.vaultview.model.MediaType
import com.vaultview.providers.LoginCredentials
import com.vaultview.providers.fake.FakeStorageProvider
import com.vaultview.providers.mega.MegaProvider
import com.vaultview.providers.mega.SdkMegaClient
import com.vaultview.providers.mega.SharedPreferencesMegaSessionStore
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
    val isAuthenticated: Boolean = false,
    val isLoginInProgress: Boolean = false,
    val requiresTwoFactorCode: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class BrowseViewModel(
    private var repository: MediaRepository,
    private val primaryRepositoryFactory: () -> MediaRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BrowseUiState(providerName = repository.providerName))
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        checkAuthentication()
    }

    fun login(email: String, password: String, twoFactorCode: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoginInProgress = true, isLoading = false, errorMessage = null)
            }

            runCatching {
                repository.login(
                    LoginCredentials(
                        email = email.trim(),
                        password = password,
                        twoFactorCode = twoFactorCode?.trim()?.takeIf { it.isNotEmpty() }
                    )
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        isLoginInProgress = false,
                        requiresTwoFactorCode = false,
                        errorMessage = null
                    )
                }
                openFolder("/")
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        isLoginInProgress = false,
                        requiresTwoFactorCode = true,
                        errorMessage = throwable.message ?: "Unable to sign in"
                    )
                }
            }
        }
    }

    fun useDemoLibrary() {
        repository = MediaRepository(FakeStorageProvider())
        _uiState.value = BrowseUiState(
            providerName = repository.providerName,
            isAuthenticated = true
        )
        openFolder("/")
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            repository = primaryRepositoryFactory()
            _uiState.value = BrowseUiState(
                providerName = repository.providerName,
                isAuthenticated = false,
                isLoading = false
            )
        }
    }

    fun openFolder(path: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, selectedItem = null, streamUrl = null)
            }

            runCatching {
                check(repository.isAuthenticated()) { "${repository.providerName} is not signed in" }
                repository.listFolder(path)
            }.onSuccess { media ->
                _uiState.update {
                    it.copy(
                        currentPath = path,
                        breadcrumbs = path.toBreadcrumbs(),
                        items = media,
                        isAuthenticated = true,
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

    private fun checkAuthentication() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val isAuthenticated = runCatching { repository.isAuthenticated() }.getOrDefault(false)
            _uiState.update {
                it.copy(isAuthenticated = isAuthenticated, isLoading = false)
            }

            if (isAuthenticated) {
                openFolder("/")
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val appContext = context.applicationContext
                val primaryRepositoryFactory = {
                    MediaRepository(
                        MegaProvider(
                            sessionStore = SharedPreferencesMegaSessionStore(appContext),
                            client = SdkMegaClient(appContext)
                        )
                    )
                }
                return BrowseViewModel(
                    repository = primaryRepositoryFactory(),
                    primaryRepositoryFactory = primaryRepositoryFactory
                ) as T
            }
        }
    }
}
