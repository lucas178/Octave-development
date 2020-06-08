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

package gg.octave.bot.commands.admin

import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import net.dv8tion.jda.api.JDA

class RestartShards : Cog {
    @Command(description = "Restart bot shards.", developerOnly = true)
    fun restartshards(ctx: Context, shardId: Int) {
        if (shardId < 0 || shardId >= ctx.jda.shardInfo.shardTotal) {
            return ctx.send("**$shardId** should be equal to or higher than 0, and less than ${ctx.jda.shardInfo.shardTotal}.")
        }

        ctx.jda.shardManager?.restart(shardId)
        ctx.send("Rebooting shard **$shardId**...")
    }

    @SubCommand(description = "Restart any shards that aren't connected.")
    fun dead(ctx: Context) {
        val deadShards = ctx.jda.shardManager?.shards?.filter { it.status != JDA.Status.CONNECTED }?.takeIf { it.isNotEmpty() }
            ?: return ctx.send("There are no dead shards.")

        deadShards.forEach { it.shardManager?.restart(it.shardInfo.shardId) }
        ctx.send("Queued **${deadShards.size}** shards for reboot.")
    }

    @SubCommand(description = "Restart all shards.")
    fun all(ctx: Context) {
        ctx.send("Rebooting all **${ctx.jda.shardInfo.shardTotal}** shards...")
        ctx.jda.shardManager?.restart()
    }
}