package gg.octave.bot.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import gg.octave.bot.Launcher
import gg.octave.bot.commands.music.embedTitle
import gg.octave.bot.commands.music.embedUri
import gg.octave.bot.music.sources.caching.CachingSourceManager
import gg.octave.bot.music.utils.TrackContext
import gg.octave.bot.utils.extensions.data
import gg.octave.bot.utils.extensions.friendlierMessage
import gg.octave.bot.utils.extensions.premiumGuild
import gg.octave.bot.utils.extensions.voiceChannel
import gg.octave.bot.utils.getDisplayValue
import me.devoxin.flight.api.Context
import java.util.concurrent.TimeUnit

class LoadResultHandler(
    private val identifier: String?,
    private val ctx: Context,
    private val musicManager: MusicManagerV2,
    private val trackContext: TrackContext,
    private val isNext: Boolean,
    private val footnote: String? = null
) : AudioLoadResultHandler {
    private val settings = ctx.data
    private val premiumGuild = ctx.premiumGuild
    private var isRetry = false

    override fun trackLoaded(track: AudioTrack) {
        cache(track)

        if (!checkVoiceState() || !checkTrack(track, false)) {
            return
        }

        track.userData = trackContext
        musicManager.enqueue(track, isNext)

        ctx.send {
            setColor(0x9570D3)
            setTitle("Music Queue")
            setDescription("Added __**[${track.info.embedTitle}](${track.info.embedUri})**__ to queue.")
            setFooter(footnote)
        }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        cache(playlist)

        if (playlist.isSearchResult) {
            return trackLoaded(playlist.tracks.first())
        }

        if (!checkVoiceState()) {
            return
        }

        val pendingEnqueue = playlist.tracks.filter { checkTrack(it, true) }
        val added = pendingEnqueue.size
        val ignored = playlist.tracks.size - added

        for (track in pendingEnqueue) {
            track.userData = trackContext
            musicManager.enqueue(track, isNext)
        }

        ctx.send {
            setColor(0x9570D3)
            setTitle("Music Queue")
            val desc = buildString {
                append("Added `$added` tracks to queue from playlist `${playlist.name}`.\n")
                if (ignored > 0) {
                    append("`$ignored` tracks were not added, either because the queue limit was hit, or the track is too long.")
                }
            }
            setDescription(desc)
            setFooter(footnote)
        }
    }

    override fun loadFailed(exception: FriendlyException) {
        if (musicManager.isIdle) {
            musicManager.destroy()
        }

        ctx.send {
            setColor(0x9570D3)
            setTitle("Load Results")
            setDescription("Unable to load the track:\n`${exception.friendlierMessage()}`")
        }
    }

    override fun noMatches() {
        if (!isRetry && identifier != null) {
            isRetry = true
            Launcher.players.playerManager.loadItemOrdered(ctx.guild!!.idLong, identifier, this)
            return
        }

        if (musicManager.isIdle) {
            musicManager.destroy()
        }

        ctx.send {
            setColor(0x9570D3)
            setTitle("Load Results")
            setDescription("Nothing found by `$identifier`")
        }
    }

    private fun checkVoiceState(): Boolean {
        val manager = ctx.guild?.audioManager
            ?: return false

        if (manager.connectedChannel == null) {
            if (ctx.voiceChannel == null) {
                ctx.send {
                    setColor(0x9570D3)
                    setTitle("Music Queue")
                    setDescription("You left the voice channel before the track was loaded.")
                }

                if (musicManager.isIdle) {
                    musicManager.destroy()
                }

                return false
            }

            return musicManager.openAudioConnection(ctx.voiceChannel!!, ctx)
        }

        return true
    }

    private fun checkTrack(track: AudioTrack, silent: Boolean): Boolean {
        val queueLimit = queueLimit()
        val queueLimitDisplay = if (queueLimit == Integer.MAX_VALUE) "unlimited" else queueLimit.toString()

        if (musicManager.queue.size + 1 >= queueLimit) {
            if (!silent) {
                ctx.send("The queue can not exceed $queueLimitDisplay songs.")
            }
            return false
        }

        if (!track.info.isStream) {
            val invalidDuration = premiumGuild == null && settings.music.maxSongLength > Launcher.configuration.durationLimit.toMillis()

            val durationLimit = when {
                settings.music.maxSongLength != 0L && !invalidDuration -> settings.music.maxSongLength
                premiumGuild != null -> premiumGuild.songLengthQuota
                settings.isPremium -> TimeUnit.MINUTES.toMillis(360) //Keep key perks.
                else -> Launcher.configuration.durationLimit.toMillis()
            }

            val durationLimitText = when {
                settings.music.maxSongLength != 0L && !invalidDuration -> getDisplayValue(settings.music.maxSongLength)
                premiumGuild != null -> getDisplayValue(premiumGuild.songLengthQuota)
                settings.isPremium -> getDisplayValue(TimeUnit.MINUTES.toMillis(360)) //Keep key perks.
                else -> Launcher.configuration.durationLimitText
            }

            if (track.duration > durationLimit) {
                if (!silent) {
                    ctx.send("The track can not exceed $durationLimitText.")
                }
                return false
            }
        }

        return true
    }

    private fun queueLimit(): Int {
        val invalidSize = premiumGuild == null && settings.music.maxQueueSize > Launcher.configuration.queueLimit

        return when {
            settings.music.maxQueueSize != 0 && !invalidSize -> settings.music.maxQueueSize
            premiumGuild != null -> premiumGuild.queueSizeQuota
            settings.isPremium -> 500 //Keep key perks.
            else -> Launcher.configuration.queueLimit
        }
    }

    fun cache(item: AudioItem) {
        if (identifier != null) {
            CachingSourceManager.cache(identifier, item)
        }
    }

    companion object {
        fun loadItem(query: String, ctx: Context, musicManager: MusicManagerV2, trackContext: TrackContext,
                     isNext: Boolean, footnote: String? = null) {
            val resultHandler = LoadResultHandler(query, ctx, musicManager, trackContext, isNext, footnote)
            Launcher.players.playerManager.loadItemOrdered(ctx.guild!!.idLong, query, resultHandler)
        }
    }
}
