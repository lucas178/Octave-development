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
import gg.octave.bot.entities.framework.MusicCog
import gg.octave.bot.utils.extensions.config
import gg.octave.bot.utils.extensions.manager
import gg.octave.bot.utils.extensions.removeAt
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command

class SkipTo : MusicCog {
    override fun sameChannel() = true
    override fun requirePlayingTrack() = true
    override fun requirePlayer() = true

    @DJ
    @CheckVoiceState
    @Command(aliases = ["skt"], description = "Skip the current music track.")
    fun skipTo(ctx: Context, where: Int?) {
        val manager = ctx.manager

        val toIndex = where?.takeIf { it > 0 && it <= manager.queue.size }
            ?: return ctx.send("You need to specify the position of the track in the queue that you want to skip to.")

        if (toIndex - 1 == 0) {
            return ctx.send("Use the `${ctx.config.prefix}skip` command to skip single tracks.")
        }

        for (i in 0 until toIndex - 1) {
            manager.queue.removeAt(0)
        }

        manager.nextTrack()
        ctx.send("Skipped **${toIndex - 1}** tracks.")
    }
}