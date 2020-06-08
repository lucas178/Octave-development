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

import com.jagrosh.jdautilities.paginator
import com.sun.management.OperatingSystemMXBean
import gg.octave.bot.Launcher
import gg.octave.bot.utils.Capacity
import gg.octave.bot.utils.OctaveBot
import gg.octave.bot.utils.Utils
import gg.octave.bot.utils.extensions.config
import gg.octave.bot.utils.extensions.selfMember
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import net.dv8tion.jda.api.JDAInfo
import org.json.JSONObject
import java.lang.management.ManagementFactory
import java.text.DecimalFormat

class BotInfo : Cog {
    private val dpFormatter = DecimalFormat("0.00")

    @Command(aliases = ["about", "info", "stats"], description = "Show information about the bot.")
    fun botinfo(ctx: Context) {
        val commandSize = ctx.commandClient.commands.size

        // Uptime
        val s = ManagementFactory.getRuntimeMXBean().uptime / 1000
        val m = s / 60
        val h = m / 60
        val d = h / 24

        val osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val procCpuUsage = dpFormatter.format(osBean.processCpuLoad * 100)
        val sysCpuUsage = dpFormatter.format(osBean.systemCpuLoad * 100)
        val ramUsedBytes = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
        val ramUsedCalculated = Capacity.calculate(ramUsedBytes)
        val ramUsedFormatted = dpFormatter.format(ramUsedCalculated.amount)
        var guilds = 0L
        var users = 0L
        var musicPlayers = 0L
        var totalNodes = 0L
        var totalMemory = 0L

        Launcher.database.jedisPool.resource.use {
            val stats = it.hgetAll("stats")
            for (shard in stats) {
                val jsonStats = JSONObject(shard.value);
                guilds += jsonStats.getLong("guild_count")
                users += jsonStats.getLong("cached_users")
            }

            val nodeStats = it.hgetAll("node-stats")
            for (node in nodeStats) {
                val jsonStats = JSONObject(node.value);
                musicPlayers += jsonStats.getLong("music_players")
                totalMemory += jsonStats.getLong("used_ram")
                totalNodes++
            }
        }

        val totalRamCalculated = Capacity.calculate(totalMemory)
        val totalRamFormatted = dpFormatter.format(totalRamCalculated.amount)

        ctx.send {
            setColor(0x9570D3)
            setTitle("Octave (Revision ${OctaveBot.GIT_REVISION})")
            setThumbnail(ctx.jda.selfUser.avatarUrl)
            setDescription("Never miss a beat with Octave, " +
                "a simple and easy to use Discord music bot delivering high quality audio to hundreds of thousands of servers." +
                " We support Youtube, Soundcloud, and more!")

            addField("CPU Usage", "${procCpuUsage}% JVM\n${sysCpuUsage}% SYS", true)
            addField("RAM Usage", "$ramUsedFormatted${ramUsedCalculated.unit}\nAll: $totalRamFormatted${totalRamCalculated.unit}", true)
            addField("Guilds", guilds.toString(), true)
            addField("Voice Connections", musicPlayers.toString(), true)

            addField("Cached Users", users.toString(), true)
            addField("Uptime", "${d}d ${h % 24}h ${m % 60}m ${s % 60}s", true)

            val general = buildString {
                append("Premium: **[Patreon](https://www.patreon.com/octavebot)**\n")
                append("Commands: **$commandSize**\n")
                append("Library: **[JDA ${JDAInfo.VERSION}](${JDAInfo.GITHUB})**\n")
            }
            addField("General", general, true)
            setFooter("${Thread.activeCount()} threads | Current Shard: ${ctx.jda.shardInfo.shardId} | Current Node: ${ctx.config.nodeNumber + 1} / $totalNodes")
        }
    }

    @SubCommand(description = "Gets node statistics.")
    fun nodes(ctx: Context) {
        ctx.textChannel?.let { tx ->
            Launcher.eventWaiter.paginator {
                setTitle("Node Statistics")
                setUser(ctx.author)
                setDescription("Per-node breakdown of the bot statistics.\nA node contains a set amount of shards.\n" +
                    "**Current Node**: ${Launcher.configuration.nodeNumber} (${Launcher.configuration.nodeNumber + 1})")
                setColor(ctx.selfMember?.color)
                Launcher.database.jedisPool.resource.use {
                    val nodeStats = it.hgetAll("node-stats")
                    for (node in nodeStats) {
                        val stats = JSONObject(node.value);

                        val ramUsedBytes = stats.getLong("used_ram")
                        val ramUsedCalculated = Capacity.calculate(ramUsedBytes)
                        val ramUsedFormatted = dpFormatter.format(ramUsedCalculated.amount)
                        val ramUsedPercent = dpFormatter.format(ramUsedBytes.toDouble() / Runtime.getRuntime().totalMemory() * 100)

                        entry {
                            StringBuilder().append("Node ${node.key}\n")
                                .append("**Slice**: ${stats.getInt("shard_slice_start")} to ${stats.getInt("shard_slice_end") - 1}\n")
                                .append("**Uptime**: ${Utils.getTimestamp(stats.getLong("uptime"))}\n")
                                .append("**RAM Usage:** $ramUsedFormatted${ramUsedCalculated.unit} ($ramUsedPercent%)\n")
                                .append("**Threads**: ${stats.getLong("thread_count")}\n")
                                .append("**Guilds**: ${stats.getLong("guild_count")}\n")
                                .append("**Cached Users**: ${stats.getLong("cached_users")}\n")
                                .append("**Players**: ${stats.getLong("music_players")}\n")
                                .toString()
                        }
                    }
                }

                setItemsPerPage(3)
            }.display(tx)
        }
    }
}
