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

package gg.octave.bot.commands.music.playlists

import gg.octave.bot.commands.music.embedTitle
import gg.octave.bot.commands.music.embedUri
import gg.octave.bot.db.music.CustomPlaylist
import gg.octave.bot.utils.Utils
import gg.octave.bot.utils.extensions.iterate
import gg.octave.bot.utils.extensions.section
import io.sentry.Sentry
import me.devoxin.flight.api.Context
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.jetbrains.kotlin.utils.addToStdlib.sumByLong
import java.util.concurrent.TimeoutException
import kotlin.math.ceil

class PlaylistManager(
    private val playlist: CustomPlaylist,
    private val ctx: Context,
    private var msg: Message
) {
    private val tracks = playlist.decodedTracks

    private val pages: Int
        get() = ceil(tracks.size.toDouble() / ELEMENTS_PER_PAGE).toInt()

    private var page = 1
        set(value) {
            field = value
            renderPage()
        }

    init {
        renderPage()
        waitForInput()
    }

    // COMMAND HANDLING
    private fun sendHelp() {
        ctx.send {
            setColor(0x9571D3)
            setTitle("Editing a Custom Playlist")
            setDescription("""
                `help             :` Shows this menu.
                `remove <index>   :` Removes the track at the specified index.
                `move <track> <to>:` Moves the track at the given index, to the new index.
                `page <#>         :` Tabs to the given page, and displays it.
                `rename <name>    :` Rename the playlist.
                `resend           :` Re-sends the track list.
                `save             :` Saves any modifications made to the playlist.
                `exit             :` Exits playlist editing mode, discarding any changes.
            """.trimIndent())
        }
    }

    private fun remove(index: Int?) {
        if (index == null || index > tracks.size || index < 1) {
            return ctx.send("You need to specify a valid track index, between 1 and ${tracks.size}.")
        }

        tracks.removeAt(index - 1)
        renderPage()
    }

    private fun move(index: Int?, to: Int?) {
        if (index == null || index > tracks.size || index < 1) {
            return ctx.send("You must specify the index of the track you wish to move.")
        }

        if (to == null || to > tracks.size || to < 1 || to == index) {
            return ctx.send("You must specify a valid index to move the track to.")
        }

        val temp = tracks.removeAt(index - 1)
        tracks.add(to - 1, temp)
        renderPage()
    }

    private fun renderPage(update: Boolean = false) {
        val playlistDuration = Utils.getTimestamp(tracks.sumByLong { it.duration })
        val start = ELEMENTS_PER_PAGE * (page - 1)
        val end = (start + ELEMENTS_PER_PAGE).coerceAtMost(tracks.size)
        val trackList = tracks.iterate(start..end)
            .map { (index, track) -> "`${index + 1}.` **[${track.info.embedTitle}](${track.info.embedUri})** `[${Utils.getTimestamp(track.duration)}]`" }
            .joinToString("\n")
            .takeIf { it.isNotEmpty() }
            ?: "No tracks to display."

        val embed = EmbedBuilder().apply {
            setColor(0x9571D3)
            setTitle("Editing ${playlist.name} | ${ctx.author.name}'s playlist")
            setDescription(trackList)
            addField(
                "The bot will be listening for commands until you run `save` or `exit`, or no commands are sent for 20 seconds.",
                "\u200b",
                false
            )
            setFooter("Page $page/$pages - Playlist Duration: $playlistDuration - Send \"help\" to view commands")
        }.build()

        if (update) {
            msg.channel.sendMessage(embed).queue(::msg::set)
        } else {
            msg.editMessage(embed).queue()
        }
    }

    // EVENT WAITING AND INPUT PROCESSING
    // ==================================
    private fun waitForInput() {
        val defaultPredicate = DEFAULT_PREDICATE(ctx.author.idLong, ctx.messageChannel.idLong)
        val response = ctx.commandClient.waitFor(defaultPredicate, 20000)

        response.thenAccept {
            val wait = handle(it.message)

            if (wait) {
                waitForInput()
            }
        }.exceptionally {
            val exc = it.cause ?: it
            playlist.setTracks(tracks)
            playlist.save()

            if (exc !is TimeoutException) {
                Sentry.capture(it)
                ctx.send {
                    setColor(0x9571D3)
                    setTitle("Error in Playlist Management")
                    setDescription(
                        "An unknown error occurred, we apologise for any inconvenience caused.\n" +
                            "Any modifications made to your playlist have been saved."
                    )
                }
            }

            return@exceptionally null
        }
    }

    private fun handle(received: Message): Boolean {
        val (command, args) = received.contentRaw.split("\\s+".toRegex()).section()

        return when (command) {
            "help" -> {
                sendHelp()
                true
            }
            "remove" -> {
                remove(args.firstOrNull()?.toIntOrNull())
                true
            }
            "move" -> {
                move(args.getOrNull(0)?.toIntOrNull(), args.getOrNull(1)?.toIntOrNull())
                true
            }
            "page" -> {
                val page = args.firstOrNull()?.toIntOrNull()?.coerceIn(1, pages)

                if (page == null) {
                    ctx.send("You need to specify the page # to jump to.")
                } else {
                    this.page = page
                }

                true
            }
            "resend" -> {
                renderPage(true)
                true
            }
            "rename" -> {
                val newName = args.joinToString(" ").takeIf { it.isNotEmpty() }

                if (newName == null) {
                    ctx.send("You need to specify a new name for the playlist.")
                } else {
                    playlist.name = newName
                    renderPage()
                }
                true
            }
            "save" -> {
                playlist.setTracks(tracks)
                playlist.save()
                ctx.send("Changes saved. Re-run `${ctx.trigger}cpl edit ${playlist.name}` if you would like to make further modifications.")
                false
            }
            "exit" -> {
                ctx.send("Discarded changes. If you wish to make any further modifications, re-run `${ctx.trigger}cpl edit ${playlist.name}`.")
                false
            }
            else -> false
        }
    }

    companion object {
        private const val ELEMENTS_PER_PAGE = 10
        private val DEFAULT_PREDICATE: (Long, Long) -> (MessageReceivedEvent) -> Boolean = { authorId, channelId ->
            { it.author.idLong == authorId && it.channel.idLong == channelId && isCommand(it.message) }
        }
        private val commands = setOf("help", "remove", "move", "page", "resend", "rename", "send", "save", "exit")

        private fun isCommand(msg: Message): Boolean {
            return commands.any { msg.contentRaw.startsWith(it) }
        }
    }
}
