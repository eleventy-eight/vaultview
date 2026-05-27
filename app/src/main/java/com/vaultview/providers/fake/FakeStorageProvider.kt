package com.vaultview.providers.fake

import com.vaultview.model.MediaItem
import com.vaultview.model.MediaMetadata
import com.vaultview.model.MediaType
import com.vaultview.providers.LoginCredentials
import com.vaultview.providers.StorageProvider
import kotlinx.coroutines.delay

class FakeStorageProvider : StorageProvider {
    override val id = "fake"
    override val displayName = "Demo Library"

    private val items = mapOf(
        "/" to listOf(
            MediaItem("photos", "Photos", "/photos", MediaType.Folder),
            MediaItem("videos", "Videos", "/videos", MediaType.Folder),
            MediaItem("summer", "Summer Archive", "/summer", MediaType.Folder),
            image("img-1", "Morning light", "/photos/morning.jpg", "https://picsum.photos/id/1018/900/600"),
            video("vid-1", "Coastal walk", "/videos/coast.mp4", "https://picsum.photos/id/1011/900/600")
        ),
        "/photos" to listOf(
            image("img-2", "Cabin window", "/photos/cabin.jpg", "https://picsum.photos/id/1025/900/600"),
            image("img-3", "Still water", "/photos/water.jpg", "https://picsum.photos/id/1039/900/600"),
            image("img-4", "Late afternoon", "/photos/afternoon.jpg", "https://picsum.photos/id/1043/900/600")
        ),
        "/videos" to listOf(
            video("vid-2", "Mountain pass", "/videos/mountain.mp4", "https://picsum.photos/id/1015/900/600"),
            video("vid-3", "City evening", "/videos/city.mp4", "https://picsum.photos/id/1016/900/600")
        ),
        "/summer" to listOf(
            image("img-5", "Boardwalk", "/summer/boardwalk.jpg", "https://picsum.photos/id/1050/900/600"),
            video("vid-4", "Harbour", "/summer/harbour.mp4", "https://picsum.photos/id/1067/900/600")
        )
    )

    override suspend fun login(credentials: LoginCredentials) {
        delay(250)
    }

    override suspend fun logout() = Unit

    override suspend fun isAuthenticated(): Boolean = true

    override suspend fun listFolder(path: String): List<MediaItem> {
        delay(200)
        return items[path].orEmpty()
    }

    override suspend fun getThumbnail(item: MediaItem): String? = item.thumbnailUrl

    override suspend fun getStreamUrl(item: MediaItem): String {
        return when (item.type) {
            MediaType.Image -> item.thumbnailUrl.orEmpty()
            MediaType.Video -> "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            MediaType.Folder -> error("Folders do not have stream URLs")
        }
    }

    override suspend fun getMetadata(item: MediaItem): MediaMetadata {
        return MediaMetadata(
            title = item.name,
            type = item.type,
            durationMillis = item.durationMillis,
            sizeBytes = item.sizeBytes
        )
    }

    private fun image(id: String, name: String, path: String, thumbnail: String) =
        MediaItem(id, name, path, MediaType.Image, thumbnailUrl = thumbnail)

    private fun video(id: String, name: String, path: String, thumbnail: String) =
        MediaItem(id, name, path, MediaType.Video, thumbnailUrl = thumbnail, durationMillis = 734_000)
}
