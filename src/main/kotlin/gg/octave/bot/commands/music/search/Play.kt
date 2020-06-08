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

import com.jagrosh.jdautilities.menu.Selector
import com.jagrosh.jdautilities.menu.SelectorBuilder
import gg.octave.bot.Launcher
import gg.octave.bot.listeners.FlightEventAdapter
import gg.octave.bot.music.LoadResultHandler
import gg.octave.bot.music.MusicManagerV2
import gg.octave.bot.music.utils.TrackContext
import gg.octave.bot.utils.extensions.config
import gg.octave.bot.utils.extensions.data
import gg.octave.bot.utils.extensions.selfMember
import gg.octave.bot.utils.extensions.voiceChannel
import gg.octave.bot.utils.getDisplayValue
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.entities.Cog
import net.dv8tion.jda.api.EmbedBuilder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class Play : Cog {
    @Command(aliases = ["p"], description = "Plays music in a voice channel.")
    fun play(ctx: Context, @Greedy query: String?) {
        val botChannel = ctx.selfMember!!.voiceState?.channel
        val userChannel = ctx.voiceChannel ?: return ctx.send("You're not in a voice channel.")

        if (botChannel != null && botChannel != userChannel) {
            return ctx.send("The bot is already playing music in another channel.")
        }

        val attachment = ctx.message.attachments.firstOrNull()

        if (query == null && attachment == null) {
            return playArgless(ctx)
        }

        val args = attachment?.let { listOf(it.url) } ?: query!!.split(" +".toRegex())
        val hasManager = Launcher.players.contains(ctx.guild!!.idLong)

        prompt(ctx, hasManager).thenAccept { proceed ->
            if (!proceed) {
                return@thenAccept
            }

            val newManager = Launcher.players.get(ctx.guild)
            smartPlay(ctx, newManager, args, false, "")
        }.exceptionally {
            ctx.send("An error occurred!")
            it.printStackTrace()
            return@exceptionally null
        }
    }

    private fun playArgless(ctx: Context) {
        val manager = Launcher.players.getExisting(ctx.guild)
            ?: return ctx.send("There's no music player in this guild.\n\uD83C\uDFB6 `${ctx.trigger}play (song/url)` to start playing some music!")

        when {
            manager.player.isPaused -> {
                manager.player.isPaused = false

                ctx.send {
                    setColor(0x9570D3)
                    setTitle("Play Music")
                    setDescription("Music is no longer paused.")
                }
            }
            manager.player.playingTrack != null -> {
                ctx.send("Music is already playing. Are you trying to queue a track? Try adding a search term with this command!")
            }
            manager.queue.isEmpty() -> {
                ctx.send {
                    setColor(0x9570D3)
                    setTitle("Empty Queue")
                    setDescription("There is no music queued right now. Add some songs with `${ctx.trigger}play (song/url)`.")
                }
            }
        }
    }

    private fun prompt(ctx: Context, hasManager: Boolean): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        val oldQueue = MusicManagerV2.getQueueForGuild(ctx.guild!!.id)

        if (!hasManager && !oldQueue.isEmpty()) {
            SelectorBuilder(Launcher.eventWaiter)
                .setType(Selector.Type.MESSAGE)
                .setUser(ctx.author)
                .setTitle("Would you like to keep your old queue?")
                .setDescription("Thanks for using Octave!")
                .addOption("Yes, keep it.") {
                    ctx.send("Kept old queue. Playing new song first and continuing with your queue...")
                    future.complete(true)
                }
                .addOption("No, start a new queue.") {
                    oldQueue.clear()
                    ctx.send("Scrapped old queue. A new queue will start.")
                    future.complete(true)
                }
                .finally {
                    if (!future.isDone) { // Timeout or cancel.
                        future.complete(false)
                        it?.delete()?.queue()
                    }
                }
                .build()
                .display(ctx.textChannel!!)
        } else {
            future.complete(true)
        }

        return future
    }

    companion object {
        fun smartPlay(ctx: Context, manager: MusicManagerV2, args: List<String>, isSearchResult: Boolean, uri: String, isNext: Boolean = false) {
            when {
                ctx.data.music.isVotePlay && !FlightEventAdapter.isDJ(ctx, false) -> startPlayVote(ctx, manager!!, args, isSearchResult, uri, isNext)
                else -> play(ctx, args, isSearchResult, uri, isNext)
            }
        }

        fun play(ctx: Context, args: List<String>, isSearchResult: Boolean, uri: String, isNext: Boolean = false) {
            val manager = Launcher.players.get(ctx.guild)
            val config = ctx.config

            //Reset expire time if play has been called.
            manager.queue.clearExpire()

            val query = when {
                "https://" in args[0] || "http://" in args[0] || args[0].startsWith("spotify:") -> {
                    args[0].removePrefix("<").removeSuffix(">")
                }
                isSearchResult -> uri
                else -> "ytsearch:${args.joinToString(" ").trim()}"
            }

            val trackContext = TrackContext(ctx.author.idLong, ctx.textChannel!!.idLong)
            val footnote = if (!isSearchResult) "You can search and pick results using ${config.prefix}youtube or ${config.prefix}soundcloud while in a channel." else null
            LoadResultHandler.loadItem(query, ctx, manager, trackContext, isNext, footnote)
        }

        fun startPlayVote(ctx: Context, manager: MusicManagerV2, args: List<String>, isSearchResult: Boolean, uri: String, isNext: Boolean) {
            if (manager.isVotingToPlay) {
                return ctx.send("There is already a vote going on!")
            }

            val data = ctx.data

            val votePlayCooldown = if (data.music.votePlayCooldown <= 0) {
                ctx.config.votePlayCooldown.toMillis()
            } else {
                data.music.votePlayCooldown
            }

            if (System.currentTimeMillis() - manager.lastPlayVoteTime < votePlayCooldown) {
                return ctx.send("You must wait $votePlayCooldown before starting a new vote.")
            }

            val votePlayDuration = if (data.music.votePlayDuration == 0L) {
                data.music.votePlayDuration
            } else {
                ctx.config.votePlayDuration.toMillis()
            }

            val votePlayDurationText = if (data.music.votePlayDuration == 0L) {
                ctx.config.votePlayDurationText
            } else {
                getDisplayValue(data.music.votePlayDuration)
            }

            manager.lastPlayVoteTime = System.currentTimeMillis()
            manager.isVotingToPlay = true
            val channel = ctx.selfMember!!.voiceState!!.channel ?: ctx.voiceChannel!!
            val halfPeople = channel.members.filter { !it.user.isBot }.size / 2

            ctx.messageChannel.sendMessage(EmbedBuilder().apply {
                setTitle("Vote Play")
                setDescription(
                    buildString {
                        append(ctx.author.asMention)
                        append(" has voted to **play** a track!")
                        append(" React with :thumbsup:\n")
                        append("If there are more than $halfPeople vote(s) within $votePlayDurationText, the track will be queued.")
                    }
                )
            }.build())
                .submit()
                .thenCompose { m ->
                    m.addReaction("👍")
                        .submit()
                        .thenApply { m }
                }
                .thenCompose {
                    it.editMessage(EmbedBuilder(it.embeds[0])
                        .apply {
                            setDescription("Voting has ended! Check the newer messages for results.")
                            clearFields()
                        }.build()
                    ).submitAfter(votePlayDuration, TimeUnit.MILLISECONDS)
                }.thenAccept { m ->
                    val votes = m.reactions.firstOrNull { it.reactionEmote.name == "👍" }?.count?.minus(1) ?: 0

                    ctx.send {
                        setColor(0x9570D3)
                        setTitle("Vote Play")
                        setDescription(
                            buildString {
                                if (votes > halfPeople) {
                                    appendln("The vote has passed! The song will be queued.")
                                    play(ctx, args, isSearchResult, uri, isNext)
                                } else {
                                    appendln("The vote has failed! The song will not be queued.")
                                }
                            }
                        )
                        addField("Results", "__$votes Play Votes__", false)
                    }
                }.whenComplete { _, _ ->
                    manager.isVotingToPlay = false
                }
        }
    }
}
