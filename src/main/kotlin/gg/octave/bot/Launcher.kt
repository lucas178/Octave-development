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

package gg.octave.bot

import com.jagrosh.jdautilities.waiter.EventWaiter
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import com.timgroup.statsd.NonBlockingStatsDClient
import gg.octave.bot.apis.nodes.NodeInfoPoster
import gg.octave.bot.apis.patreon.PatreonAPI
import gg.octave.bot.apis.statsposter.StatsPoster
import gg.octave.bot.db.Database
import gg.octave.bot.entities.BotCredentials
import gg.octave.bot.entities.Configuration
import gg.octave.bot.entities.ExtendedShardManager
import gg.octave.bot.entities.framework.DefaultPrefixProvider
import gg.octave.bot.entities.framework.parsers.*
import gg.octave.bot.listeners.BotListener
import gg.octave.bot.listeners.FlightEventAdapter
import gg.octave.bot.listeners.VoiceListener
import gg.octave.bot.music.PlayerRegistry
import gg.octave.bot.utils.DiscordFM
import gg.octave.bot.utils.OctaveBot
import gg.octave.bot.utils.extensions.registerAlmostAllParsers
import io.sentry.Sentry
import jodd.util.concurrent.ThreadFactoryBuilder
import me.devoxin.flight.FlightInfo
import me.devoxin.flight.api.CommandClient
import me.devoxin.flight.api.CommandClientBuilder
import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.requests.RestAction
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Launcher {
    val configuration = Configuration(File("bot.conf"))
    val credentials = BotCredentials(File("credentials.conf"))
    val database = Database("bot")
    val db = database

    val eventWaiter = EventWaiter()
    val datadog = NonBlockingStatsDClient("statsd", "localhost", 8125)
    val statsPoster = StatsPoster(configuration.clientId)
    val patreon = PatreonAPI(credentials.patreonAccessToken)

    val players = PlayerRegistry()
    val discordFm = DiscordFM()

    lateinit var shardManager: ExtendedShardManager
        private set // begone foul modifications

    lateinit var commandClient: CommandClient
        private set // uwu

    val loaded: Boolean
        get() = shardManager.shardsRunning == shardManager.shardsTotal

    lateinit var commandExecutor: ExecutorService
        private set

    @ExperimentalStdlibApi
    @JvmStatic
    fun main(args: Array<String>) {
        println("+---------------------------------+")
        println("|           O c t a v e           |")
        println("|        Revision ${OctaveBot.GIT_REVISION}        |")
        println("| JDA   : ${JDAInfo.VERSION.padEnd(24)}|")
        println("| Flight: ${FlightInfo.VERSION.padEnd(24)}|")
        println("| LP    : ${PlayerLibrary.VERSION.padEnd(24)}|")
        println("+---------------------------------+")

        Sentry.init(configuration.sentryDsn)
        Sentry.getStoredClient().release = OctaveBot.GIT_REVISION
        RestAction.setPassContext(false)

        commandExecutor = Executors.newCachedThreadPool(
            ThreadFactoryBuilder().setNameFormat("Octave-FlightExecutor-%d").get()
        )

        commandClient = CommandClientBuilder()
            .setPrefixProvider(DefaultPrefixProvider())
            .registerAlmostAllParsers()
            .addCustomParser(ExtendedMemberParser())
            .addCustomParser(DurationParser())
            .addCustomParser(KeyTypeParser())
            .addCustomParser(BoostSettingParser())
            .addCustomParser(RepeatOptionParser())
            .setOwnerIds(*configuration.admins.toLongArray())
            .addEventListeners(FlightEventAdapter())
            .setExecutionThreadPool(commandExecutor)
            .configureDefaultHelpCommand { enabled = false }
            .build()

        shardManager = ExtendedShardManager.create(credentials.token) {
            addEventListeners(eventWaiter, BotListener(), VoiceListener(), commandClient)
        }

        commandClient.commands.register("gg.octave.bot.commands")

        if (configuration.nodeNumber == 0) {
            statsPoster.postEvery(30, TimeUnit.MINUTES)
        }

        NodeInfoPoster(configuration.nodeNumber).postEvery(5, TimeUnit.SECONDS)
    }
}
