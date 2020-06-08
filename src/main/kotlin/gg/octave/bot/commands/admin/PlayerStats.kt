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

import gg.octave.bot.Launcher
import gg.octave.bot.music.MusicManagerV2
import gg.octave.bot.music.settings.BoostSetting
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.entities.Cog
import org.json.JSONObject

class PlayerStats : Cog {
    @Command(aliases = ["ps"], description = "Shows (est) encoding, and total players", developerOnly = true)
    fun playerstats(ctx: Context) {
        val players = Launcher.players.registry.values
        var musicPlayers = 0L

        Launcher.database.jedisPool.resource.use {
            val nodeStats = it.hgetAll("node-stats")
            for (node in nodeStats) {
                val jsonStats = JSONObject(node.value);
                musicPlayers += jsonStats.getLong("music_players")
            }
        }

        val paused = players.count { it.player.isPaused }
        val encoding = players.count(::isEncoding)
        val alone = players.count {
            it.guild?.audioManager?.connectedChannel?.members?.none { m -> !m.user.isBot } ?: false
        }
        val bySource = players.mapNotNull { it.player.playingTrack?.sourceManager?.sourceName }.groupingBy { it }.eachCount()
        //val bySource = sources.associateBy({ it }, { players.count { m -> isSource(it, m) } })
        val bySourceFormatted = bySource.map { "• ${it.key.capitalize()}: **${it.value}**" }.joinToString("\n")

        ctx.send {
            setColor(0x9570D3)
            setTitle("Player Statistics")
            setDescription("**This node**: ${players.size}\n" +
                "**All nodes**: $musicPlayers")
            addField("Source Insight", "**This node:**\n$bySourceFormatted", true)
            addField("Statistics", "**This node:**\n• **$encoding** encoding\n• **$paused** paused\n• **$alone** alone", true)
        }
    }

    private fun isEncoding(manager: MusicManagerV2): Boolean {
        val hasDspFx = manager.dspFilter.let {
            it.karaokeEnable || it.timescaleEnable || it.tremoloEnable || it.bassBoost != BoostSetting.OFF
        }

        return manager.player.playingTrack != null &&
            (manager.player.volume != 100 || hasDspFx || !opusSources.any { it in manager.player.playingTrack.info.uri })
    }

    companion object {
        private val opusSources = listOf("youtube", "soundcloud")
    }
}
