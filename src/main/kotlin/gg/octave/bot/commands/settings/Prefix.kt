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

package gg.octave.bot.commands.settings

import gg.octave.bot.Launcher
import gg.octave.bot.utils.extensions.data
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import net.dv8tion.jda.api.Permission

class Prefix : Cog {
    @Command(description = "Sets the prefix for the server.", userPermissions = [Permission.MANAGE_SERVER])
    fun prefix(ctx: Context, prefix: String?) {
        val data = ctx.data

        if (prefix == null) {
            val botPrefix = data.command.prefix ?: Launcher.configuration.prefix
            return ctx.send("My prefix is `$botPrefix`")
        }

        if (prefix matches mention) {
            return ctx.send("The prefix cannot be set to a mention.")
        }

        if (data.command.prefix == prefix) {
            return ctx.send("The prefix is already set to `$prefix`.")
        }

        data.command.prefix = prefix
        data.save()

        ctx.send("Prefix has been set to `$prefix`.")
    }

    @SubCommand(aliases = ["clear"], description = "Resets the bot's prefix back to default.")
    fun reset(ctx: Context) {
        ctx.data.let {
            it.command.prefix = null
            it.save()
        }

        return ctx.send("The prefix has been reset to `${Launcher.configuration.prefix}`.")
    }

    companion object {
        private val mention = Regex("<@!?(\\d+)>|<#(\\d+)>|<@&(\\d+)>")
    }
}
