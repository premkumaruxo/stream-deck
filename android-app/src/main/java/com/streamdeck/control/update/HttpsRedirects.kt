package com.streamdeck.control.update

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal object HttpsRedirects {
    private val redirectCodes = setOf(301, 302, 303, 307, 308)

    fun execute(
        client: OkHttpClient,
        initialRequest: Request,
        allowedHosts: Set<String>,
    ): Response {
        var request = initialRequest
        var redirectCount = 0
        while (true) {
            require(
                request.url.isHttps &&
                    request.url.username.isEmpty() &&
                    request.url.password.isEmpty() &&
                    request.url.host in allowedHosts
            ) {
                "Update requests must remain on an approved HTTPS host"
            }

            val response = client.newCall(request).execute()
            if (response.code !in redirectCodes) return response

            val location = response.header("Location")
            if (location == null) return response
            val nextUrl = response.request.url.resolve(location)
            response.close()
            require(
                nextUrl != null &&
                    nextUrl.isHttps &&
                    nextUrl.username.isEmpty() &&
                    nextUrl.password.isEmpty() &&
                    nextUrl.host in allowedHosts
            ) {
                "Update server redirected to an unapproved URL"
            }
            check(redirectCount++ < MAX_REDIRECTS) { "Too many update server redirects" }
            request = request.newBuilder().url(nextUrl).build()
        }
    }

    private const val MAX_REDIRECTS = 5
}
