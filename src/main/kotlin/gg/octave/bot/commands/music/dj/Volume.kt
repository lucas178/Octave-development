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

package gg.octave.bot.commands.music.dj

import gg.octave.bot.entities.framework.CheckVoiceState
import gg.octave.bot.entities.framework.DJ
import gg.octave.bot.entities.framework.DonorOnly
import gg.octave.bot.entities.framework.MusicCog
import gg.octave.bot.utils.extensions.config
import gg.octave.bot.utils.extensions.data
import gg.octave.bot.utils.extensions.manager
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command

class Volume : MusicCog {
    override fun requirePlayer() = true

    private val totalBlocks = 20
    private val maximumVolume = 150

    @DJ
    @CheckVoiceState
    @DonorOnly
    @Command(aliases = ["v", "vol"], description = "Set the volume of the music player.")
    fun volume(ctx: Context, amount: Int?) {
        if (amount == null) {
            val volume = ctx.manager.player.volume
            val bar = buildBar(volume, maximumVolume)

            return ctx.send {
                setColor(0x9570D3)
                setTitle("Volume")
                setDescription(bar)
                setFooter("Set the volume by using ${ctx.config.prefix}volume (number)", null)
            }
        }

        val newVolume = amount.coerceIn(0, maximumVolume)
        val oldVolume = ctx.manager.player.volume
        val bar = buildBar(newVolume, maximumVolume)

        ctx.manager.player.volume = newVolume
        ctx.data.music.volume = newVolume

        ctx.send {
            setColor(0x9570D3)
            setTitle("Volume")
            setDescription(bar)
            setFooter("Volume changed from $oldVolume% to ${ctx.manager.player.volume}%")
        }
    }

    private fun buildBar(value: Int, maximum: Int): String {
        val percent = (value.toDouble() / maximum).coerceIn(0.0, 1.0)

        return buildString {
            for (i in 0 until totalBlocks) {
                if ((percent * (totalBlocks - 1)).toInt() == i) {
                    append("__**\u25AC**__")
                } else {
                    append("\u2015")
                }
            }
            append(" **%.0f**%%".format(percent * maximum))
        }
    }
}
