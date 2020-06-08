/*
 * MIT License
 *
 * Copyright (c) 2020 Melms Media LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package gg.octave.bot.music.sources.spotify

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import gg.octave.bot.music.sources.spotify.loaders.SpotifyAlbumLoader
import gg.octave.bot.music.sources.spotify.loaders.SpotifyPlaylistLoader
import gg.octave.bot.music.sources.spotify.loaders.SpotifyTrackLoader
import org.apache.http.HttpStatus
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.DataInput
import java.io.DataOutput
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SpotifyAudioSourceManager(
    private val clientId: String?,
    private val clientSecret: String?,
    private val youtubeAudioSourceManager: YoutubeAudioSourceManager
) : AudioSourceManager {
    private val sched = Executors.newSingleThreadScheduledExecutor()
    private val trackLoaderPool = Executors.newFixedThreadPool(10)

    private val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()!!
    internal var accessToken: String = ""
        private set

    val enabled: Boolean
        get() = "" != clientId && "" != clientSecret

    init {
        refreshAccessToken()
    }


    /**
     * Source manager shizzle
     */
    override fun getSourceName() = "spotify"

    override fun isTrackEncodable(track: AudioTrack) = false

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        throw UnsupportedOperationException("${this::class.java.simpleName} does not support track decoding.")
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        throw UnsupportedOperationException("${this::class.java.simpleName} does not support track encoding.")
    }

    override fun shutdown() {
        httpInterfaceManager.close()
    }

    override fun loadItem(manager: DefaultAudioPlayerManager, reference: AudioReference): AudioItem? {
        if (accessToken.isEmpty()) {
            return null
        }

        return try {
            loadItemOnce(manager, reference.identifier)
        } catch (exception: FriendlyException) {
            // In case of a connection reset exception, try once more.
            if (HttpClientTools.isRetriableNetworkException(exception.cause)) {
                loadItemOnce(manager, reference.identifier)
            } else {
                throw exception
            }
        }
    }

    private fun loadItemOnce(manager: DefaultAudioPlayerManager, identifier: String): AudioItem? {
        for (loader in loaders) {
            val matcher = loader.pattern().matcher(identifier)

            if (matcher.find()) {
                return loader.load(manager, this, matcher)
            }
        }

        return null
    }

    internal fun doYoutubeSearch(manager: DefaultAudioPlayerManager, identifier: String): AudioItem? {
        val reference = AudioReference(identifier, null)
        return youtubeAudioSourceManager.loadItem(manager, reference)
    }

    internal fun queueYoutubeSearch(manager: DefaultAudioPlayerManager, identifier: String): CompletableFuture<AudioItem?> {
        val future = CompletableFuture<AudioItem?>()

        trackLoaderPool.submit {
            val reference = AudioReference(identifier, null)

            try {
                val result = youtubeAudioSourceManager.loadItem(manager, reference)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }


    /**
     * Spotify shizzle
     */
    fun refreshAccessToken() {
        if (!enabled) {
            return
        }

        val base64Auth = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())

        request(HttpPost.METHOD_NAME, "https://accounts.spotify.com/api/token") {
            addHeader("Authorization", "Basic $base64Auth")
            addHeader("Content-Type", "application/x-www-form-urlencoded")
            entity = StringEntity("grant_type=client_credentials")
        }.use {
            if (it.statusLine.statusCode != HttpStatus.SC_OK) {
                log.warn("Received code ${it.statusLine.statusCode} from Spotify while trying to update access token!")
                sched.schedule(::refreshAccessToken, 1, TimeUnit.MINUTES)
                return
            }

            val content = EntityUtils.toString(it.entity)
            val json = JSONObject(content)

            if (json.has("error")) {
                val errorMessage = json.getString("error")
                if (errorMessage.startsWith("invalid_")) {
                    log.error("Spotify API access disabled ($errorMessage)")
                    accessToken = ""
                    return
                }

                log.error("There was an error refreshing access token: $errorMessage. Queueing refresh for a minute from now.")
                sched.schedule(::refreshAccessToken, 1, TimeUnit.MINUTES)
                return
            }

            val refreshSeconds = json.getInt("expires_in").toLong()
            accessToken = json.getString("access_token")

            val refreshMillis = TimeUnit.SECONDS.toMillis(refreshSeconds)
            // Refresh 5 minutes early. This means should there be an issue with Spotify, we can refresh 5 times (1/min)
            // before the token actually expires.
            val earlyRefresh = TimeUnit.MINUTES.toMillis(5)
            val refreshDelay = refreshMillis - earlyRefresh
            sched.schedule(::refreshAccessToken, refreshDelay, TimeUnit.MILLISECONDS)

            log.info("Successfully refreshed Spotify access token.")
        }
    }


    /**
     * Utils boiiii
     */
    internal fun request(url: String, requestBuilder: RequestBuilder.() -> Unit): CloseableHttpResponse {
        return request(HttpGet.METHOD_NAME, url, requestBuilder)
    }

    private fun request(method: String, url: String, requestBuilder: RequestBuilder.() -> Unit): CloseableHttpResponse {
        return httpInterfaceManager.`interface`.use {
            it.execute(RequestBuilder.create(method).setUri(url).apply(requestBuilder).build())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SpotifyAudioSourceManager::class.java)

        private val loaders = listOf(
            SpotifyAlbumLoader(),
            SpotifyPlaylistLoader(),
            SpotifyTrackLoader()
        )
    }
}
