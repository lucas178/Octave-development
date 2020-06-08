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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import gg.octave.bot.Launcher
import gg.octave.bot.commands.music.embedTitle
import gg.octave.bot.commands.music.embedUri
import gg.octave.bot.music.filters.DSPFilter
import gg.octave.bot.music.sources.caching.CachingSourceManager
import gg.octave.bot.music.utils.DiscordFMTrackContext
import gg.octave.bot.music.utils.TrackContext
import gg.octave.bot.utils.extensions.data
import gg.octave.bot.utils.extensions.friendlierMessage
import gg.octave.bot.utils.extensions.premiumGuild
import gg.octave.bot.utils.extensions.voiceChannel
import gg.octave.bot.utils.getDisplayValue
import me.devoxin.flight.api.Context
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class MusicManager(val bot: Launcher, val guildId: String, val playerRegistry: PlayerRegistry, val playerManager: AudioPlayerManager) {
    @Volatile
    private var leaveTask: Future<*>? = null

    /** @return Audio player for the guild. */
    val player = playerManager.createPlayer()
    val dspFilter = DSPFilter(player)

    /**  @return Track scheduler for the player.*/
    val scheduler = TrackScheduler(this, player).also(player::addListener)

    /** @return Wrapper around AudioPlayer to use it as an AudioSendHandler. */
    private val sendHandler: AudioPlayerSendHandler = AudioPlayerSendHandler(player)

    private val dbAnnouncementChannel: String?
        get() = bot.db.getGuildData(guildId)?.music?.announcementChannel

    /**
     * @return Voting cooldown.
     */
    var lastVoteTime: Long = 0L

    /**
     * @return Whether there is a vote to skip the song or not.
     */
    var isVotingToSkip = false
    var isVotingToPlay = false
    var lastPlayVoteTime: Long = 0L
    var lastPlayedAt: Long = 0L

    val currentRequestChannel: TextChannel?
        get() {
            return (player.playingTrack ?: scheduler.lastTrack)
                ?.getUserData(TrackContext::class.java)
                ?.requestedChannel
                ?.let { it -> guild?.getTextChannelById(it) }
        }

    val announcementChannel: TextChannel?
        get() {
            val dbAnnChn = dbAnnouncementChannel
            return when {
                dbAnnChn != null -> guild!!.getTextChannelById(dbAnnChn)
                else -> currentRequestChannel
            }
        }

    val guild: Guild?
        get() = Launcher.shardManager.getGuildById(guildId)

    var loops = 0L

    /**
     * @return If the user is listening to DiscordFM
     */
    var discordFMTrack: DiscordFMTrackContext? = null

    fun destroy() {
        scheduler.queue.expire(4, TimeUnit.HOURS)
        player.destroy()
        dspFilter.clearFilters()

        closeAudioConnection()
    }

    fun openAudioConnection(channel: VoiceChannel, ctx: Context): Boolean {
        when {
            !guild?.selfMember!!.hasPermission(channel, Permission.VOICE_CONNECT) -> {
                ctx.send("The bot can't connect to this channel due to a lack of permission.")
                playerRegistry.destroy(guild)
                return false
            }
            channel.userLimit != 0
                && guild?.selfMember!!.hasPermission(channel, Permission.VOICE_MOVE_OTHERS)
                && channel.members.size >= channel.userLimit -> {
                ctx.send("The bot can't join due to the user limit.")
                playerRegistry.destroy(guild)
                return false
            }
            else -> {
                guild?.audioManager?.apply {
                    openAudioConnection(channel)
                    sendingHandler = sendHandler
                }

                ctx.send {
                    setColor(0x9570D3)
                    setTitle("Music Playback")
                    setDescription("Joining channel `${channel.name}`.")
                }
                return true
            }
        }
    }

    fun moveAudioConnection(channel: VoiceChannel) {
        guild?.let {
            if (!it.selfMember.voiceState!!.inVoiceChannel()) {
                throw IllegalStateException("Bot is not in a voice channel")
            }

            if (!it.selfMember.hasPermission(channel, Permission.VOICE_CONNECT)) {
                currentRequestChannel?.sendMessage("I don't have permission to join `${channel.name}`.")?.queue()
                playerRegistry.destroy(it)
                return
            }

            player.isPaused = true
            it.audioManager.openAudioConnection(channel)
            player.isPaused = false

            currentRequestChannel?.sendMessage(EmbedBuilder().apply {
                setTitle("Music Playback")
                setDescription("Moving to channel `${channel.name}`.")
            }.build())?.queue()
        }
    }

    fun closeAudioConnection() {
        guild?.audioManager?.apply {
            closeAudioConnection()
            sendingHandler = null
        }
    }

    fun isAlone() = guild?.selfMember?.voiceState?.channel?.members?.none { !it.user.isBot } ?: true

    val leaveQueued: Boolean
        get() = leaveTask != null

    fun queueLeave() {
        leaveTask?.cancel(false)
        leaveTask = createLeaveTask()
        player.isPaused = true
    }

    fun cancelLeave() {
        leaveTask?.cancel(false)
        leaveTask = null
        player.isPaused = false
    }

    private fun createLeaveTask() = schedulerThread.schedule({ playerRegistry.destroy(guildId.toLong()) }, 30, TimeUnit.SECONDS)

    fun loadAndPlay(ctx: Context, trackUrl: String, trackContext: TrackContext, footnote: String? = null, isNext: Boolean) {
        playerManager.loadItemOrdered(this, trackUrl, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                cache(trackUrl, track)

                if (!guild?.selfMember!!.voiceState!!.inVoiceChannel()) { // wtf is this mess
                    if (!openAudioConnection(ctx.voiceChannel!!, ctx)) {
                        return
                    }
                }

                val queueLimit = queueLimit(ctx)
                val queueLimitDisplay = when (queueLimit) {
                    Integer.MAX_VALUE -> "unlimited"
                    else -> queueLimit.toString()
                }

                if (scheduler.queue.size >= queueLimit) {
                    return ctx.send("The queue can not exceed $queueLimitDisplay songs.")
                }

                if (!track.info.isStream) {
                    val data = ctx.data
                    val premiumGuild = ctx.premiumGuild
                    val invalidDuration = premiumGuild == null && data.music.maxSongLength > bot.configuration.durationLimit.toMillis()

                    val durationLimit = when {
                        data.music.maxSongLength != 0L && !invalidDuration -> data.music.maxSongLength
                        premiumGuild != null -> premiumGuild.songLengthQuota
                        data.isPremium -> TimeUnit.MINUTES.toMillis(360) //Keep key perks.
                        else -> bot.configuration.durationLimit.toMillis()
                    }

                    val durationLimitText = when {
                        data.music.maxSongLength != 0L && !invalidDuration -> getDisplayValue(data.music.maxSongLength)
                        premiumGuild != null -> getDisplayValue(premiumGuild.songLengthQuota)
                        data.isPremium -> getDisplayValue(TimeUnit.MINUTES.toMillis(360)) //Keep key perks.
                        else -> bot.configuration.durationLimitText
                    }

                    if (track.duration > durationLimit) {
                        return ctx.send("The track can not exceed $durationLimitText.")
                    }
                }

                track.userData = trackContext
                lastPlayedAt = System.currentTimeMillis()
                scheduler.queue(track, isNext)

                ctx.send {
                    setColor(0x9570D3)
                    setTitle("Music Queue")
                    setDescription("Added __**[${track.info.embedTitle}](${track.info.embedUri})**__ to queue.")
                    setFooter(footnote)
                }
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                cache(trackUrl, playlist)

                if (playlist.isSearchResult) {
                    return trackLoaded(playlist.tracks.first())
                }

                val queueLimit = queueLimit(ctx)
                val queueLimitDisplay = when (queueLimit) {
                    Integer.MAX_VALUE -> "unlimited"
                    else -> queueLimit.toString()
                }

                if (!guild?.selfMember!!.voiceState!!.inVoiceChannel()) {
                    if (!ctx.member!!.voiceState!!.inVoiceChannel()) {
                        ctx.send("You left the channel before the track is loaded.")

                        // Track is not supposed to load and the queue is empty
                        // destroy player
                        if (scheduler.queue.isEmpty()) {
                            playerRegistry.destroy(guild)
                        }
                        return
                    }
                    if (!openAudioConnection(ctx.voiceChannel!!, ctx)) {
                        return
                    }
                }

                val tracks = playlist.tracks
                var ignored = 0

                var added = 0
                for (track in tracks) {
                    if (scheduler.queue.size + 1 >= queueLimit) {
                        ignored = tracks.size - added
                        break
                    }

                    track.userData = trackContext

                    scheduler.queue(track, isNext)
                    added++
                }

                ctx.send {
                    setColor(0x9570D3)
                    setTitle("Music Queue")
                    val desc = buildString {
                        append("Added `$added` tracks to queue from playlist `${playlist.name}`.\n")
                        if (ignored > 0) {
                            append("Ignored `$ignored` songs as the queue can not exceed `$queueLimitDisplay` songs.")
                        }
                    }
                    setDescription(desc)
                    setFooter(footnote)
                }
            }

            override fun noMatches() {
                // No track found and queue is empty
                // destroy player
                if (player.playingTrack == null && scheduler.queue.isEmpty()) {
                    playerRegistry.destroy(guild)
                }

                ctx.send("Nothing found by `$trackUrl`")
            }

            override fun loadFailed(e: FriendlyException) {
                // No track found and queue is empty
                // destroy player

                if (e.message!!.contains("decoding")) {
                    return
                }

                if (player.playingTrack == null && scheduler.queue.isEmpty()) {
                    playerRegistry.destroy(guild)
                }

                ctx.send(e.friendlierMessage())
            }
        })
    }

    fun queueLimit(ctx: Context): Int {
        val premiumGuild = ctx.premiumGuild
        val data = ctx.data
        val invalidSize = premiumGuild == null && data.music.maxQueueSize > bot.configuration.queueLimit

        return when {
            data.music.maxQueueSize != 0 && !invalidSize -> data.music.maxQueueSize
            premiumGuild != null -> premiumGuild.queueSizeQuota
            data.isPremium -> 500 //Keep key perks.
            else -> bot.configuration.queueLimit
        }
    }

    companion object {
        val schedulerThread = Executors.newSingleThreadScheduledExecutor()
        fun cache(identifier: String, item: AudioItem) = CachingSourceManager.cache(identifier, item)
    }
}
