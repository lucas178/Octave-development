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

import gg.octave.bot.entities.framework.MusicCog
import gg.octave.bot.utils.extensions.config
import gg.octave.bot.utils.extensions.data
import gg.octave.bot.utils.extensions.manager
import gg.octave.bot.utils.extensions.selfMember
import gg.octave.bot.utils.getDisplayValue
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import net.dv8tion.jda.api.EmbedBuilder
import java.util.concurrent.TimeUnit

class VoteSkip : MusicCog {
    override fun sameChannel() = true
    override fun requirePlayingTrack() = true
    override fun requirePlayer() = true

    @Command(aliases = ["vs"], description = "Vote to skip the current music track.")
    suspend fun voteskip(ctx: Context) {
        val manager = ctx.manager

        if (ctx.member!!.voiceState!!.isDeafened) {
            return ctx.send("You actually have to be listening to the song to start a vote.")
        }

        if (manager.isVotingToSkip) {
            return ctx.send("There's already a vote going on.")
        }

        val data = ctx.data

        val voteSkipCooldown = if (data.music.voteSkipCooldown == 0L) {
            ctx.config.voteSkipCooldown.toMillis()
        } else {
            data.music.voteSkipCooldown
        }

        val voteSkipCooldownText = if (data.music.voteSkipCooldown == 0L) {
            ctx.config.voteSkipCooldownText
        } else {
            getDisplayValue(data.music.voteSkipCooldown)
        }

        if (System.currentTimeMillis() - manager.lastVoteTime < voteSkipCooldown) {
            return ctx.send("You must wait $voteSkipCooldownText before starting a new vote.")
        }

        val voteSkipDuration = if (data.music.voteSkipDuration == 0L) {
            ctx.config.voteSkipDuration.toMillis()
        } else {
            data.music.voteSkipDuration
        }

        val voteSkipDurationText = if (data.music.voteSkipDuration == 0L) {
            ctx.config.voteSkipDurationText
        } else {
            val durationMinutes = ctx.config.voteSkipDuration.toMinutes()
            if (durationMinutes > 0) {
                "$durationMinutes minutes"
            } else {
                "${ctx.config.voteSkipDuration.toSeconds()} seconds"
            }
        }

        if (manager.player.playingTrack.duration - manager.player.playingTrack.position <= voteSkipDuration) {
            return ctx.send("By the time the vote finishes in $voteSkipDurationText, the song will be over.")
        }

        manager.lastVoteTime = System.currentTimeMillis()
        manager.isVotingToSkip = true
        val halfPeople = ctx.selfMember!!.voiceState!!.channel!!.members.filterNot { it.user.isBot }.size / 2

        val message = ctx.sendAsync {
            setTitle("Vote Skip")
            setDescription(
                buildString {
                    append(ctx.message.author.asMention)
                    append(" has voted to **skip** the current track!")
                    append(" React with :thumbsup:\n")
                    append("If at least **${halfPeople + 1}** vote(s) from listeners are obtained " +
                        "within **$voteSkipDurationText**, the song will be skipped!")
                }
            )
        }

        message.addReaction("👍")
            .submit()
            .thenApply { message }
            .thenCompose {
                it.editMessage(EmbedBuilder(it.embeds[0])
                    .apply {
                        setDescription("Voting has ended! Check the newer messages for results.")
                        clearFields()
                    }.build()
                ).submitAfter(voteSkipDuration, TimeUnit.MILLISECONDS)
            }.thenAccept { m ->
                val skip = m.reactions.firstOrNull { it.reactionEmote.name == "👍" }?.count?.minus(1) ?: 0

                ctx.send {
                    setColor(0x9570D3)
                    setTitle("Vote Skip")
                    setDescription(
                        buildString {
                            if (skip > halfPeople) {
                                appendln("The vote has passed! The song has been skipped.")
                                manager.nextTrack()
                            } else {
                                appendln("The vote has failed! The song will stay.")
                            }
                        }
                    )
                    addField("Results", "__$skip Skip Votes__", false)
                }
            }.whenComplete { _, _ ->
                manager.isVotingToSkip = false
            }
    }
}
