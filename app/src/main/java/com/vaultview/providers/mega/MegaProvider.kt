package com.vaultview.providers.mega

import com.vaultview.model.MediaItem
import com.vaultview.model.MediaMetadata
import com.vaultview.model.MediaType
import com.vaultview.providers.LoginCredentials
import com.vaultview.providers.StorageProvider

class MegaProvider(
    private val sessionStore: MegaSessionStore,
    private val client: MegaClient
) : StorageProvider {
    override val id = "mega"
    override val displayName = "MEGA"

    private var resumedSessionId: String? = null

    override suspend fun login(credentials: LoginCredentials) {
        val result = client.login(
            email = credentials.email,
            password = credentials.password,
            twoFactorCode = credentials.twoFactorCode
        )
        sessionStore.saveSessionId(result.sessionId)
        resumedSessionId = result.sessionId
    }

    override suspend fun logout() {
        runCatching { client.logout() }
        resumedSessionId = null
        sessionStore.clear()
    }

    override suspend fun isAuthenticated(): Boolean {
        val sessionId = sessionStore.loadSessionId() ?: return false
        if (resumedSessionId == sessionId) return true

        return runCatching {
            client.resumeSession(sessionId)
            resumedSessionId = sessionId
            true
        }.getOrElse {
            sessionStore.clear()
            resumedSessionId = null
            false
        }
    }

    override suspend fun listFolder(path: String): List<MediaItem> {
        requireAuthenticated()
        return client.listFolder(path).map { node ->
            MediaItem(
                id = node.handle,
                name = node.name,
                path = node.path,
                type = node.type,
                thumbnailUrl = node.thumbnailUrl,
                durationMillis = node.durationMillis,
                sizeBytes = node.sizeBytes
            )
        }
    }

    override suspend fun getThumbnail(item: MediaItem): String? {
        if (item.thumbnailUrl != null) return item.thumbnailUrl
        requireAuthenticated()
        return client.getThumbnailUrl(item.id)
    }

    override suspend fun getStreamUrl(item: MediaItem): String {
        require(item.type != MediaType.Folder) { "Folders do not have stream URLs" }
        requireAuthenticated()
        return client.getStreamUrl(item.id)
    }

    override suspend fun getMetadata(item: MediaItem): MediaMetadata {
        return MediaMetadata(
            title = item.name,
            type = item.type,
            durationMillis = item.durationMillis,
            sizeBytes = item.sizeBytes
        )
    }

    private suspend fun requireAuthenticated() {
        check(isAuthenticated()) { "MEGA session is not authenticated" }
    }
}
