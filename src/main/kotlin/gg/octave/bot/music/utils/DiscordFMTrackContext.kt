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

package gg.octave.bot.music.utils

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import gg.octave.bot.Launcher
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.concurrent.CompletableFuture

class DiscordFMTrackContext(
    val station: String,
    requester: Long,
    requestedChannel: Long
) : TrackContext(requester, requestedChannel) {
    override fun serialize(stream: ByteArrayOutputStream) {
        val writer = DataOutputStream(stream)
        writer.writeInt(2)
        // 1 => TrackContext
        // 2 => DiscordFMTrackContext
        // 3 => RadioTrackContext
        writer.writeLong(requester)
        writer.writeLong(requestedChannel)
        writer.writeUTF(station)
        writer.close() // This invokes flush.
    }

    fun nextDiscordFMTrack(errorDepth: Int = 0): CompletableFuture<AudioTrack?> {
        if (errorDepth > errorTolerance) {
            return CompletableFuture.completedFuture(null)
        }

        val randomSong = Launcher.discordFm.getRandomSong(station)
            ?: return nextDiscordFMTrack(errorDepth + 1)

        val future = CompletableFuture<AudioTrack?>()

        Launcher.players.playerManager.loadItemOrdered(this, randomSong, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                track.userData = this@DiscordFMTrackContext
                future.complete(track)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) = trackLoaded(playlist.tracks.first())

            override fun noMatches() {
                future.complete(null)
            }

            override fun loadFailed(exception: FriendlyException) {
                if (errorDepth >= errorTolerance) {
                    future.complete(null)
                } else {
                    nextDiscordFMTrack(errorDepth + 1)
                }
            }
        })

        return future
    }

    companion object {
        @JvmStatic
        val errorTolerance = 3
    }
}
