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

package gg.octave.bot.music.sources.attachment

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor
import com.sedmelluq.discord.lavaplayer.tools.Units
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import java.net.URI

class DiscordAttachmentAudioTrack(
    discordTrackInfo: AudioTrackInfo,
    internal val containerTrackFactory: MediaContainerDescriptor,
    private val sourceManager: DiscordAttachmentAudioSourceManager
) : DelegatedAudioTrack(discordTrackInfo) {
    override fun getSourceManager() = sourceManager
    override fun makeShallowClone() = DiscordAttachmentAudioTrack(trackInfo, containerTrackFactory, sourceManager)

    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.httpInterface.use {
            PersistentHttpStream(it, URI(trackInfo.identifier), Units.CONTENT_LENGTH_UNKNOWN).use { inputStream ->
                processDelegate(containerTrackFactory.createTrack(trackInfo, inputStream) as InternalAudioTrack, executor)
            }
        }
    }
}
