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

import gg.octave.bot.Launcher
import gg.octave.bot.commands.music.PLAY_MESSAGE
import gg.octave.bot.music.LoadResultHandler
import gg.octave.bot.music.radio.DiscordRadio
import gg.octave.bot.music.radio.PlaylistRadio
import gg.octave.bot.music.radio.RadioTrackContext
import gg.octave.bot.utils.DiscordFM
import gg.octave.bot.utils.extensions.DEFAULT_SUBCOMMAND
import gg.octave.bot.utils.extensions.db
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import org.apache.commons.lang3.StringUtils

class Radio : Cog {
    private val stations = DiscordFM.LIBRARIES.joinToString("\n") { "• `${it.capitalize()}`" }

    @Command(description = "Stream random songs from some radio stations.")
    fun radio(ctx: Context) = DEFAULT_SUBCOMMAND(ctx)

    @SubCommand(aliases = ["dfm"], description = "Listen to songs from predefined playlists.")
    fun discordfm(ctx: Context, @Greedy station: String?) {
        if (station == null) {
            return ctx.send {
                setColor(0x9570D3)
                setDescription(
                    buildString {
                        appendln("Stream random songs from radio stations!")
                        appendln("Select and stream a station using `${ctx.trigger}radio <station name>`.")
                        append("Stop streaming songs from a station with `${ctx.trigger}radio stop`,")
                    }
                )
                addField("Available Stations", stations, false)
            }
        }

        val query = station.toLowerCase()
        // quick check for incomplete query
        // classic -> classical
        val library = DiscordFM.LIBRARIES.firstOrNull { it.contains(query) }
            ?: DiscordFM.LIBRARIES.minBy { StringUtils.getLevenshteinDistance(it, query) }
            ?: return ctx.send("Library $query doesn't exist. Available stations: `${DiscordFM.LIBRARIES.contentToString()}`.")

        val manager = Launcher.players.get(ctx.guild)

        val track = Launcher.discordFm.getRandomSong(library)
            ?: return ctx.send("Couldn't find any tracks in that library.")

        val trackContext = RadioTrackContext(DiscordRadio(library), ctx.author.idLong, ctx.textChannel!!.idLong)
        manager.radio = trackContext

        ctx.send {
            setColor(0x9570D3)
            setTitle("Radio")
            setDescription("Radio set to `$library`. The radio will be played when there's nothing else queued.")
        }

        if (manager.isIdle) {
            LoadResultHandler.loadItem(track, ctx, manager, trackContext, false, "Now streaming random tracks from the `$library` radio station!")
        }
    }

    @SubCommand(aliases = ["cpl", "pl"], description = "Listen to songs from your custom playlists.")
    fun custom(ctx: Context, @Greedy playlistName: String) {
        val playlist = ctx.db.findCustomPlaylist(ctx.author.id, playlistName)
            ?: return ctx.send {
                setColor(0x9570D3)
                setTitle("Radio")
                setDescription("Couldn't find any playlists with that name.\n" +
                    "To view a list of your playlists, run `${ctx.trigger}cpl list`.")
            }

        if (playlist.encodedTracks.isEmpty()) {
            return ctx.send {
                setColor(0x9570D3)
                setTitle("Radio")
                setDescription("`${playlist.name}` contains no tracks. Try another playlist.")
            }
        }

        val manager = Launcher.players.get(ctx.guild)
        val trackContext = RadioTrackContext(PlaylistRadio(playlist.name, ctx.author.id), ctx.author.idLong, ctx.textChannel!!.idLong)
        manager.radio = trackContext

        ctx.send {
            setColor(0x9570D3)
            setTitle("Radio")
            setDescription("Radio set to `${playlist.name}`. The radio will be played when there's nothing else queued.")
        }

        if (manager.isIdle) {
            val lrh = LoadResultHandler(null, ctx, manager, trackContext, false,
                "Now streaming random tracks from the playlist `${playlist.name}`!")

            val randomTrack = Launcher.players.playerManager.decodeTrack(playlist.encodedTracks.random())
                ?: return ctx.send {
                    setColor(0x9570D3)
                    setTitle("Radio")
                    setDescription("Failed to retrieve a track from the playlist. Try again, or perhaps try another playlist.")
                }

            lrh.trackLoaded(randomTrack)
        }
    }

    @SubCommand(description = "Turns off the radio.")
    fun stop(ctx: Context) {
        val manager = Launcher.players.getExisting(ctx.guild)
            ?: return ctx.send("There's no music player in this guild.\n${PLAY_MESSAGE.format(ctx.trigger)}")

        val source = manager.radio?.source
            ?: return ctx.send("The radio is not active.")

        val message = when (source) {
            is DiscordRadio -> "the `${source.name.capitalize()}` station."
            is PlaylistRadio -> "<@${source.author}>'s playlist, `${source.name}`."
            else -> "an unknown source."
        }

        manager.radio = null

        ctx.send {
            setColor(0x9570D3)
            setTitle("Radio")
            setDescription("No longer streaming random songs from $message")
        }
    }
}
