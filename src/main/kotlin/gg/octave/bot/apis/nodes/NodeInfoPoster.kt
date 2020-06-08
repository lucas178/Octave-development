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

package gg.octave.bot.apis.nodes

import gg.octave.bot.Launcher
import org.json.JSONObject
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NodeInfoPoster(var nodeId: Int) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    fun postEvery(time: Long, unit: TimeUnit) {
        scheduler.scheduleWithFixedDelay({
            val ramUsedBytes = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
            val ramTotal = Runtime.getRuntime().totalMemory()

            Launcher.database.jedisPool.resource.use {
                it.hset("node-stats", nodeId.toString(),
                    JSONObject()
                        .put("music_players", Launcher.players.size())
                        .put("uptime", ManagementFactory.getRuntimeMXBean().uptime)
                        .put("total_ram", ramTotal)
                        .put("used_ram", ramUsedBytes)
                        .put("thread_count", Thread.activeCount())
                        .put("guild_count", Launcher.shardManager.guildCache.size())
                        .put("cached_users", Launcher.shardManager.userCache.size())
                        .put("shard_slice_start", Launcher.credentials.shardStart)
                        .put("shard_slice_end", Launcher.credentials.shardEnd)
                        .toString()
                )
            }
        }, time, time, unit)
    }
}