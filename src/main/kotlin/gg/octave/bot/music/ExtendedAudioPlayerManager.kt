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

package gg.octave.bot.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block
import gg.octave.bot.Launcher
import gg.octave.bot.music.sources.attachment.DiscordAttachmentAudioSourceManager
import gg.octave.bot.music.sources.caching.CachingSourceManager
import gg.octave.bot.music.sources.spotify.SpotifyAudioSourceManager
import gg.octave.bot.music.utils.TrackContext
import io.sentry.Sentry
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.util.*

class ExtendedAudioPlayerManager(private val dapm: AudioPlayerManager = DefaultAudioPlayerManager()) : AudioPlayerManager by dapm {
    init {
        dapm.configuration.apply {
            isFilterHotSwapEnabled = true
            setFrameBufferFactory(::NonAllocatingAudioFrameBuffer)
        }

        val youtubeAudioSourceManager = YoutubeAudioSourceManager(true)
        val config = Launcher.configuration
        val credentials = Launcher.credentials

        if (config.ipv6Block.isNotEmpty()) {
            val block = config.ipv6Block
            val blocks = listOf(Ipv6Block(block))
            val planner = when {
                config.ipv6Exclude.isEmpty() -> RotatingNanoIpRoutePlanner(blocks)
                else -> try {
                    val blacklistedGW = InetAddress.getByName(config.ipv6Exclude)
                    RotatingNanoIpRoutePlanner(blocks) { it != blacklistedGW }
                } catch (ex: Exception) {
                    Sentry.capture(ex)
                    log.error("Error setting up IPv6 exclude GW, falling back to registering the whole block", ex)
                    RotatingNanoIpRoutePlanner(blocks)
                }
            }

            YoutubeIpRotatorSetup(planner)
                .forSource(youtubeAudioSourceManager)
                .setup()
        }

        val spotifyAudioSourceManager = SpotifyAudioSourceManager(
            credentials.spotifyClientId,
            credentials.spotifyClientSecret,
            youtubeAudioSourceManager
        )

        registerSourceManagers(
            CachingSourceManager(),
            DiscordAttachmentAudioSourceManager(),
            spotifyAudioSourceManager,
            youtubeAudioSourceManager,
            SoundCloudAudioSourceManager.createDefault(),
            GetyarnAudioSourceManager(),
            BandcampAudioSourceManager(),
            VimeoAudioSourceManager(),
            TwitchStreamAudioSourceManager(),
            BeamAudioSourceManager()
        )
    }

    private fun registerSourceManagers(vararg sourceManagers: AudioSourceManager) {
        for (sm in sourceManagers) {
            registerSourceManager(sm)
        }
    }

    /**
     * @return a base64 encoded string containing the track data.
     */
    fun encodeTrack(track: AudioTrack): String {
        val baos = ByteArrayOutputStream()
        dapm.encodeTrack(MessageOutput(baos), track)

        track.userData?.takeIf { it is TrackContext }?.let {
            (it as TrackContext).serialize(baos) // Write our user data to the stream.
        }

        val encoded = Base64.getEncoder().encodeToString(baos.toByteArray())
        baos.close()
        return encoded
    }

    /**
     * @return An AudioTrack with possibly-null user data.
     */
    fun decodeTrack(base64: String): AudioTrack? {
        val decoded = Base64.getDecoder().decode(base64)
        val bais = ByteArrayInputStream(decoded)
        val track = dapm.decodeTrack(MessageInput(bais))

        val audioTrack = track?.decodedTrack
            ?: return null

        val trackContext = TrackContext.deserialize(bais)

        if (trackContext != null) {
            audioTrack.userData = trackContext
        }

        return audioTrack
    }

    fun decodePlaylist(encodedTracks: List<String>, name: String): BasicAudioPlaylist {
        val decoded = encodedTracks.mapNotNull(::decodeMaybeNullAudioTrack)
        return BasicAudioPlaylist(name, decoded, decoded[0], false)
    }

    fun toJsonString(playlist: AudioPlaylist): String {
        val selectedIndex = playlist.selectedTrack?.let(playlist.tracks::indexOf) ?: -1
        val tracks = JSONArray()

        for (track in playlist.tracks) {
            val enc = encodeAudioTrack(track)
            tracks.put(enc)
        }

        return JSONObject().apply {
            put("name", playlist.name)
            put("tracks", tracks)
            put("search", playlist.isSearchResult)
            put("selected", selectedIndex)
        }.toString()
    }

    fun decodePlaylist(jsonString: String): BasicAudioPlaylist {
        val jo = JSONObject(jsonString)

        val name = jo.getString("name")
        val isSearch = jo.getBoolean("search")
        val selectedIndex = jo.getInt("selected")

        val encodedTracks = jo.getJSONArray("tracks")
        val tracks = mutableListOf<AudioTrack>()

        for (encodedTrack in encodedTracks) {
            val decodedTrack = decodeAudioTrack(encodedTrack as String)
            tracks.add(decodedTrack)
        }

        val selectedTrack = if (selectedIndex > -1) tracks[selectedIndex] else null
        return BasicAudioPlaylist(name, tracks, selectedTrack, isSearch)
    }

    fun encodePlaylist(playlist: BasicAudioPlaylist) = playlist.tracks.map(::encodeAudioTrack)
    fun encodeAudioTrack(track: AudioTrack) = encodeTrack(track)

    // This is used at the top of the file. Don't ask :^)
    fun decodeMaybeNullAudioTrack(encoded: String) = decodeTrack(encoded)
    fun decodeAudioTrack(encoded: String) = decodeTrack(encoded)!!

    companion object {
        private val log = LoggerFactory.getLogger(ExtendedAudioPlayerManager::class.java)
    }
}
