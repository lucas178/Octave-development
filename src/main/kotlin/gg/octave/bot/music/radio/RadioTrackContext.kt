package gg.octave.bot.music.radio

import gg.octave.bot.music.utils.TrackContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class RadioTrackContext(
    val source: RadioSource,
    requester: Long,
    requestedChannel: Long
) : TrackContext(requester, requestedChannel) {
    fun nextTrack() = source.nextTrack(this)

    override fun serialize(stream: ByteArrayOutputStream) {
        val writer = DataOutputStream(stream)
        writer.writeInt(3)
        // 1 => TrackContext
        // 2 => DiscordFMTrackContext
        // 3 => RadioTrackContext
        writer.writeLong(requester)
        writer.writeLong(requestedChannel)
        source.serialize(stream)
        writer.close() // This invokes flush.
    }
}
