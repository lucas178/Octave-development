package gg.octave.bot.music.radio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import gg.octave.bot.Launcher
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.concurrent.CompletableFuture

class DiscordRadio(override val name: String) : RadioSource {
    override fun nextTrack(context: RadioTrackContext) = nextTrack0(context)

    override fun serialize(stream: ByteArrayOutputStream) {
        val writer = DataOutputStream(stream)
        writer.writeInt(1)
        writer.writeUTF(name)
        writer.close() // This invokes flush.
    }

    private fun nextTrack0(context: RadioTrackContext, attempts: Int = 1): CompletableFuture<AudioTrack?> {
        if (attempts > 3) {
            return CompletableFuture.completedFuture(null)
        }

        val randomSong = Launcher.discordFm.getRandomSong(name)
            ?: return nextTrack0(context, attempts + 1)

        val future = CompletableFuture<AudioTrack?>()

        Launcher.players.playerManager.loadItemOrdered(this, randomSong, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                track.userData = context
                future.complete(track)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) = trackLoaded(playlist.tracks.first())

            override fun noMatches() {
                future.complete(null)
            }

            override fun loadFailed(exception: FriendlyException) {
                if (attempts >= 3) {
                    future.complete(null)
                } else {
                    nextTrack0(context, attempts + 1)
                }
            }
        })

        return future
    }
}
