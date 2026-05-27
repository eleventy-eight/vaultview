package com.vaultview.providers.mega

import com.vaultview.model.MediaType

data class MegaLoginResult(
    val sessionId: String
)

data class MegaNode(
    val handle: String,
    val name: String,
    val path: String,
    val type: MediaType,
    val thumbnailUrl: String? = null,
    val durationMillis: Long? = null,
    val sizeBytes: Long? = null
)

interface MegaClient {
    suspend fun login(email: String, password: String, twoFactorCode: String?): MegaLoginResult
    suspend fun resumeSession(sessionId: String)
    suspend fun logout()
    suspend fun listFolder(path: String): List<MegaNode>
    suspend fun getThumbnailUrl(handle: String): String?
    suspend fun getStreamUrl(handle: String): String
}
