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
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.ProbingAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.Units
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.NoRedirectsStrategy
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.tools.io.ThreadLocalHttpInterfaceManager
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder
import gg.octave.bot.music.utils.LimitedContainerRegistry
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.net.URI

class DiscordAttachmentAudioSourceManager : ProbingAudioSourceManager(LimitedContainerRegistry()) {
    private val httpInterfaceManager = ThreadLocalHttpInterfaceManager(
        HttpClientTools
            .createSharedCookiesHttpBuilder()
            .setRedirectStrategy(NoRedirectsStrategy()),
        HttpClientTools.DEFAULT_REQUEST_CONFIG
    )
    internal val httpInterface: HttpInterface
        get() = httpInterfaceManager.`interface`

    override fun getSourceName() = "attachment"

    override fun loadItem(manager: DefaultAudioPlayerManager, reference: AudioReference): AudioItem? {
        if (!(reference.identifier matches cdnRegex)) {
            return null
        }

        return if (reference.containerDescriptor != null) {
            createTrack(AudioTrackInfoBuilder.create(reference, null).build(), reference.containerDescriptor)
        } else {
            handleLoadResult(detectContainer(reference))
        }
    }

    private fun detectContainer(reference: AudioReference): MediaContainerDetectionResult? {
        return try {
            httpInterface.use { detectContainerWithClient(httpInterface, reference) }
        } catch (e: IOException) {
            throw FriendlyException("Connecting to the URL failed.", FriendlyException.Severity.SUSPICIOUS, e)
        }
    }

    @Throws(IOException::class)
    private fun detectContainerWithClient(httpInterface: HttpInterface, reference: AudioReference): MediaContainerDetectionResult? {
        val uri = inlineTry("Not a valid URL.") { URI(reference.identifier) }

        // We could probably scrape content-length from headers.
        PersistentHttpStream(httpInterface, uri, Units.CONTENT_LENGTH_UNKNOWN).use { inputStream ->
            val statusCode = inputStream.checkStatusCode()
            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return null
            } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw FriendlyException("That URL is not playable.", FriendlyException.Severity.COMMON, IllegalStateException("Status code $statusCode"))
            }
            val hints = MediaContainerHints.from(getHeaderValue(inputStream.currentResponse, "Content-Type"), null)
            return MediaContainerDetection(containerRegistry, reference, inputStream, hints).detectContainer()
        }
    }

    private fun getHeaderValue(response: HttpResponse, name: String) = response.getFirstHeader(name)?.value

    override fun isTrackEncodable(track: AudioTrack) = false

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack? {
        val containerTrackFactory = decodeTrackFactory(input)
            ?: return null

        return DiscordAttachmentAudioTrack(trackInfo, containerTrackFactory, this)
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        encodeTrackFactory((track as DiscordAttachmentAudioTrack).containerTrackFactory, output)
    }

    override fun createTrack(trackInfo: AudioTrackInfo, containerTrackFactory: MediaContainerDescriptor): AudioTrack {
        return DiscordAttachmentAudioTrack(trackInfo, containerTrackFactory, this)
    }

    override fun shutdown() {
        // Do nothing.
    }

    private fun <R> inlineTry(friendlyError: String, block: () -> R): R {
        return try {
            block()
        } catch (e: Throwable) {
            throw FriendlyException(friendlyError, FriendlyException.Severity.COMMON, e)
        }
    }

    companion object {
        private val cdnRegex = "^https?://cdn\\.discordapp\\.com/attachments/\\d{17,21}/\\d{17,21}/[a-zA-Z0-9_-]+\\.\\w{2,6}".toRegex()
    }
}
