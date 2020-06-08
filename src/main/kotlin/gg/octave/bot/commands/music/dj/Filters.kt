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

import gg.octave.bot.entities.framework.*
import gg.octave.bot.music.MusicManagerV2
import gg.octave.bot.utils.extensions.DEFAULT_SUBCOMMAND
import gg.octave.bot.utils.extensions.manager
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.SubCommand

class Filters : MusicCog {
    override fun sameChannel() = true
    override fun requirePlayingTrack() = true
    override fun requirePlayer() = true

    @DJ
    @CheckVoiceState
    @DonorOnly
    @Command(aliases = ["filters", "fx", "effects"], description = "Apply audio filters to the music such as speed and pitch")
    fun filter(ctx: Context) = DEFAULT_SUBCOMMAND(ctx)

    @Usages("depth 0.5")
    @SubCommand(description = "Wobble effect.")
    fun tremolo(ctx: Context, type: String, value: Double) = modifyTremolo(ctx, type, value, ctx.manager)

    @Usages("speed 1.5")
    @SubCommand(description = "Pitch, rate, and speed.")
    fun timescale(ctx: Context, type: String, value: Double) = modifyTimescale(ctx, type, value, ctx.manager)

    @Usages("width 100")
    @SubCommand(description = "Karaoke settings for better vocal filtering.")
    fun karaoke(ctx: Context, type: String?, value: Float?) = modifyKaraoke(ctx, type, value, ctx.manager)

    @SubCommand(description = "Check the current status of filters.")
    fun status(ctx: Context) {
        val manager = ctx.manager
        val karaokeStatus = if (manager.dspFilter.karaokeEnable) "Enabled" else "Disabled"
        val tremoloStatus = if (manager.dspFilter.tremoloEnable) "Enabled" else "Disabled"
        val timescaleStatus = if (manager.dspFilter.timescaleEnable) "Enabled" else "Disabled"

        ctx.send {
            setColor(0x9570D3)
            setTitle("Music Effects")
            addField("Karaoke", karaokeStatus, true)
            addField("Timescale", timescaleStatus, true)
            addField("Tremolo", tremoloStatus, true)
        }
    }

    @SubCommand(description = "Clear all filters.")
    fun clear(ctx: Context) {
        ctx.manager.dspFilter.clearFilters()
        ctx.send("Cleared all filters.")
    }

    private fun modifyTimescale(ctx: Context, type: String, amount: Double, manager: MusicManagerV2) {
        val value = amount.coerceIn(0.1, 3.0)

        when (type) {
            "pitch" -> manager.dspFilter.tsPitch = value
            "speed" -> manager.dspFilter.tsSpeed = value
            "rate" -> manager.dspFilter.tsRate = value
            else -> return ctx.send("Invalid choice `$type`, pick one of `pitch`/`speed`/`rate`.")
        }

        ctx.send("Timescale `${type.toLowerCase()}` set to `$value`")
    }

    private fun modifyTremolo(ctx: Context, type: String, amount: Double, manager: MusicManagerV2) {
        when (type) {
            "depth" -> {
                val depth = amount.coerceIn(0.0, 1.0)
                manager.dspFilter.tDepth = depth.toFloat()
                ctx.send("Tremolo `depth` set to `$depth`")
            }

            "frequency" -> {
                val frequency = amount.coerceAtLeast(0.1)
                manager.dspFilter.tFrequency = frequency.toFloat()
                ctx.send("Tremolo `frequency` set to `$frequency`")
            }
            else -> ctx.send("Invalid choice `$type`, pick one of `depth`/`frequency`.")
        }
    }

    private fun modifyKaraoke(ctx: Context, type: String?, amount: Float?, manager: MusicManagerV2) {
        if (type != null && (type == "level" || type == "band" || type == "width") && amount == null) {
            return ctx.send("You must specify a valid number for `amount`.")
        }

        when (type) {
            "level" -> {
                val level = amount!!.coerceAtLeast(0.0f)
                manager.dspFilter.kLevel = level
                return ctx.send("Karaoke `$type` set to `$level`")
            }
            "band" -> manager.dspFilter.kFilterBand = amount!!
            "width" -> manager.dspFilter.kFilterWidth = amount!!
            else -> return ctx.send("Invalid choice, `type` must be `level`/`band`/`width`.")
        }

        ctx.send("Karaoke `$type` set to `$amount`")
    }
}