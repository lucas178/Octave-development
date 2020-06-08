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

package gg.octave.bot.entities.framework

import gg.octave.bot.Launcher
import gg.octave.bot.commands.music.PLAY_MESSAGE
import gg.octave.bot.utils.extensions.selfMember
import gg.octave.bot.utils.extensions.voiceChannel
import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.entities.Cog

interface MusicCog : Cog {
    fun sameChannel() = false
    fun requirePlayingTrack() = false
    fun requirePlayer() = false
    fun requireManager() = true
    fun requireVoiceState() = false

    override fun localCheck(ctx: Context, command: CommandFunction) = check(ctx)

    fun check(ctx: Context): Boolean {
        val manager = Launcher.players.getExisting(ctx.guild)

        if (requireManager() && manager == null) {
            ctx.send("There's no music player in this server.\n${PLAY_MESSAGE.format(ctx.trigger)}")
            return false
        }

        val botChannel = ctx.selfMember!!.voiceState?.channel

        if (requireVoiceState() && ctx.voiceChannel == null) {
            ctx.send("You're not in a voice channel.")
            return false
        }

        if (requirePlayer() && botChannel == null) {
            ctx.send("The bot is not currently in a voice channel.\n${PLAY_MESSAGE.format(ctx.trigger)}")
            return false
        }

        if (sameChannel() && ctx.voiceChannel != botChannel) {
            ctx.send("You're not in the same channel as the bot.")
            return false
        }

        if (requirePlayingTrack() && manager?.player?.playingTrack == null) {
            ctx.send("The player is not playing anything.")
            return false
        }

        return true
    }
}
