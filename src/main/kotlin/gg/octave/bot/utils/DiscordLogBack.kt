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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import club.minnced.discord.webhook.WebhookClient

class DiscordLogBack : AppenderBase<ILoggingEvent>() {
    private var patternLayout: PatternLayout? = null

    override fun append(event: ILoggingEvent) {
        if (client == null) {
            return
        }

        if (!event.level.isGreaterOrEqual(Level.INFO)) {
            return
        }

        var content = patternLayout!!.doLayout(event)

        if (!content.contains("UnknownHostException")) //Spams the shit out of console, not needed
            if (content.length > 2000) {
                val sb = StringBuilder(":warning: Received a message but it was too long. ")
                val url = Utils.hasteBin(content)
                sb.append(url ?: "Error while posting to HasteBin.")
                content = sb.toString()
            }

        client?.send(content)
    }

    override fun start() {
        patternLayout = PatternLayout()
        patternLayout!!.context = getContext()
        patternLayout!!.pattern = "`%d{HH:mm:ss}` `%t/%level` `%logger{0}` %msg"
        patternLayout!!.start()

        super.start()
    }

    companion object {
        private var client: WebhookClient? = null

        fun enable(webhookClient: WebhookClient) {
            client = webhookClient
        }
    }
}
