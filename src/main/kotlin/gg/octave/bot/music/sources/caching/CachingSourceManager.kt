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

package gg.octave.bot.music.sources.caching

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.*
import gg.octave.bot.Launcher
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.Protocol
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.params.SetParams
import java.io.DataInput
import java.io.DataOutput
import java.util.concurrent.TimeUnit

class CachingSourceManager : AudioSourceManager {
    init {
        try {
            jedisPool.resource.use {
                log.info("AudioTrack and AudioPlaylist caching is available (Connected to Redis).")
            }
        } catch (e: JedisConnectionException) {
            log.warn("Caching is disabled for this session (Unable to connect to Redis).")
            jedisPool.close()
            enabled = false
        }
    }

    override fun getSourceName() = "caching"

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        throw UnsupportedOperationException("This source manager does not support the decoding of tracks.")
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        throw UnsupportedOperationException("This source manager does not support the encoding of tracks.")
    }

    override fun isTrackEncodable(track: AudioTrack) = false

    override fun loadItem(manager: DefaultAudioPlayerManager, reference: AudioReference): AudioItem? {
        if (jedisPool.isClosed) {
            return null
        }

        totalHits++

        jedisPool.resource.use {
            val encoded = it.get(reference.identifier)
                ?: return null

            successfulHits++

            if (encoded.startsWith('{')) { // JSON object representing a playlist.
                return Launcher.players.playerManager.decodePlaylist(encoded)
            }

            return Launcher.players.playerManager.decodeAudioTrack(encoded)
        }
    }

    override fun shutdown() {

    }

    companion object {
        private val log = LoggerFactory.getLogger(CachingSourceManager::class.java)

        var enabled = true
            private set

        // Metrics
        var totalHits = 0
            private set
        var successfulHits = 0
            private set

        private val creds = Launcher.credentials
        private val redisHost = creds.redisHost
        private val redisPort = creds.redisPort
        private val redisAuth = creds.redisAuth
        private val jedisPool = JedisPool(JedisPoolConfig(), redisHost, redisPort, Protocol.DEFAULT_TIMEOUT, redisAuth)

        private val PLAYLIST_TTL = TimeUnit.HOURS.toMillis(2)
        private val SEARCH_TTL = TimeUnit.HOURS.toMillis(12)
        private val TRACK_TTL = TimeUnit.HOURS.toMillis(12)

        fun cache(identifier: String, item: AudioItem) {
            if (jedisPool.isClosed) {
                return
            }

            if (item is AudioTrack) {
                jedisPool.resource.use {
                    val encoded = Launcher.players.playerManager.encodeAudioTrack(item)
                    val setParams = SetParams.setParams().nx().px(TRACK_TTL)
                    it.set(identifier, encoded, setParams)
                }
            } else if (item is AudioPlaylist) {
                jedisPool.resource.use {
                    val ttl = if (item.isSearchResult) SEARCH_TTL else PLAYLIST_TTL
                    val encoded = Launcher.players.playerManager.toJsonString(item)
                    val setParams = SetParams.setParams().nx().px(ttl)
                    it.set(identifier, encoded, setParams)
                }
            }
        }
    }
}
