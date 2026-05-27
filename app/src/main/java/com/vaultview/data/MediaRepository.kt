package com.vaultview.data

import com.vaultview.model.MediaItem
import com.vaultview.providers.LoginCredentials
import com.vaultview.providers.StorageProvider

class MediaRepository(
    val provider: StorageProvider
) {
    val providerName: String = provider.displayName

    suspend fun isAuthenticated(): Boolean = provider.isAuthenticated()

    suspend fun login(credentials: LoginCredentials) = provider.login(credentials)

    suspend fun logout() = provider.logout()

    suspend fun listFolder(path: String): List<MediaItem> = provider.listFolder(path)

    suspend fun streamUrl(item: MediaItem): String = provider.getStreamUrl(item)
}
