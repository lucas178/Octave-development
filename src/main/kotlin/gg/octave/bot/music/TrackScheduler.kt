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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import gg.octave.bot.Launcher
import gg.octave.bot.commands.music.embedTitle
import gg.octave.bot.commands.music.embedUri
import gg.octave.bot.db.OptionsRegistry
import gg.octave.bot.music.settings.RepeatOption
import gg.octave.bot.music.utils.TrackContext
import gg.octave.bot.utils.extensions.friendlierMessage
import gg.octave.bot.utils.extensions.insertAt
import gg.octave.bot.utils.extensions.shuffle
import io.sentry.Sentry
import io.sentry.event.Event
import io.sentry.event.EventBuilder
import io.sentry.event.interfaces.StackTraceInterface
import net.dv8tion.jda.api.EmbedBuilder
import org.redisson.api.RQueue

class TrackScheduler(private val manager: MusicManager, private val player: AudioPlayer) : AudioEventAdapter() {
    //Base64 encoded.
    val queue: RQueue<String> = Launcher.db.redisson.getQueue("playerQueue:${manager.guildId}")
    var repeatOption = RepeatOption.NONE
    var lastTrack: AudioTrack? = null
        private set
    var currentTrack: AudioTrack? = null
        private set

    private var lastTimeAnnounced = 0L
    private var lastErrorAnnounced = 0L
    private var errorCount = 0L

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    fun queue(track: AudioTrack, isNext: Boolean) {
        if (!player.startTrack(track, true)) {
            val encoded = Launcher.players.playerManager.encodeAudioTrack(track)
            if (isNext) {
                queue.insertAt(0, encoded)
            } else {
                queue.offer(encoded)
            }
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    fun nextTrack() {
        if (repeatOption != RepeatOption.NONE) {
            val cloneThis = currentTrack
                ?: return

            val cloned = cloneThis.makeClone().also { it.userData = cloneThis.userData }
            // Pretty sure makeClone now copies user data, but better to be safe than sorry.

            if (repeatOption == RepeatOption.SONG) {
                return player.playTrack(cloned)
            } else if (repeatOption == RepeatOption.QUEUE) {
                queue.offer(Launcher.players.playerManager.encodeAudioTrack(cloned))
            } // NONE doesn't need any handling.
        }

        if (queue.isNotEmpty()) {
            val track = queue.poll()
            val decodedTrack = Launcher.players.playerManager.decodeAudioTrack(track)
            return player.playTrack(decodedTrack)
        }

        if (manager.discordFMTrack == null) {
            return player.stopTrack() // Don't disconnect here. We'll wait for the user to queue more music OR
            // for them to run the leave command/disconnect from voice channel.
            // If the user disconnects from the voice channel, the VoiceListener will (read: should) pick up
            // on this and schedule a disconnect 30 seconds from the time of being alone in vc.

            // return MusicManager.schedulerThread.execute { manager.playerRegistry.destroy(manager.guild) }
        }

        manager.discordFMTrack?.let {
            it.nextDiscordFMTrack().thenAccept { track ->
                if (track == null) {
                    return@thenAccept Launcher.players.destroy(manager.guild)
                }

                player.startTrack(track, false)
            }
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        this.lastTrack = track

        if (endReason.mayStartNext) {
            nextTrack()
        }
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long, stackTrace: Array<out StackTraceElement>) {
        val guild = manager.guild ?: return
        track.getUserData(TrackContext::class.java)
            ?.requestedChannel
            ?.let(guild::getTextChannelById)
            ?.sendMessage("The track ${track.info.embedTitle} is stuck longer than ${thresholdMs}ms threshold.")
            ?.queue()

        val eventBuilder = EventBuilder().withMessage("AudioTrack stuck longer than ${thresholdMs}ms")
            .withLevel(Event.Level.ERROR)
            .withSentryInterface(StackTraceInterface(stackTrace))

        Sentry.capture(eventBuilder)
        nextTrack()
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        repeatOption = RepeatOption.NONE

        if (exception.toString().contains("decoding")) {
            return
        }

        Sentry.capture(exception)
        val channel = track.getUserData(TrackContext::class.java)?.requestedChannel?.let {
            manager.guild?.getTextChannelById(it)
        } ?: return

        if (errorCount < 20L && (lastErrorAnnounced == 0L || lastErrorAnnounced + 6000 < System.currentTimeMillis())) {
            channel.sendMessage("An unknown error occurred while playing **${track.info.title}**:\n${exception.friendlierMessage()}")
                .queue {
                    errorCount++
                    lastErrorAnnounced = System.currentTimeMillis()
                }
        }
    }

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        if (currentTrack != null) {
            currentTrack?.let {
                if (it.identifier == track.identifier) {
                    manager.loops++
                } else {
                    manager.loops = 0
                }
            }
        }

        val announce = currentTrack?.identifier != track.identifier
        currentTrack = track

        if (announce && OptionsRegistry.ofGuild(manager.guildId).music.announce) {
            announceNext(track)
        }
    }

    private fun announceNext(track: AudioTrack) {
        val channel = manager.announcementChannel ?: return
        val description = buildString {
            append("Now playing __**[").append(track.info.embedTitle)
            append("](").append(track.info.embedUri).append(")**__")

            val reqData = track.getUserData(TrackContext::class.java)
            append(" requested by ")
            append(reqData?.requesterMention ?: "Unknown")
            append(".")
        }

        // Avoid spamming by just sending it if the last time it was announced was more than 10s ago.
        if (lastTimeAnnounced == 0L || lastTimeAnnounced + 10000 < System.currentTimeMillis()) {
            val embed = EmbedBuilder().setDescription(description).build()
            channel.sendMessage(embed).queue {
                lastTimeAnnounced = System.currentTimeMillis()
            }
        }
    }

    fun shuffle() = queue.shuffle()

    companion object {
        fun getQueueForGuild(guildId: String): RQueue<String> {
            return Launcher.db.redisson.getQueue("playerQueue:$guildId")
        }
    }
}
