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

import com.jagrosh.jdautilities.selector
import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import gg.octave.bot.Launcher
import gg.octave.bot.commands.music.embedTitle
import gg.octave.bot.commands.music.embedUri
import gg.octave.bot.utils.Utils
import gg.octave.bot.utils.extensions.selfMember
import gg.octave.bot.utils.extensions.voiceChannel
import me.devoxin.flight.api.Context
import java.awt.Color
import java.util.function.Consumer

fun genericSearchCommand(ctx: Context, query: String, searchPrefix: String, provider: String, color: Color, link: String, icon: String, thumbnail: String) {
    val handler: (List<AudioTrack>) -> Unit = handler@{ results ->
        if (results.isEmpty()) {
            return@handler ctx.send("No search results for `$query`.")
        }

        val botChannel = ctx.selfMember!!.voiceState?.channel
        val userChannel = ctx.voiceChannel

        if (userChannel == null || botChannel != null && botChannel != userChannel) {
            return@handler ctx.send {
                setColor(Color(141, 20, 0))
                setAuthor("$provider Results", link, icon)
                setThumbnail(thumbnail)
                setDescription(
                    results.joinToString("\n") {
                        "**[${it.info.embedTitle}](${it.info.embedUri})**\n" +
                            "**`${Utils.getTimestamp(it.duration)}`** by **${it.info.author}**\n"
                    }
                )
                setFooter("Want to play one of these music tracks? Join a voice channel and reenter this command.", null)
            }
        }

        Launcher.eventWaiter.selector {
            setColor(color)
            setTitle("$provider Results")
            setDescription("Select one of the following options to play them in your current music channel.")
            setUser(ctx.author)

            for (result in results) {
                addOption("`${Utils.getTimestamp(result.info.length)}` **[${result.info.embedTitle}](${result.info.embedUri})**") {
                    if (ctx.member!!.voiceState!!.inVoiceChannel()) {
                        val manager = Launcher.players.get(ctx.guild)
                        val args = query.split(" +".toRegex())
                        Play.smartPlay(ctx, manager, args, true, result.info.uri)
                    } else {
                        ctx.send("You're not in a voice channel anymore!")
                    }
                }
            }

            finally { it?.delete()?.queue() }
        }.display(ctx.textChannel!!)
    }

    Launcher.players.playerManager.loadItem("$searchPrefix:$query", FunctionalResultHandler(
        Consumer { handler(listOf(it)) },
        Consumer {
            if (!it.isSearchResult) handler(emptyList())
            else handler(it.tracks.subList(0, 5.coerceAtMost(it.tracks.size)))
        },
        Runnable { handler(emptyList()) },
        Consumer { handler(emptyList()) }
    ))
}
