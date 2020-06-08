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

package gg.octave.bot.commands.music

import gg.octave.bot.entities.framework.MusicCog
import gg.octave.bot.utils.Utils
import gg.octave.bot.utils.extensions.manager
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import net.dv8tion.jda.api.EmbedBuilder

class DMNowPlaying : MusicCog {
    override fun requirePlayingTrack() = true
    override fun requirePlayer() = true

    private val totalBlocks = 20

    @Command(description = "DMs you the now playing message with the song URL.")
    fun dmnp(ctx: Context) {
        val manager = ctx.manager

        val track = manager.player.playingTrack
        val percent = track.position.toDouble() / track.duration
        val progress = buildString {
            for (i in 0 until totalBlocks) {
                if ((percent * (totalBlocks - 1)).toInt() == i) {
                    append("__**\u25AC**__")
                } else {
                    append("\u2015")
                }
            }
            append(" **%.1f**%%".format(percent * 100))
        }

        ctx.author.openPrivateChannel().flatMap { channel ->
            channel.sendMessage(EmbedBuilder().apply {
                setDescription(
                    "**[${track.info.embedTitle}](${track.info.embedUri})**\n" +
                        "Track URL: ${track.info.uri}"
                )
                addField("Repeating", manager.repeatOption.name.toLowerCase().capitalize(), true)
                addField("Bass Boost", manager.dspFilter.bassBoost.name.toLowerCase().capitalize(), true)
                val timeString = if (track.duration == Long.MAX_VALUE) {
                    "`Streaming`"
                } else {
                    val position = Utils.getTimestamp(track.position)
                    val duration = Utils.getTimestamp(track.duration)
                    "`[$position / $duration]`"
                }
                addField("Time", timeString, true)
                addField("Progress", progress, false)
            }.build())
        }.queue()
    }
}
