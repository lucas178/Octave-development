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

package gg.octave.bot.utils

import net.dv8tion.jda.api.requests.GatewayIntent

object IntentHelper {
    private val disabledIntents = listOf(
        GatewayIntent.GUILD_INVITES,
        GatewayIntent.GUILD_BANS,
        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.DIRECT_MESSAGE_TYPING,
        GatewayIntent.GUILD_EMOJIS,
        GatewayIntent.GUILD_MEMBERS,
        //GatewayIntent.GUILD_MESSAGE_REACTIONS,
        GatewayIntent.GUILD_MESSAGE_TYPING,
        GatewayIntent.GUILD_PRESENCES
    )

    // Basically everything except GUILD_MESSAGES, GUILD_VOICE_STATES, and GUILD_MESSAGE_REACTIONS.
    // Not actually sure if we need GUILD_MESSAGE_REACTIONS but I've left it in for the sake of vote-* commands.

    val allIntents = GatewayIntent.ALL_INTENTS
    val disabledIntentsInt = GatewayIntent.getRaw(disabledIntents)
    val enabledIntentsInt = allIntents and disabledIntentsInt.inv()
    val enabledIntents = GatewayIntent.getIntents(enabledIntentsInt)
}
