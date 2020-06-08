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

package gg.octave.bot.listeners

import gg.octave.bot.Launcher
import gg.octave.bot.db.OptionsRegistry
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.*
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.awt.Color
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class BotListener : EventListener {
    private val log = LoggerFactory.getLogger(BotListener::class.java)

    override fun onEvent(event: GenericEvent) {
        when (event) {
            is GuildJoinEvent -> onGuildJoin(event)
            is GuildMessageReceivedEvent -> onGuildMessageReceived(event)
            is GuildLeaveEvent -> onGuildLeave(event)
            is StatusChangeEvent -> onStatusChange(event)
            is ReadyEvent -> onReady(event)
            is ResumedEvent -> onResume(event)
            is ReconnectedEvent -> onReconnect(event)
            is DisconnectEvent -> onDisconnect(event)
            is ExceptionEvent -> onException(event)
        }
    }

    private fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author.isBot && event.author === event.jda.selfUser) {
            val guildOptions = OptionsRegistry.ofGuild(event.guild)
            if (guildOptions.command.isAutoDelete) {
                val deleteDelay = guildOptions.command.autoDeleteDelay.takeIf { it > 0 }
                    ?: TimeUnit.SECONDS.toMillis(10)
                event.message.delete().queueAfter(deleteDelay, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun onGuildJoin(event: GuildJoinEvent) {
        //Don't fire this if the SelfMember joined a longish time ago. This avoids discord fuckups.
        if (event.guild.selfMember.timeJoined.isBefore(OffsetDateTime.now().minusSeconds(30))) return

        //Find the first channel we can talk to.
        val channel = event.guild.textChannels.firstOrNull { it.canTalk() }
            ?: return

        //Greet message start.
        val prefix = Launcher.configuration.prefix
        val embedBuilder = EmbedBuilder()
            .setThumbnail(event.jda.selfUser.effectiveAvatarUrl)
            .setColor(Color.BLUE)
            .setDescription("Welcome to Octave! The highest quality Discord music bot!\n" +
                "Please check the links below to get help, and use `${prefix}help` to get started!")
            .addField("Important Links",
                "[Support Server](https://discord.gg/musicbot)\n" +
                    "[Website](https://octave.gg) \n" +
                    "[Invite Link](https://invite.octave.gg)\n" +
                    "[Patreon](https://patreon.com/octave)", true)
            .setFooter("Thanks for using Octave!")

        channel.sendMessage(embedBuilder.build()).queue { it.delete().queueAfter(1, TimeUnit.MINUTES) }

        Launcher.datadog.gauge("octave_bot.guilds", Launcher.shardManager.guildCache.size())
        Launcher.datadog.gauge("octave_bot.users", Launcher.shardManager.userCache.size())
        Launcher.datadog.gauge("octave_bot.players", Launcher.players.size().toLong())
        Launcher.datadog.incrementCounter("octave_bot.guildJoin")
        postStats(event.jda)
    }

    private fun onGuildLeave(event: GuildLeaveEvent) {
        Launcher.players.destroy(event.guild)
        Launcher.datadog.gauge("octave_bot.guilds", Launcher.shardManager.guildCache.size())
        Launcher.datadog.gauge("octave_bot.users", Launcher.shardManager.userCache.size())
        Launcher.datadog.gauge("octave_bot.players", Launcher.players.size().toLong())
        Launcher.datadog.incrementCounter("octave_bot.guildLeave")
        postStats(event.jda)
    }

    private fun onStatusChange(event: StatusChangeEvent) {
        if (event.newStatus.ordinal >= JDA.Status.LOADING_SUBSYSTEMS.ordinal) {
            log.info("Shard #{} Status: {} -> {}", event.jda.shardInfo.shardId, event.oldStatus, event.newStatus)
        }

        postStats(event.jda)
    }

    private fun onReady(event: ReadyEvent) {
        Launcher.datadog.incrementCounter("octave_bot.shardReady")
        log.info("JDA ${event.jda.shardInfo.shardId} is ready.")
        postStats(event.jda)
    }

    private fun onResume(event: ResumedEvent) {
        Launcher.datadog.incrementCounter("octave_bot.shardResume")
        log.info("JDA ${event.jda.shardInfo.shardId} has resumed.")
        postStats(event.jda)
    }

    private fun onReconnect(event: ReconnectedEvent) {
        Launcher.datadog.incrementCounter("octave_bot.shardReconnect")
        log.info("JDA ${event.jda.shardInfo.shardId} has reconnected.")
        postStats(event.jda)
    }

    private fun onDisconnect(event: DisconnectEvent) {
        Launcher.datadog.incrementCounter("octave_bot.shardDisconnect")

        if (event.isClosedByServer) {
            log.info("JDA {} disconnected (closed by server). Code: {} {}",
                event.jda.shardInfo.shardId, event.serviceCloseFrame?.closeCode ?: -1, event.closeCode)
        } else {
            log.info("JDA {} disconnected. Code: {} {}",
                event.jda.shardInfo.shardId, event.serviceCloseFrame?.closeCode
                ?: -1, event.clientCloseFrame?.closeReason ?: "")
        }
    }

    private fun onException(event: ExceptionEvent) {
        Launcher.datadog.incrementCounter("octave_bot.exception")
        if (!event.isLogged)
            log.error("Exception in JDA {}", event.jda.shardInfo.shardId, event.cause)
    }

    private fun postStats(jda: JDA) {
        if (jda.status == JDA.Status.INITIALIZED) {
            return
        }

        Launcher.database.jedisPool.resource.use {
            it.hset("stats", jda.shardInfo.shardId.toString(), JSONObject()
                .put("guild_count", jda.guildCache.size())
                .put("cached_users", jda.userCache.size())
                .put("status", jda.status)
                .put("ping", jda.gatewayPing)
                .toString()
            )
        }
    }
}
