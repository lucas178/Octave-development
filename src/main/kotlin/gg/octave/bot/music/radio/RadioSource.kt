package gg.octave.bot.music.radio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.util.concurrent.CompletableFuture

interface RadioSource {
    val name: String

    fun nextTrack(context: RadioTrackContext): CompletableFuture<AudioTrack?>
    fun serialize(stream: ByteArrayOutputStream)

    companion object {
        fun deserialize(stream: ByteArrayInputStream): RadioSource {
            if (stream.available() == 0) {
                throw IllegalStateException("Cannot parse RadioSource with no remaining bytes")
            }

            val reader = DataInputStream(stream)

            val ctx = when (val sourceType = reader.readInt()) {
                1 -> DiscordRadio(reader.readUTF())
                2 -> PlaylistRadio(reader.readUTF(), reader.readUTF())
                else -> throw IllegalArgumentException("Invalid contextType $sourceType!")
            }

            reader.close()
            return ctx
        }
    }
}
