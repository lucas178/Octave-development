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

import com.jagrosh.jdautilities.paginator
import gg.octave.bot.Launcher
import gg.octave.bot.music.utils.TrackContext
import gg.octave.bot.utils.Utils
import gg.octave.bot.utils.extensions.config
import gg.octave.bot.utils.extensions.selfMember
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.entities.Cog

class Queue : Cog {
    @Command(aliases = ["q"], description = "Shows the current queue.")
    fun queue(ctx: Context) {
        val manager = Launcher.players.getExisting(ctx.guild)
            ?: return ctx.send("There's no music player in this guild.\n${PLAY_MESSAGE.format(ctx.trigger)}")

        val queue = manager.queue
        var queueLength = 0L

        ctx.textChannel?.let {
            Launcher.eventWaiter.paginator {
                setUser(ctx.author)
                setTitle("Music Queue")
                setColor(ctx.selfMember?.color)
                setEmptyMessage("**Empty queue.** Add some music with `${ctx.config.prefix}play url|YT search`.")
                finally { message -> message?.delete()?.queue() }

                for (track in queue) {
                    val decodedTrack = Launcher.players.playerManager.decodeAudioTrack(track)

                    entry {
                        buildString {
                            val req = decodedTrack.getUserData(TrackContext::class.java)?.requesterMention?.plus(" ")
                                ?: ""
                            append(req)
                            append("`[").append(Utils.getTimestamp(decodedTrack.duration)).append("]` __[")
                            append(decodedTrack.info.embedTitle)
                            append("](").append(decodedTrack.info.embedUri).append(")__")
                        }
                    }

                    queueLength += decodedTrack.duration
                }

                field("Now Playing", false) {
                    val track = manager.player.playingTrack
                    if (track == null) {
                        "Nothing"
                    } else {
                        "**[${track.info.embedTitle}](${track.info.uri})**"
                    }
                }

                manager.radio?.let {
                    field("Radio") {
                        buildString {
                            append("Currently streaming music from radio station `${it.source.name.capitalize()}`")
                            append(", requested by ${it.requesterMention}")
                            append(". When the queue is empty, random tracks from the station will be added.")
                        }
                    }
                }

                field("Entries", true) { queue.size }
                field("Total Duration", true) { Utils.getTimestamp(queueLength) }
                field("Repeating", true) { manager.repeatOption.name.toLowerCase().capitalize() }
            }.display(it)
        }
    }
}
