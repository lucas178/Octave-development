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

import gg.octave.bot.db.PremiumKey
import gg.octave.bot.db.Redeemer
import gg.octave.bot.utils.Utils
import gg.octave.bot.utils.extensions.DEFAULT_SUBCOMMAND
import gg.octave.bot.utils.extensions.db
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import java.util.*

class Keys : Cog {
    @Command(aliases = ["keys"], description = "Manage keys.", developerOnly = true)
    fun key(ctx: Context) = DEFAULT_SUBCOMMAND(ctx)

    @SubCommand(aliases = ["generate"], description = "Generate a premium key.")
    fun gen(ctx: Context, quantity: Int, duration: String, type: PremiumKey.Type = PremiumKey.Type.PREMIUM) {
        val keyDuration = Utils.parseTime(duration).takeIf { it > 0 }
            ?: return ctx.send("wait, that's illegal")

        val keys = buildString {
            (0 until quantity)
                .map { PremiumKey(UUID.randomUUID().toString(), type, keyDuration).apply { save() }.id }
                .forEach { appendln(it) }
        }

        ctx.sendPrivate(keys)
        ctx.send("The keys have been sent to your DMs.")
    }

    @SubCommand(aliases = ["revoke"], description = "Revokes a premium key.")
    fun rev(ctx: Context, @Greedy keys: String) {
        val ids = keys.split(" +|\n".toRegex()).filter { it.isNotEmpty() }

        val result = buildString {
            for (id in ids) {
                appendln("**Key** `$id`")
                val key = ctx.db.getPremiumKey(id)

                if (key == null) {
                    appendln(" NOT FOUND\n")
                    continue
                }

                val redeemer = key.redeemer

                if (redeemer == null) {
                    appendln(" Not redeemed")
                } else {
                    when (redeemer.type) {
                        Redeemer.Type.GUILD -> {
                            val guildData = ctx.db.getGuildData(redeemer.id)

                            if (guildData != null) {
                                guildData.premiumKeys.remove(key.id)
                                guildData.save()
                                appendln(" Revoked the key from guild ID `${guildData.id}`.")
                            } else {
                                appendln(" Guild ID `${redeemer.id}` redeemed the key but no longer exists in the DB.")
                            }
                        }
                        else -> appendln(" Unknown redeemer type")
                    }
                }

                key.delete()
                appendln(" Deleted from database.\n")
            }
        }

        ctx.send(result)
    }
}
