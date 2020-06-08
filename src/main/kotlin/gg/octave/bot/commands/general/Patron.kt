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

package gg.octave.bot.commands.general

import gg.octave.bot.Launcher
import gg.octave.bot.db.premium.PremiumGuild
import gg.octave.bot.db.premium.PremiumUser
import gg.octave.bot.utils.extensions.DEFAULT_SUBCOMMAND
import gg.octave.bot.utils.extensions.db
import gg.octave.bot.utils.extensions.shardManager
import io.sentry.Sentry
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.max

class Patron : Cog {
    private val ignore = setOf(
        ErrorResponse.MISSING_ACCESS,
        ErrorResponse.MISSING_PERMISSIONS,
        ErrorResponse.CANNOT_SEND_TO_USER
    )

    @Command(aliases = ["patreon"], description = "Link/manage Patron membership.")
    fun patron(ctx: Context) = DEFAULT_SUBCOMMAND(ctx)

    @SubCommand(description = "Checks premium status of you or another user.")
    fun status(ctx: Context, user: Long?) {
        val userId = user ?: ctx.author.idLong

        if (!ctx.db.hasPremiumUser(userId.toString())) {
            return ctx.send("There is no entry for that user (not premium).")
        }

        val premiumUser = ctx.db.getPremiumUser(userId.toString())
        val totalServers = premiumUser.totalPremiumGuildQuota
        val premiumServers = totalServers - premiumUser.remainingPremiumGuildQuota

        ctx.send {
            setColor(0x9570D3)
            setTitle("Premium Status")
            setDescription("Status for <@$userId>")
            addField("Is Premium?", if (!premiumUser.isPremium) "No" else "Yes", true)
            addField("Pledge Amount", String.format("$%1$,.2f", premiumUser.pledgeAmount), true)
            addField("Premium Servers", "$premiumServers/$totalServers", true)
        }
    }

    @SubCommand(description = "Link your Discord account to Patreon.")
    fun link(ctx: Context) {
        ctx.messageChannel.sendMessage("Looking for your pledge, this may take a minute...")
            .submit()
            .thenCompose { Launcher.patreon.fetchPledges() }
            .thenAccept { pledges ->
                val pledge = pledges.firstOrNull { it.discordId != null && it.discordId == ctx.author.idLong }
                    ?: return@thenAccept ctx.send {
                        setColor(0x9570D3)
                        setDescription(
                            "Couldn't find your pledge.\n" +
                                "[Re-link your account](https://support.patreon.com/hc/en-us/articles/212052266-Get-my-Discord-role) and try again.\n" +
                                "If this still doesn't work, [join the Discord server](https://discord.gg/musicbot) for support."
                        )
                    }

                if (pledge.isDeclined || pledge.pledgeCents <= 0) {
                    return@thenAccept ctx.send("It looks like your pledge was declined, or your pledge is too low!\n" +
                        "We are unable to link your account until this is resolved.")
                }

                val pledgeAmount = pledge.pledgeCents.toDouble() / 100

                val user = PremiumUser(ctx.author.id)
                    .setPledgeAmount(pledgeAmount)

                user.save()

                ctx.send {
                    setColor(0x9570D3)
                    setTitle("Thank you, ${ctx.author.name}!")
                    setDescription("Thanks for pledging $${String.format("%1$,.2f", pledgeAmount)}!\n" +
                        "You can have up to **${user.totalPremiumGuildQuota}** premium servers, which can be " +
                        "added and removed with the `${ctx.trigger}patron servers` command.")
                    setThumbnail("https://cdn.discordapp.com/attachments/690754397486973021/695724606115545098/pledge-lemon-enhancing-polish-orange-clean.png")
                    setFooter("❤")
                }
            }
            .exceptionally {
                if (it is ErrorResponseException && (it.isServerError || it.errorResponse in ignore)) {
                    return@exceptionally null
                }

                Sentry.capture(it)
                ctx.send("An unknown error occurred while looking for your pledge.\n`${it.localizedMessage}`")
                return@exceptionally null
            }
    }

    @SubCommand(description = "Manage your premium servers.")
    fun servers(ctx: Context, @Greedy action: String?) {
        val remainingServers = ctx.db.getPremiumUser(ctx.author.id).remainingPremiumGuildQuota

        when (action?.toLowerCase()) {
            "add" -> serversAdd(ctx)
            "remove" -> serversRemove(ctx, action.split(" +".toRegex()).drop(1))
            else -> {
                val premGuilds = ctx.db.getPremiumGuilds(ctx.author.id)?.toList()
                    ?: emptyList()

                val output = buildString {
                    appendln("`${ctx.trigger}patron servers <add/remove>`")
                    appendln("```")
                    appendln("%-20s | %-21s | %-5s".format("Server Name", "Server ID", "Added"))

                    for (g in premGuilds) {
                        val guildName = ctx.shardManager.getGuildById(g.id)?.let { truncate(it.name) }
                            ?: "Unknown Server"
                        val guildId = g.id
                        val guildAdded = g.daysSinceAdded
                        appendln("%-20s | %-21s | %d days ago".format(guildName, guildId, guildAdded))
                    }

                    appendln()
                    append("You can have ${max(remainingServers, 0)} more premium server${plural(remainingServers)}.")
                    append("```")
                }

                ctx.send(output)
            }
        }
    }

    fun serversAdd(ctx: Context) {
        val premiumGuild = ctx.db.getPremiumGuild(ctx.guild!!.id)

        if (premiumGuild != null) {
            return ctx.send("This server already has premium status, redeemed by <@${premiumGuild.redeemerId}>")
        }

        val profile = ctx.db.getPremiumUser(ctx.author.id)
        val remaining = profile.remainingPremiumGuildQuota

        if (remaining <= 0) {
            return ctx.send("You have no premium server slots remaining.")
        }

        ctx.messageChannel.sendMessage("Do you want to register **${ctx.guild!!.name}** as one of your premium servers? (`y`/`n`)")
            .submit()
            .thenCompose { prompt(ctx) }
            .thenAccept {
                if (!it) {
                    ctx.send("OK. **${ctx.guild!!.name}** will not be registered as a premium server.")
                    return@thenAccept
                }

                PremiumGuild(ctx.guild!!.id)
                    .setAdded(Instant.now().toEpochMilli())
                    .setRedeemer(ctx.author.id)
                    .save()

                ctx.send("Added **${ctx.guild!!.name}** as a premium server. You have **${remaining - 1}** premium server slots left.")
            }
            .exceptionally {
                ctx.send(
                    "An unknown error has occurred while removing the server's premium status.\n" +
                        "`${it.localizedMessage}`\n" +
                        "Please report this to the developers."
                )
                return@exceptionally null
            }
    }

    fun serversRemove(ctx: Context, args: List<String>) {
        if (args.isNotEmpty() && args[0].toLongOrNull() == null) {
            return ctx.send(
                "Invalid server ID provided. You can omit the server ID to remove the current server.\n" +
                    "Alternatively, you can list your premium servers with `${ctx.trigger}patron servers`, " +
                    "and then copy a server ID from there."
            )
        }

        val guildId = args.firstOrNull() ?: ctx.guild!!.id
        val guild = ctx.shardManager.getGuildById(guildId)?.name ?: "Unknown Server"
        val hasDevOverride = ctx.commandClient.ownerIds.contains(ctx.author.idLong)

        val premiumGuild = ctx.db.getPremiumGuild(guildId)
            ?: return ctx.send("The server does not have premium status.")

        if (premiumGuild.redeemerId != ctx.author.id && !hasDevOverride) {
            return ctx.send("You may not remove premium status for the server.")
        }

        if (premiumGuild.daysSinceAdded < minDaysBeforeRemoval && !hasDevOverride) {
            val remainingDays = minDaysBeforeRemoval - premiumGuild.daysSinceAdded
            return ctx.send(
                "You must wait $remainingDays more days before removing the premium status for the server.\n" +
                    "If there is a valid reason for early removal, please contact the developers."
            )
        }

        ctx.messageChannel.sendMessage("Do you want to remove **$guild**'s premium status? (`y`/`n`)")
            .submit()
            .thenCompose { prompt(ctx) }
            .thenAccept {
                if (!it) {
                    ctx.send("OK. **$guild** will not be removed as a premium server.")
                    return@thenAccept
                }

                premiumGuild.delete()
                ctx.send("Removed **$guild** as a premium server.")
            }
            .exceptionally {
                ctx.send(
                    "An unknown error has occurred while removing the server's premium status.\n" +
                        "`${it.localizedMessage}`\n" +
                        "Please report this to the developers."
                )
                return@exceptionally null
            }
    }

    fun prompt(ctx: Context): CompletableFuture<Boolean> {
        return ctx.commandClient.waitFor<MessageReceivedEvent>(
            { it.author.idLong == ctx.author.idLong },
            TimeUnit.SECONDS.toMillis(15)
        ).thenApply { it.message.contentRaw.toLowerCase() in answers }
    }

    private fun plural(a: Int): String {
        return if (a == 1) "" else "s"
    }

    private fun truncate(s: String, l: Int = 20): String {
        return s.takeIf { it.length <= l }
            ?: s.take(l - 3) + "..."
    }

    companion object {
        private const val minDaysBeforeRemoval = 28
        private val answers = setOf("y", "yes", "yeah", "ok", "true", "1")
    }
}
