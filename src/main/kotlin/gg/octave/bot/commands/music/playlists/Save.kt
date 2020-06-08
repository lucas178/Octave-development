package gg.octave.bot.commands.music.playlists

import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import gg.octave.bot.Launcher
import gg.octave.bot.commands.music.embedTitle
import gg.octave.bot.commands.music.embedUri
import gg.octave.bot.db.music.CustomPlaylist
import gg.octave.bot.utils.extensions.db
import gg.octave.bot.utils.extensions.existingManager
import gg.octave.bot.utils.extensions.friendlierMessage
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.entities.Cog
import java.util.function.Consumer

class Save : Cog {
    @Command(aliases = ["+"], description = "Saves the current track to a custom playlist.")
    fun saveto(ctx: Context, @Greedy playlist: String) {
        val track = ctx.existingManager?.player?.playingTrack
            ?: return ctx.send("There's no player here.") // Shouldn't happen.

        val existingPlaylist = ctx.db.findCustomPlaylist(ctx.author.id, playlist)
            ?: return ctx.send("No custom playlists found with that name.")

        existingPlaylist.addTrack(track.makeClone())
        existingPlaylist.save()

        ctx.send {
            setColor(0x9571D3)
            setTitle("Track Added")
            setDescription("Added [${track.info.embedTitle}](${track.info.embedUri}) to **${existingPlaylist.name}**")
        }
    }

    @Command(aliases = ["sq"], description = "Saves the entire queue to a custom playlist.")
    fun savequeue(ctx: Context, @Greedy playlist: String) {
        val manager = ctx.existingManager
            ?: return ctx.send("There's no player here.") // Shouldn't happen.

        val existingPlaylist = ctx.db.findCustomPlaylist(ctx.author.id, playlist)
            ?: return ctx.send("No custom playlists found with that name.")

        if (manager.queue.isEmpty()) {
            return ctx.send("There's nothing to save - the queue is empty.")
        }

        val tracks = manager.queue.map(Launcher.players.playerManager::decodeTrack)
        existingPlaylist.addTracks(tracks)
        existingPlaylist.save()

        ctx.send {
            setColor(0x9571D3)
            setTitle("Tracks Added")
            setDescription("Added `${tracks.size}` tracks to **${existingPlaylist.name}**")
        }
    }

    @Command(description = "Searches for, and adds a track to a playlist.")
    fun add(ctx: Context, playlist: String, @Greedy query: String) {
        val existingPlaylist = ctx.db.findCustomPlaylist(ctx.author.id, playlist)
            ?: return ctx.send("No custom playlists found with that name.")

        val handler = FunctionalResultHandler(
            Consumer { addToPlaylist(ctx, existingPlaylist, it) },
            Consumer { addToPlaylist(ctx, existingPlaylist, it) },
            Runnable { ctx.send("Nothing found for the given query. Try again?") },
            Consumer { ctx.send("Encountered an issue while looking for the resource.\n`${it.friendlierMessage()}`") }
        )

        val exQuery = if (directResourcePrefixes.any { it in query }) query.removeSurrounding("<", ">") else "ytsearch:$query"
        Launcher.players.playerManager.loadItem(exQuery, handler)
    }

    private fun addToPlaylist(ctx: Context, playlist: CustomPlaylist, item: AudioItem) {
        when (item) {
            is AudioTrack -> {
                playlist.addTrack(item)
                ctx.send {
                    setColor(0x9571D3)
                    setTitle("Track Added")
                    setDescription("Added [${item.info.embedTitle}](${item.info.embedUri}) to **${playlist.name}**")
                }
            }
            is AudioPlaylist -> {
                if (item.isSearchResult) {
                    return addToPlaylist(ctx, playlist, item.tracks.first())
                }

                playlist.addTracks(item.tracks)

                ctx.send {
                    setColor(0x9571D3)
                    setTitle("Track Added")
                    setDescription("Added `${item.tracks.size}` tracks from **${item.name}** to **${playlist.name}**")
                }
            }
        }

        playlist.save()
    }

    companion object {
        private val directResourcePrefixes = listOf("http://", "https://", "spotify:")
    }
}
