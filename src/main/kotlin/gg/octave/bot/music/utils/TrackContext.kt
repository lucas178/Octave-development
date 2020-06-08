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

package gg.octave.bot.music.utils

import gg.octave.bot.music.radio.DiscordRadio
import gg.octave.bot.music.radio.RadioSource
import gg.octave.bot.music.radio.RadioTrackContext
import java.io.*

open class TrackContext(val requester: Long, val requestedChannel: Long) {
    val requesterMention = if (requester != -1L) "<@$requester>" else ""
    val channelMention = if (requestedChannel != -1L) "<#$requestedChannel>" else ""

    open fun serialize(stream: ByteArrayOutputStream) {
        val writer = DataOutputStream(stream)
        writer.writeInt(1)
        // 1 => TrackContext
        // 2 => DiscordFMTrackContext
        // 3 => RadioTrackContext
        writer.writeLong(requester)
        writer.writeLong(requestedChannel)
        writer.close() // This invokes flush.
    }

    companion object {
        fun deserialize(stream: ByteArrayInputStream): TrackContext? {
            if (stream.available() == 0) {
                return null
            }

            try {
                val reader = DataInputStream(stream)
                val contextType = reader.readInt()
                val requester = reader.readLong()
                val requestedChannel = reader.readLong()

                val ctx = when (contextType) {
                    1 -> TrackContext(requester, requestedChannel)
                    2 -> {
                        val station = reader.readUTF()
                        //DiscordFMTrackContext(station, requester, requestedChannel)
                        RadioTrackContext(DiscordRadio(station), requester, requestedChannel)
                    }
                    3 -> {
                        val source = RadioSource.deserialize(stream)
                        RadioTrackContext(source, requester, requestedChannel)
                    }
                    else -> throw IllegalArgumentException("Invalid contextType $contextType!")
                }

                reader.close()
                return ctx
            } catch (e: EOFException) {
                println("End of stream; no user data to be read! Remaining bytes: ${stream.available()}")
                return null
            }
        }
    }
}
