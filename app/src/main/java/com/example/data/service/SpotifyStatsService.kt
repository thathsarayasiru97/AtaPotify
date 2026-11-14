package com.example.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SpotifyMonthlySnapshot(
    val spotifyDisplayName: String,
    val minutesListened: Long,
    val tracksCounted: Long,
    val topArtist: String,
    val topTrack: String,
)

class SpotifyStatsService(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isClientIdConfigured: Boolean
        get() {
            val id = BuildConfig.SPOTIFY_CLIENT_ID
            return id.isNotBlank() && id != "MY_SPOTIFY_CLIENT_ID"
        }

    val isConnected: Boolean
        get() = !prefs.getString(KEY_REFRESH_TOKEN, null).isNullOrBlank() ||
            !prefs.getString(KEY_ACCESS_TOKEN, null).isNullOrBlank()

    val connectedSpotifyName: String
        get() = prefs.getString(KEY_SPOTIFY_NAME, null).orEmpty()

    fun buildAuthorizationIntent(): Result<Intent> = runCatching {
        check(isClientIdConfigured) {
            "SPOTIFY_CLIENT_ID is not configured in Secrets yet."
        }
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)
        val state = generateCodeVerifier().take(24)

        prefs.edit()
            .putString(KEY_CODE_VERIFIER, verifier)
            .putString(KEY_OAUTH_STATE, state)
            .apply()

        val uri = Uri.Builder()
            .scheme("https")
            .authority("accounts.spotify.com")
            .path("authorize")
            .appendQueryParameter("client_id", BuildConfig.SPOTIFY_CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("state", state)
            .appendQueryParameter("show_dialog", "true")
            .build()

        Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    suspend fun handleRedirectUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(isClientIdConfigured) { "SPOTIFY_CLIENT_ID is not configured in Secrets." }
            val error = uri.getQueryParameter("error")
            if (!error.isNullOrBlank()) {
                error("Spotify login cancelled or denied: $error")
            }
            val code = uri.getQueryParameter("code") ?: error("Missing Spotify authorization code")
            val state = uri.getQueryParameter("state")
            val expectedState = prefs.getString(KEY_OAUTH_STATE, null)
            if (expectedState != null && state != expectedState) {
                error("Spotify OAuth state mismatch. Please try connecting again.")
            }
            val verifier = prefs.getString(KEY_CODE_VERIFIER, null)
                ?: error("Missing PKCE verifier. Please restart Spotify login.")

            val form = buildFormBody(
                mapOf(
                    "client_id" to BuildConfig.SPOTIFY_CLIENT_ID,
                    "grant_type" to "authorization_code",
                    "code" to code,
                    "redirect_uri" to REDIRECT_URI,
                    "code_verifier" to verifier,
                )
            )
            val tokenJson = postForm("https://accounts.spotify.com/api/token", form)
            saveTokenResponse(tokenJson)
            prefs.edit().remove(KEY_CODE_VERIFIER).remove(KEY_OAUTH_STATE).apply()
        }
    }

    suspend fun fetchAndAccumulateMonthlyStats(monthId: String): Result<SpotifyMonthlySnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = ensureValidAccessToken()
                val meJson = getJson("https://api.spotify.com/v1/me", token)
                val spotifyName = meJson.optString("display_name").ifBlank { "Spotify User" }
                prefs.edit().putString(KEY_SPOTIFY_NAME, spotifyName).apply()

                val recentJson = getJson(
                    "https://api.spotify.com/v1/me/player/recently-played?limit=50",
                    token
                )
                val items = recentJson.optJSONArray("items")

                val seenKey = "seen_plays_$monthId"
                val minutesKey = "minutes_ms_$monthId"
                val tracksKey = "tracks_count_$monthId"

                val existingSeen = prefs.getStringSet(seenKey, emptySet())?.toMutableSet() ?: mutableSetOf()
                var totalDurationMs = prefs.getLong(minutesKey, 0L)
                var totalTracks = prefs.getLong(tracksKey, 0L)

                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val playedAt = item.optString("played_at")
                        if (!isPlayedAtInMonth(playedAt, monthId)) continue
                        if (existingSeen.add(playedAt)) {
                            val track = item.optJSONObject("track")
                            val durationMs = track?.optLong("duration_ms", 0L) ?: 0L
                            totalDurationMs += durationMs.coerceAtLeast(0L)
                            totalTracks += 1L
                        }
                    }
                }

                prefs.edit()
                    .putStringSet(seenKey, existingSeen)
                    .putLong(minutesKey, totalDurationMs)
                    .putLong(tracksKey, totalTracks)
                    .apply()

                val topArtistsJson = runCatching {
                    getJson("https://api.spotify.com/v1/me/top/artists?time_range=short_term&limit=3", token)
                }.getOrNull()
                val topArtist = topArtistsJson
                    ?.optJSONArray("items")
                    ?.optJSONObject(0)
                    ?.optString("name")
                    .orEmpty()

                val topTracksJson = runCatching {
                    getJson("https://api.spotify.com/v1/me/top/tracks?time_range=short_term&limit=3", token)
                }.getOrNull()
                val firstTrackObj = topTracksJson?.optJSONArray("items")?.optJSONObject(0)
                val topTrack = if (firstTrackObj != null) {
                    val trackName = firstTrackObj.optString("name")
                    val artistName = firstTrackObj.optJSONArray("artists")?.optJSONObject(0)?.optString("name").orEmpty()
                    if (artistName.isNotBlank()) "$trackName — $artistName" else trackName
                } else {
                    ""
                }

                SpotifyMonthlySnapshot(
                    spotifyDisplayName = spotifyName,
                    minutesListened = (totalDurationMs / 60_000L).coerceAtLeast(0L),
                    tracksCounted = totalTracks,
                    topArtist = topArtist,
                    topTrack = topTrack,
                )
            }
        }

    fun disconnect() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_SPOTIFY_NAME)
            .apply()
    }

    private fun ensureValidAccessToken(): String {
        val currentToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (!currentToken.isNullOrBlank() && System.currentTimeMillis() < expiresAt - 30_000L) {
            return currentToken
        }
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            ?: error("Spotify session expired. Please reconnect Spotify.")
        val form = buildFormBody(
            mapOf(
                "client_id" to BuildConfig.SPOTIFY_CLIENT_ID,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
            )
        )
        val json = postForm("https://accounts.spotify.com/api/token", form)
        saveTokenResponse(json)
        return checkNotNull(prefs.getString(KEY_ACCESS_TOKEN, null)) { "Missing refreshed Spotify token" }
    }

    private fun saveTokenResponse(json: JSONObject) {
        val access = json.getString("access_token")
        val expiresInSec = json.optLong("expires_in", 3600L)
        val refresh = json.optString("refresh_token")
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, access)
            putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresInSec * 1000L)
            if (refresh.isNotBlank()) {
                putString(KEY_REFRESH_TOKEN, refresh)
            }
        }.apply()
    }

    private fun getJson(urlStr: String, accessToken: String): JSONObject {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        return readJsonResponse(conn)
    }

    private fun postForm(urlStr: String, formBody: String): JSONObject {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        conn.outputStream.use { it.write(formBody.toByteArray(Charsets.UTF_8)) }
        return readJsonResponse(conn)
    }

    private fun readJsonResponse(conn: HttpURLConnection): JSONObject {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.use { s ->
            BufferedReader(InputStreamReader(s)).readText()
        }.orEmpty()
        conn.disconnect()
        if (code !in 200..299) {
            val msg = runCatching {
                JSONObject(text).optJSONObject("error")?.optString("message")
                    ?: JSONObject(text).optString("error_description")
            }.getOrNull().orEmpty().ifBlank { "HTTP $code" }
            error("Spotify API error: $msg")
        }
        return JSONObject(text)
    }

    private fun buildFormBody(params: Map<String, String>): String =
        params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun isPlayedAtInMonth(playedAtIso: String, targetMonthId: String): Boolean {
        if (playedAtIso.length >= 7 && playedAtIso.startsWith(targetMonthId)) return true
        return runCatching {
            val clean = playedAtIso.replace(Regex("\\.\\d+Z$"), "Z")
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(clean) ?: return false
            val localMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(date)
            localMonth == targetMonthId
        }.getOrDefault(false)
    }

    private companion object {
        const val PREFS_NAME = "atapotify_spotify_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_CODE_VERIFIER = "code_verifier"
        const val KEY_OAUTH_STATE = "oauth_state"
        const val KEY_SPOTIFY_NAME = "spotify_name"

        const val REDIRECT_URI = "atapotify://spotify-callback"
        const val SCOPES = "user-read-recently-played user-top-read user-read-private"
    }
}
