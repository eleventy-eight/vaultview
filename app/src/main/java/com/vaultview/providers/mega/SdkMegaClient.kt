package com.vaultview.providers.mega

import android.content.Context
import com.vaultview.model.MediaType
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaNode as SdkMegaNode

class SdkMegaClient(context: Context) : MegaClient {
    private val appContext = context.applicationContext
    private val nodesByHandle = mutableMapOf<String, SdkMegaNode>()

    private val api: MegaApiAndroid by lazy {
        MegaApiAndroid(APP_KEY, USER_AGENT, "${appContext.filesDir.absolutePath}/mega/")
    }

    override suspend fun login(
        email: String,
        password: String,
        twoFactorCode: String?
    ): MegaLoginResult {
        val pin = twoFactorCode?.takeIf { it.isNotBlank() }

        awaitRequest(MegaRequest.TYPE_LOGIN) { listener ->
            if (pin == null) {
                api.login(email, password, listener)
            } else {
                api.multiFactorAuthLogin(email, password, pin, listener)
            }
        }

        fetchNodes()
        return MegaLoginResult(sessionId = api.dumpSession() ?: error("MEGA did not return a session"))
    }

    override suspend fun resumeSession(sessionId: String) {
        awaitRequest(MegaRequest.TYPE_LOGIN) { listener ->
            api.fastLogin(sessionId, listener)
        }
        fetchNodes()
    }

    override suspend fun logout() {
        runCatching {
            awaitRequest(MegaRequest.TYPE_LOGOUT) { listener ->
                api.logout(listener)
            }
        }
        nodesByHandle.clear()
    }

    override suspend fun listFolder(path: String): List<MegaNode> {
        val parent = resolveFolder(path)
        return api.getChildren(parent, MegaApiJava.ORDER_DEFAULT_ASC)
            .orEmpty()
            .filter { it.type == SdkMegaNode.TYPE_FOLDER || it.isSupportedMedia }
            .map { node ->
                nodesByHandle[node.handle.toString()] = node
                MegaNode(
                    handle = node.handle.toString(),
                    name = node.name.orEmpty(),
                    path = node.handle.toString(),
                    type = node.toMediaType(),
                    durationMillis = node.duration.takeIf { it > 0 }?.times(1000L),
                    sizeBytes = node.size.takeIf { it > 0 }
                )
            }
    }

    override suspend fun getThumbnailUrl(handle: String): String? = null

    override suspend fun getStreamUrl(handle: String): String {
        val node = nodesByHandle[handle] ?: error("MEGA node is not loaded")
        if (api.httpServerIsRunning() == 0 && !api.httpServerStart(true)) {
            error("Unable to start MEGA local streaming server")
        }
        return api.httpServerGetLocalLink(node) ?: error("Unable to create MEGA stream URL")
    }

    private suspend fun fetchNodes() {
        awaitRequest(MegaRequest.TYPE_FETCH_NODES) { listener ->
            api.fetchNodes(listener)
        }
        nodesByHandle.clear()
        api.rootNode?.let { nodesByHandle[it.handle.toString()] = it }
    }

    private fun resolveFolder(path: String): SdkMegaNode {
        if (path == "/") {
            return api.rootNode ?: error("MEGA root folder is not loaded")
        }

        nodesByHandle[path]?.let { return it }

        return api.getNodeByPath(path) ?: error("MEGA folder is not loaded")
    }

    private suspend fun awaitRequest(
        expectedType: Int,
        start: (MegaRequestListenerInterface) -> Unit
    ) {
        suspendCancellableCoroutine { continuation ->
            val listener = object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava, request: MegaRequest) = Unit
                override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) = Unit

                override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                    if (request.type != expectedType || !continuation.isActive) return

                    if (e.errorCode == MegaError.API_OK || e.errorCode == MegaError.API_ESID) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWith(Result.failure(e.toException()))
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaApiJava,
                    request: MegaRequest,
                    e: MegaError
                ) = Unit
            }

            start(listener)
        }
    }

    private fun MegaError.toException(): Throwable {
        val message = when (errorCode) {
            MegaError.API_EMFAREQUIRED -> "MEGA requires a 2FA code."
            MegaError.API_ENOENT -> "Incorrect MEGA email, password, or 2FA code."
            MegaError.API_EBLOCKED -> "This MEGA account is blocked."
            MegaError.API_EAPPKEY -> "The MEGA application key is invalid."
            else -> errorString ?: "MEGA request failed with code $errorCode"
        }
        return IllegalStateException(message)
    }

    private val SdkMegaNode.isSupportedMedia: Boolean
        get() = name.orEmpty().substringAfterLast('.', "").lowercase() in SUPPORTED_EXTENSIONS

    private fun SdkMegaNode.toMediaType(): MediaType {
        if (type == SdkMegaNode.TYPE_FOLDER) return MediaType.Folder
        val extension = name.orEmpty().substringAfterLast('.', "").lowercase()
        return if (extension in VIDEO_EXTENSIONS) MediaType.Video else MediaType.Image
    }

    private companion object {
        const val APP_KEY = "l4cmkI7B"
        const val USER_AGENT = "VaultView Android TV"

        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
        val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "mkv", "mov", "avi", "webm", "3gp", "ts")
        val SUPPORTED_EXTENSIONS = IMAGE_EXTENSIONS + VIDEO_EXTENSIONS
    }
}
