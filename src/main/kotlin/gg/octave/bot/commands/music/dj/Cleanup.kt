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
import gg.octave.bot.entities.`typealias`.Predicate
import gg.octave.bot.entities.framework.CheckVoiceState
import gg.octave.bot.entities.framework.DJ
import gg.octave.bot.entities.framework.MusicCog
import gg.octave.bot.entities.framework.Usages
import gg.octave.bot.music.utils.TrackContext
import gg.octave.bot.utils.extensions.DEFAULT_SUBCOMMAND
import gg.octave.bot.utils.extensions.manager
import gg.octave.bot.utils.getDisplayValue
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.annotations.SubCommand
import net.dv8tion.jda.api.entities.Member
import java.time.Duration

class Cleanup : MusicCog {
    override fun sameChannel() = true
    override fun requirePlayingTrack() = true
    override fun requirePlayer() = true

    @DJ
    @CheckVoiceState
    @Command(aliases = ["cu"], description = "Clear songs based on a specific user, duplicates, or if a user left")
    fun cleanup(ctx: Context, @Greedy member: Member?) {
        if (member == null) {
            return DEFAULT_SUBCOMMAND(ctx)
        }

        val oldSize = ctx.manager.queue.size

        val predicate: Predicate = {
            val track = Launcher.players.playerManager.decodeAudioTrack(it)
            track.getUserData(TrackContext::class.java)?.requester == member.idLong
        }

        ctx.manager.queue.removeIf(predicate)
        val newSize = ctx.manager.queue.size

        val removed = oldSize - newSize
        if (removed == 0) {
            return ctx.send("There are no songs to clear.")
        }

        ctx.send("Removed $removed songs from the user ${member.user.asTag}.")
    }

    @SubCommand(aliases = ["absent"], description = "Removes tracks queued by absent members.")
    fun left(ctx: Context) {
        val oldSize = ctx.manager.queue.size

        // Return Boolean: True if track should be removed
        val predicate: Predicate = check@{
            val track = Launcher.players.playerManager.decodeAudioTrack(it)

            val req = track.getUserData(TrackContext::class.java)?.let { m -> ctx.guild?.getMemberById(m.requester) }
                ?: return@check true

            return@check req.voiceState?.channel?.idLong != ctx.guild!!.selfMember.voiceState?.channel?.idLong
        }

        ctx.manager.queue.removeIf(predicate)
        val newSize = ctx.manager.queue.size
        val removed = oldSize - newSize
        if (removed == 0) {
            return ctx.send("There are no songs to clear.")
        }

        ctx.send("Removed $removed songs from users no longer in the voice channel.")
    }

    @SubCommand(aliases = ["d", "dupes"], description = "Removes tracks that exist multiple times in the queue.")
    fun duplicates(ctx: Context) {
        val oldSize = ctx.manager.queue.size

        val tracks = mutableSetOf<String>()
        // Return Boolean: True if track should be removed (could not add to set: already exists).
        val predicate: Predicate = {
            val track = Launcher.players.playerManager.decodeAudioTrack(it)
            !tracks.add(track.identifier)
        }

        ctx.manager.queue.removeIf(predicate)
        val newSize = ctx.manager.queue.size

        val removed = oldSize - newSize
        if (removed == 0) {
            return ctx.send("There are no duplicate songs to clear.")
        }
    }

    @SubCommand(aliases = ["longerthan", "duration", "time"], description = "Removes tracks that are longer than the given duration.")
    @Usages("20m")
    fun exceeds(ctx: Context, duration: Duration) {
        val oldSize = ctx.manager.queue.size

        ctx.manager.queue.removeIf { Launcher.players.playerManager.decodeAudioTrack(it).duration > duration.toMillis() }
        val newSize = ctx.manager.queue.size

        val removed = oldSize - newSize
        if (removed == 0) {
            return ctx.send("There are no songs to clear.")
        }

        ctx.send("Removed $removed songs longer than ${getDisplayValue(duration.toMillis())} minutes.")
    }
}
