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

package gg.octave.bot.commands.music.search

import gg.octave.bot.commands.music.PLAY_MESSAGE
import gg.octave.bot.entities.framework.DJ
import gg.octave.bot.utils.extensions.existingManager
import gg.octave.bot.utils.extensions.selfMember
import gg.octave.bot.utils.extensions.voiceChannel
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.entities.Cog

class PlayNext : Cog {
    @DJ
    @Command(aliases = ["pn"], description = "Adds a song at the start of the queue.")
    fun playnext(ctx: Context, @Greedy query: String) {
        val manager = ctx.existingManager
            ?: return ctx.send("There's no queue here.\n${PLAY_MESSAGE.format(ctx.trigger)}")

        val botChannel = ctx.selfMember!!.voiceState?.channel
            ?: return ctx.send("I'm not currently playing anything.\n${PLAY_MESSAGE.format(ctx.trigger)}")
        val userChannel = ctx.voiceChannel

        if (botChannel != userChannel) {
            return ctx.send("The bot is already playing music in another channel.")
        }

        val args = query.split(" +".toRegex())
        Play.smartPlay(ctx, manager, args, false, "", true)
    }
}
