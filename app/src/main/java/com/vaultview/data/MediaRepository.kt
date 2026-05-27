package com.vaultview.data

import com.vaultview.model.MediaItem
import com.vaultview.providers.StorageProvider

class MediaRepository(
    private val provider: StorageProvider
) {
    val providerName: String = provider.displayName

    suspend fun ensureAuthenticated() {
        if (!provider.isAuthenticated()) {
            provider.login()
        }
    }

    suspend fun listFolder(path: String): List<MediaItem> = provider.listFolder(path)

    suspend fun streamUrl(item: MediaItem): String = provider.getStreamUrl(item)
}
