package gg.octave.bot.music.radio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import gg.octave.bot.Launcher
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.concurrent.CompletableFuture

class PlaylistRadio(override val name: String, val author: String) : RadioSource {
    override fun nextTrack(context: RadioTrackContext): CompletableFuture<AudioTrack?> {
        val playlist = Launcher.database.getCustomPlaylist(author, name)?.takeIf { it.encodedTracks.isNotEmpty() }
            ?: return CompletableFuture.completedFuture(null)

        val randomTrack = Launcher.players.playerManager.decodeTrack(playlist.encodedTracks.random())
        randomTrack?.userData = context
        return CompletableFuture.completedFuture(randomTrack)
    }

    override fun serialize(stream: ByteArrayOutputStream) {
        val writer = DataOutputStream(stream)
        writer.writeInt(2)
        writer.writeUTF(name)
        writer.writeUTF(author)
        writer.close() // This invokes flush.
    }
}
