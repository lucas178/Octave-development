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

package gg.octave.bot.music

import gg.octave.bot.Launcher
import gg.octave.bot.db.OptionsRegistry
import gg.octave.bot.utils.Scheduler
import net.dv8tion.jda.api.entities.Guild
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PlayerRegistry {
    val playerManager = ExtendedAudioPlayerManager()
    val registry = ConcurrentHashMap<Long, MusicManagerV2>(Launcher.configuration.musicLimit)
    private val executor = Executors.newSingleThreadScheduledExecutor()

    init {
        Scheduler.fixedRateScheduleWithSuppression(executor, 3, 3, TimeUnit.MINUTES) { sweep() }
    }

    fun sweep() {
        registry.values.filter {
            // If guild null, or if connected, and not playing, and not queued for leave,
            // if last played >= IDLE_TIMEOUT minutes ago, and not 24/7 (all day) music, destroy/queue leave.
            it.guild == null || it.guild!!.audioManager.isConnected && it.player.playingTrack == null &&
                !it.isLeaveQueued && System.currentTimeMillis() - it.lastPlayedAt > 120000 &&
                !isAllDayMusic(it.guildId.toString())
        }.forEach {
            if (it.guild == null) {
                destroy(it.guildId)
            } else {
                it.queueLeave() //Then queue leave.
            }
        }
    }

    fun get(guild: Guild?) = registry.computeIfAbsent(guild!!.idLong) { MusicManagerV2(guild.idLong, playerManager.createPlayer()) }

    fun getExisting(id: Long) = registry[id]
    fun getExisting(guild: Guild?) = getExisting(guild!!.idLong)
    fun destroy(id: Long) {
        registry.remove(id)?.cleanup()
    }

    fun destroy(guild: Guild?) = destroy(guild!!.idLong)
    fun contains(id: Long) = registry.containsKey(id)
    fun contains(guild: Guild) = registry.containsKey(guild.idLong)

    fun size() = registry.size

    private fun isAllDayMusic(guildId: String): Boolean {
        val premium = Launcher.database.getPremiumGuild(guildId)
        val guildData = OptionsRegistry.ofGuild(guildId)
        val key = guildData.isPremium

        return (premium != null || key) && guildData.music.isAllDayMusic
    }
}
