package com.vaultview.providers.mega

class MissingMegaClient : MegaClient {
    override suspend fun login(email: String, password: String, twoFactorCode: String?): MegaLoginResult {
        throw MegaSdkUnavailableException()
    }

    override suspend fun resumeSession(sessionId: String) {
        throw MegaSdkUnavailableException()
    }

    override suspend fun logout() = Unit

    override suspend fun listFolder(path: String): List<MegaNode> {
        throw MegaSdkUnavailableException()
    }

    override suspend fun getThumbnailUrl(handle: String): String? {
        throw MegaSdkUnavailableException()
    }

    override suspend fun getStreamUrl(handle: String): String {
        throw MegaSdkUnavailableException()
    }
}

class MegaSdkUnavailableException : IllegalStateException(
    "MEGA sign-in is not connected yet. The screen is ready, including 2FA, but the official MEGA SDK adapter still needs to be wired."
)
