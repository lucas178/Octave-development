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
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.hooks.EventListener

class VoiceListener : EventListener {
    override fun onEvent(event: GenericEvent) {
        if (event !is GenericGuildVoiceEvent || !Launcher.loaded) {
            return
        }

        when (event) {
            is GuildVoiceJoinEvent -> onGuildVoiceJoin(event)
            is GuildVoiceLeaveEvent -> onGuildVoiceLeave(event)
            is GuildVoiceMoveEvent -> onGuildVoiceMove(event)
        }
    }

    private fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (event.member.user.idLong != event.jda.selfUser.idLong) {
            checkVoiceState(event.guild)
        }
    }

    private fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        when (event.member.user.idLong) {
            event.jda.selfUser.idLong -> Launcher.players.destroy(event.guild.idLong)
            else -> checkVoiceState(event.guild)
        }
    }

    private fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.member.user.idLong != event.jda.selfUser.idLong) {
            return
        }

        val manager = Launcher.players.getExisting(event.guild.idLong)
            ?: return

        if (event.channelJoined.idLong == event.guild.afkChannel?.idLong) {
            return Launcher.players.destroy(event.guild.idLong)
        }

        val options = OptionsRegistry.ofGuild(event.guild)

        if (options.music.channels.isNotEmpty() && event.channelJoined.id !in options.music.channels) {
            manager.announcementChannel
                ?.sendMessage("Cannot join `${event.channelJoined.name}`, it isn't one of the designated music channels.")
                ?.queue()

            return Launcher.players.destroy(event.guild.idLong)
        }

        checkVoiceState(event.guild)
    }

    private fun checkVoiceState(guild: Guild) {
        val manager = Launcher.players.getExisting(guild.idLong)
            ?: return

        if (guild.audioManager.connectedChannel == null) {
            return Launcher.players.destroy(guild.idLong)
        }

        val guildData = OptionsRegistry.ofGuild(guild.id)
        val premiumGuild = Launcher.database.getPremiumGuild(guild.id)
        val avoidLeave = (premiumGuild != null || guildData.isPremium) && guildData.music.isAllDayMusic

        when {
            manager.isAlone && !manager.isLeaveQueued && !avoidLeave -> manager.queueLeave()
            !manager.isAlone && manager.isLeaveQueued -> manager.cancelLeave()
        }
    }
}
