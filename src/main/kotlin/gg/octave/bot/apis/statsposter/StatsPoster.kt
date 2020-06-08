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

package gg.octave.bot.apis.statsposter

import gg.octave.bot.Launcher
import gg.octave.bot.apis.statsposter.websites.*
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StatsPoster(botId: String) {
    init {
        log.info("Posting statistics to bot lists using client (or bot) id {}", botId)
    }

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    val websites = listOf(
        BotsForDiscord(botId, Launcher.credentials.botsForDiscord ?: ""),
        BotsGg(botId, Launcher.credentials.botsGg ?: ""),
        BotsOnDiscord(botId, Launcher.credentials.botsOnDiscord ?: ""),
        TopGg(botId, Launcher.credentials.topGg ?: "")
    )

    fun update(count: Long) {
        for (website in websites.filter(Website::canPost)) {
            website.update(count)
                .thenApply { it.body()?.close() }
                .exceptionally {
                    log.error("Updating server count failed for ${website.name}: ", it)
                    return@exceptionally null
                }
        }
    }

    fun postEvery(time: Long, unit: TimeUnit) {
        scheduler.scheduleWithFixedDelay({
            val guilds = Launcher.database.jedisPool.resource.use { jedis ->
                (0 until Launcher.credentials.totalShards)
                    .map { jedis.hget("stats", it.toString()) }
                    .map(::JSONObject)
                    .map { it.getLong("guild_count") }
                    .sum()
            }

            update(guilds)
        }, time, time, unit)
    }

    companion object {
        private val log = LoggerFactory.getLogger(StatsPoster::class.java)
    }
}
