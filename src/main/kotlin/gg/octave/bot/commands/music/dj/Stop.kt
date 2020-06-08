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

import gg.octave.bot.Launcher
import gg.octave.bot.entities.framework.CheckVoiceState
import gg.octave.bot.entities.framework.DJ
import gg.octave.bot.entities.framework.MusicCog
import gg.octave.bot.utils.extensions.manager
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command

class Stop : MusicCog {
    @DJ
    @CheckVoiceState
    @Command(aliases = ["end", "st", "fuckoff"], description = "Stop and clear the music player.")
    fun stop(ctx: Context, clear: Boolean = false) {
        val karen = ctx.manager

        if (clear) {
            karen.queue.clear()
        }

        karen.radio = null
        ctx.guild!!.audioManager.closeAudioConnection()
        Launcher.players.destroy(ctx.guild!!.idLong)

        val extra = when {
            !clear && karen.queue.isEmpty() -> "."
            clear -> ", and the queue has been cleared."
            else -> ". If you want to clear the queue run `${ctx.trigger}clearqueue` or `${ctx.trigger}stop yes`."
        }

        ctx.send("Playback has been completely stopped$extra")
    }
}
