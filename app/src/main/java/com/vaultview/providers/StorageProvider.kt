package com.vaultview.providers

import com.vaultview.model.MediaItem
import com.vaultview.model.MediaMetadata

interface StorageProvider {
    val id: String
    val displayName: String

    suspend fun login()
    suspend fun isAuthenticated(): Boolean
    suspend fun listFolder(path: String): List<MediaItem>
    suspend fun getThumbnail(item: MediaItem): String?
    suspend fun getStreamUrl(item: MediaItem): String
    suspend fun getMetadata(item: MediaItem): MediaMetadata
}
