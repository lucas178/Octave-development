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

package gg.octave.bot.utils.extensions

import gg.octave.bot.Launcher
import gg.octave.bot.db.Database
import gg.octave.bot.db.OptionsRegistry
import gg.octave.bot.db.guilds.GuildData
import gg.octave.bot.db.premium.PremiumGuild
import gg.octave.bot.db.premium.PremiumUser
import gg.octave.bot.entities.Configuration
import gg.octave.bot.music.MusicManagerV2
import me.devoxin.flight.api.Context
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.sharding.ShardManager

val Context.db: Database
    get() = Launcher.database

val Context.shardManager: ShardManager
    get() = this.jda.shardManager!!

val Context.data: GuildData
    get() = OptionsRegistry.ofGuild(guild!!)

val Context.premiumGuild: PremiumGuild?
    get() = db.getPremiumGuild(guild!!.id)

val Context.isGuildPremium: Boolean
    get() = premiumGuild != null || data.isPremium

val Context.premiumUser: PremiumUser
    get() = db.getPremiumUser(author.id)

val Context.config: Configuration
    get() = Launcher.configuration

val Context.launcher: Launcher
    get() = Launcher

val Context.manager: MusicManagerV2
    get() = Launcher.players.get(this.guild)

val Context.existingManager: MusicManagerV2?
    get() = Launcher.players.getExisting(this.guild)

val Context.voiceChannel: VoiceChannel?
    get() = member!!.voiceState?.channel

val Context.selfMember: Member?
    get() = guild!!.selfMember
