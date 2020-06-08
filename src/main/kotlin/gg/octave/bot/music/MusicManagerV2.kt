package gg.octave.bot.music

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import gg.octave.bot.Launcher
import gg.octave.bot.commands.music.embedTitle
import gg.octave.bot.commands.music.embedUri
import gg.octave.bot.db.OptionsRegistry
import gg.octave.bot.music.filters.DSPFilter
import gg.octave.bot.music.radio.PlaylistRadio
import gg.octave.bot.music.radio.RadioTrackContext
import gg.octave.bot.music.settings.RepeatOption
import gg.octave.bot.music.utils.TrackContext
import gg.octave.bot.utils.Task
import gg.octave.bot.utils.extensions.friendlierMessage
import gg.octave.bot.utils.extensions.insertAt
import io.sentry.Sentry
import io.sentry.event.Event
import io.sentry.event.EventBuilder
import io.sentry.event.interfaces.StackTraceInterface
import me.devoxin.flight.api.Context
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import org.redisson.api.RQueue
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class MusicManagerV2(val guildId: Long, val player: AudioPlayer) : AudioSendHandler, AudioEventAdapter() {
    // Meta
    val guild: Guild? get() = Launcher.shardManager.getGuildById(guildId)
    val isAlone: Boolean get() = guild?.selfMember?.voiceState?.channel?.members?.none { !it.user.isBot } ?: true
    val isIdle: Boolean get() = player.playingTrack == null && queue.isEmpty()

    // Playback/Music related.
    val queue: RQueue<String> = Launcher.db.redisson.getQueue("playerQueue:$guildId")
    val dspFilter = DSPFilter(player)
    var lastTrack: AudioTrack? = null
        private set
    var currentTrack: AudioTrack? = null
        private set
    var radio: RadioTrackContext? = null
    var repeatOption = RepeatOption.NONE

    // Settings/internals.
    private val leaveTask = Task(30, TimeUnit.SECONDS) { destroy() }
    val isLeaveQueued: Boolean get() = leaveTask.isRunning

    private var lastTimeAnnounced = 0L
    private var lastErrorAnnounced = 0L
    private var errorCount = 0L

    var lastVoteTime = 0L
    var isVotingToSkip = false
    var isVotingToPlay = false
    var lastPlayVoteTime = 0L
    var lastPlayedAt = 0L

    var loops = 0L
        private set

    // Misc
    private val dbAnnouncementChannel: String? get() = Launcher.db.getGuildData(guildId.toString())?.music?.announcementChannel
    private val currentRequestChannel: TextChannel?
        get() = (player.playingTrack ?: lastTrack)?.getUserData(TrackContext::class.java)
            ?.requestedChannel?.let { guild?.getTextChannelById(it) }
    val announcementChannel: TextChannel?
        get() = dbAnnouncementChannel?.let { guild?.getTextChannelById(it) } ?: currentRequestChannel

    // ---------- End Properties ----------

    init {
        player.addListener(this)
        player.volume = Launcher.db.getGuildData(guildId.toString())?.music?.volume ?: 100
    }

    fun enqueue(track: AudioTrack, isNext: Boolean) {
        if (!player.startTrack(track, true)) {
            val encoded = Launcher.players.playerManager.encodeAudioTrack(track)
            if (isNext) {
                queue.insertAt(0, encoded)
            } else {
                queue.offer(encoded)
            }
        }
    }

    fun openAudioConnection(channel: VoiceChannel, ctx: Context): Boolean {
        when {
            !guild?.selfMember!!.hasPermission(channel, Permission.VOICE_CONNECT) -> {
                ctx.send("The bot can't connect to this channel due to a lack of permission.")
                destroy()
                return false
            }
            channel.userLimit != 0
                && guild?.selfMember!!.hasPermission(channel, Permission.VOICE_MOVE_OTHERS)
                && channel.members.size >= channel.userLimit -> {
                ctx.send("The bot can't join due to the user limit.")
                destroy()
                return false
            }
            else -> {
                guild?.audioManager?.apply {
                    openAudioConnection(channel)
                    sendingHandler = this@MusicManagerV2
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
                return destroy()
            }

            if (!it.selfMember.hasPermission(channel, Permission.VOICE_CONNECT)) {
                currentRequestChannel?.sendMessage("I don't have permission to join `${channel.name}`.")?.queue()
                return destroy()
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

    fun queueLeave() {
        leaveTask.start()
        player.isPaused = true
    }

    fun cancelLeave() {
        leaveTask.stop()
        player.isPaused = false
    }

    fun nextTrack() {
        if (repeatOption != RepeatOption.NONE) {
            val cloneThis = currentTrack
                ?: return

            val cloned = cloneThis.makeClone().also { it.userData = cloneThis.userData } // I think makeClone copies user data, but better safe than sorry.

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

        val radioTrack = radio?.nextTrack()
            ?: return player.stopTrack()

        radioTrack.thenCompose {
            if (radio?.source is PlaylistRadio || it == null || lastTrack == null || it.identifier != lastTrack?.identifier) {
                return@thenCompose CompletableFuture.completedFuture(it)
            }

            return@thenCompose radio?.let { r -> r.source.nextTrack(r) }
                ?: CompletableFuture.completedFuture(it)
        }.thenAccept { player.startTrack(it, false) }
    }

    private fun announceNext(track: AudioTrack) {
        val channel = announcementChannel ?: return
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
            val embed = EmbedBuilder()
                .setColor(0x9570D3)
                .setDescription(description)
                .build()
            channel.sendMessage(embed).queue {
                lastTimeAnnounced = System.currentTimeMillis()
            }
        }
    }

    fun destroy() = Launcher.players.destroy(guildId)

    fun cleanup() {
        player.destroy()
        dspFilter.clearFilters()
        queue.expire(4, TimeUnit.HOURS)

        closeAudioConnection()
    }

    // *----------- Scheduler/Event Handling -----------*
    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        lastPlayedAt = System.currentTimeMillis()
        this.lastTrack = track

        if (endReason.mayStartNext) {
            nextTrack()
        }
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long, stackTrace: Array<out StackTraceElement>) {
        val guild = guild ?: return
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
            guild?.getTextChannelById(it)
        } ?: return

        if (errorCount < 20L && (lastErrorAnnounced == 0L || lastErrorAnnounced + 6000 < System.currentTimeMillis())) {
            channel.sendMessage("An unknown error occurred while playing **${track.info.embedTitle}**:\n${exception.friendlierMessage()}")
                .queue {
                    errorCount++
                    lastErrorAnnounced = System.currentTimeMillis()
                }
        }
    }

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        currentTrack?.let {
            if (it.identifier == track.identifier) {
                loops++
            } else {
                loops = 0
            }
        }

        val announce = currentTrack?.identifier != track.identifier
        currentTrack = track

        if (announce && OptionsRegistry.ofGuild(guildId.toString()).music.announce) {
            announceNext(track)
        }
    }

    // *----------- AudioSendHandler -----------*
    private val frameBuffer = ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize())
    private val lastFrame = MutableAudioFrame().also { it.setBuffer(frameBuffer) }

    override fun canProvide() = player.provide(lastFrame)
    override fun provide20MsAudio() = frameBuffer.flip()
    override fun isOpus() = true

    companion object {
        fun getQueueForGuild(guildId: String): RQueue<String> {
            return Launcher.db.redisson.getQueue("playerQueue:$guildId")
        }
    }
}
