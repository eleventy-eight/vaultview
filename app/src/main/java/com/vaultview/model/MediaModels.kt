package com.vaultview.model

enum class MediaType {
    Folder,
    Image,
    Video
}

data class MediaItem(
    val id: String,
    val name: String,
    val path: String,
    val type: MediaType,
    val thumbnailUrl: String? = null,
    val durationMillis: Long? = null,
    val sizeBytes: Long? = null
)

data class MediaMetadata(
    val title: String,
    val type: MediaType,
    val width: Int? = null,
    val height: Int? = null,
    val durationMillis: Long? = null,
    val sizeBytes: Long? = null
)
