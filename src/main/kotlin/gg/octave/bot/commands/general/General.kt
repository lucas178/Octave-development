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

package gg.octave.bot.commands.general

import kotlinx.coroutines.future.await
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.entities.Cog
import net.dv8tion.jda.api.Permission

class General : Cog {
    @Command(aliases = ["invitebot", "add", "link", "links"], description = "Get a link to invite the bot to your server.")
    fun invite(ctx: Context) {
        val link = ctx.jda.getInviteUrl(Permission.ADMINISTRATOR)

        ctx.send {
            setColor(0x9570D3)
            setTitle("Add Octave to your server!")
            setDescription("__**[Click to invite Octave to your server.]($link)**__")
        }
    }

    @Command(aliases = ["pong", "hello????"], description = "Show the bot's current response time.")
    suspend fun ping(ctx: Context) {
        val latency = ctx.jda.restPing.submit().await()
        ctx.send(
            "```prolog\n" +
                "Shard ID: ${ctx.jda.shardInfo.shardId}\n" +
                "Latency (HTTP): ${latency}ms\n" +
                "Latency (WS  ): ${ctx.jda.shardManager!!.averageGatewayPing.toInt()}ms```"
        )
    }

    @Command(description = "Shows how to vote for the bot.")
    fun vote(ctx: Context) {
        ctx.send {
            setColor(0x9570D3)
            setTitle("Vote")
            setDescription(
                "Vote here to increase the visibility of the bot!\nIf you vote for Octave, you can get a normie box in Dank Memer everytime you vote too!\n" +
                    "**[Vote by clicking here](https://discordbots.org/bot/octave/vote)**"
            )
        }
    }

    @Command(aliases = ["supportserver"], description = "Shows a link to the support server.")
    fun support(ctx: Context) {
        ctx.send {
            setColor(0x9570D3)
            setTitle("Support Server")
            setDescription("[Join our support server by clicking here!](https://discord.gg/musicbot)")
        }
    }

    @Command(description = "Show the donation info.")
    fun donate(ctx: Context) {
        ctx.send {
            setColor(0x9570D3)
            setDescription("Want to donate to support Octave?\n**[Patreon](https://www.patreon.com/octavebot)**")
        }
    }
}
