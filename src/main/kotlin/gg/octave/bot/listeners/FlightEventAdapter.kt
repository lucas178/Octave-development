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
import gg.octave.bot.db.guilds.GuildData
import gg.octave.bot.entities.framework.DJ
import gg.octave.bot.entities.framework.DonorOnly
import gg.octave.bot.utils.extensions.*
import gg.octave.bot.utils.getDisplayValue
import gg.octave.bot.utils.hasAnyRoleId
import gg.octave.bot.utils.hasAnyRoleNamed
import io.sentry.Sentry
import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.SubCommandFunction
import me.devoxin.flight.api.exceptions.BadArgument
import me.devoxin.flight.api.hooks.DefaultCommandEventAdapter
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import kotlin.reflect.full.hasAnnotation

class FlightEventAdapter : DefaultCommandEventAdapter() {
    @ExperimentalStdlibApi
    override fun onBadArgument(ctx: Context, command: CommandFunction, error: BadArgument) {
        if (error.argument.type.isEnum) {
            val options = error.argument.type.enumConstants.map { it.toString().toLowerCase() }
            return ctx.send {
                setColor(0x9570D3)
                setTitle("Help | ${command.name}")
                setDescription("You specified an invalid argument for `${error.argument.name}`.")
                addField("Valid Options", options.joinToString("`\n- `", prefix = "- `", postfix = "`"), true)
            }
        }

        val executed = ctx.invokedCommand
        val arguments = executed.arguments
        val commandLayout = buildString {
            append(ctx.trigger)
            append(command.name)

            if (executed is SubCommandFunction) {
                append(" ")
                append(executed.name)
            }
        }

        val syntax = buildString {
            append(commandLayout)
            append(" ")
            for (argument in arguments) {
                append(argument.name)
                append(" ")
            }
        }.trim()

        ctx.send {
            setColor(0x9570D3)
            setTitle("Help | ${command.name}")
            setDescription("You specified an invalid argument for `${error.argument.name}`")
            addField("Syntax", "`$syntax`", false)
            addField("Example Usage(s)", executed.generateExampleUsage(commandLayout), false)
            addField("Still Confused?", "Head over to our [#support channel](https://discord.gg/musicbot)", false)
        }
    }

    override fun onParseError(ctx: Context, command: CommandFunction, error: Throwable) {
        error.printStackTrace()
        Sentry.capture(error)
        ctx.send("An error was encountered while parsing the arguments for this command.\n" +
            "The error has been logged. We apologise for any inconvenience caused!")
    }

    override fun onCommandError(ctx: Context, command: CommandFunction, error: Throwable) {
        error.printStackTrace()
        Sentry.capture(error)
        ctx.send("The command encountered an error, which has been logged.\n" +
            "We apologise for any inconvenience caused!")
    }

    override fun onCommandCooldown(ctx: Context, command: CommandFunction, cooldown: Long) {
        ctx.send("This command is on cool-down. Wait ${getDisplayValue(cooldown, true)}.")
    }

    @ExperimentalStdlibApi
    override fun onCommandPreInvoke(ctx: Context, command: CommandFunction): Boolean {
        if (ctx.guild == null) {
            return false
        }

        if (!ctx.selfMember!!.hasPermission(ctx.textChannel!!, Permission.MESSAGE_EMBED_LINKS)) {
            ctx.send("This bot requires the permission Embed Links to work (else the bot can't show embeds).")
            return false
        }

        val data = ctx.data
        if (data.command.isInvokeDelete && ctx.selfMember!!.hasPermission(Permission.MESSAGE_MANAGE)) {
            ctx.message.delete().queue() // delete the message that triggered the command
        }

        if (command.method.hasAnnotation<DonorOnly>() && !ctx.isGuildPremium) {
            ctx.send("This command is only for premium servers. " +
                "If you want to pledge to unlock this command, head to <https://www.patreon.com/octavebot>. Thank you for your support!")
            //Lazily reset volume to 100. This will only run if the guild isn't premium, command is donor-only and the volume isn't already 100.
            if (data.music.volume != 100) {
                data.music.volume = 100
                data.save()
            }

            return false
        }

        if (ctx.member!!.hasPermission(Permission.ADMINISTRATOR)
            || ctx.member!!.hasPermission(Permission.MANAGE_SERVER)
            || ctx.author.idLong in ctx.config.admins) {
            return true
        }

        //Don't send a message if it's just ignored.
        if (isIgnored(ctx, data, ctx.member!!)) {
            return false
        }

        if (command.category == "Music" || command.category == "Dj" || command.category == "Search") { // CheckVoiceState
            if (ctx.member!!.voiceState?.channel == null) {
                ctx.send("You're not in a voice channel.")
                return false
            }

            if (ctx.member!!.voiceState?.channel == ctx.guild!!.afkChannel) {
                ctx.send("You can't play music in the AFK channel.")
                return false
            }

            if (data.music.channels.isNotEmpty() && ctx.member!!.voiceState?.channel?.id !in data.music.channels) {
                val channels = data.music.channels
                    .mapNotNull { ctx.guild!!.getVoiceChannelById(it)?.name }
                    .joinToString(", ")

                ctx.send("Music can only be played in: `$channels`, since this server has set it/them as a designated voice channel.")
                return false
            }
        }

        if (command.method.hasAnnotation<DJ>() || data.command.isDjOnlyMode) {
            return data.music.isDisableDj || isDJ(ctx)
        }

        return true
    }

    private fun isIgnored(ctx: Context, data: GuildData, member: Member): Boolean {
        return member.user.id in data.ignored.users || ctx.textChannel!!.id in data.ignored.channels
            || data.ignored.roles.any { id -> member.roles.any { it.id == id } }
    }

    override fun onCommandPostInvoke(ctx: Context, command: CommandFunction, failed: Boolean) {
        Launcher.datadog.incrementCounter("bot.commands_ran")
    }

    override fun onBotMissingPermissions(ctx: Context, command: CommandFunction, permissions: List<Permission>) {
        val formatted = permissions.joinToString("`\n`", prefix = "`", postfix = "`", transform = Permission::getName)

        if (Permission.MESSAGE_EMBED_LINKS in permissions) {
            return ctx.send("__Missing Permissions__\n\nThis command requires the following permissions:\n$formatted")
        }
        // Perhaps the above should be in `preInvoke` with a message when perm is missing?
        // I'm pretty sure we don't label embed_links as a requirement for all commands anyway.

        ctx.send {
            setColor(0x9570D3)
            setTitle("Missing Permissions")
            setDescription("I need the following permissions:\n$formatted")
        }
    }

    override fun onUserMissingPermissions(ctx: Context, command: CommandFunction, permissions: List<Permission>) {
        val formatted = permissions.joinToString("`\n`", prefix = "`", postfix = "`", transform = Permission::getName)

        ctx.send {
            setColor(0x9570D3)
            setTitle("Missing Permissions")
            setDescription("You need the following permissions:\n$formatted")
        }
    }

    companion object {
        fun isDJ(ctx: Context, send: Boolean = true): Boolean {
            val data = ctx.data
            val isAlone = ctx.guild!!.audioManager.connectedChannel.let { it != null && it.members.count { m -> !m.user.isBot } == 1 }
            val djRole = data.command.djRole
            val djRolePresent = djRole?.let(ctx.member!!::hasAnyRoleId)
                ?: data.music.djRoles.any(ctx.member!!::hasAnyRoleId)

            val admin = ctx.member!!.hasPermission(Permission.MANAGE_SERVER)

            if (ctx.member!!.hasAnyRoleNamed("DJ") || djRolePresent || isAlone || admin) {
                return true
            }

            val append = if (!djRolePresent) "" else buildString {
                val roleName = djRole?.let(ctx.guild!!::getRoleById)?.name
                val moreRoles = data.music.djRoles.mapNotNull(ctx.guild!!::getRoleById)
                    .joinToString("`, `", prefix = "`", postfix = "`", transform = Role::getName)

                if (!roleName.isNullOrEmpty()) {
                    append(", or a role called $roleName")
                }

                if (moreRoles.isNotEmpty()) {
                    appendln(", or any of the following roles:")
                    append(moreRoles)
                }
            }

            if (send) {
                ctx.send(
                    "You need a role called DJ$append.\n" +
                        "This can be bypassed if you're an admin (either Manage Server or Administrator) or you're alone with the bot."
                )
            }
            return false
        }
    }
}
