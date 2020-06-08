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

import me.devoxin.flight.api.CommandClientBuilder
import me.devoxin.flight.api.entities.Invite
import me.devoxin.flight.internal.parsers.*

fun CommandClientBuilder.registerAlmostAllParsers(): CommandClientBuilder {
    val booleanParser = BooleanParser()
    addCustomParser(Boolean::class.java, booleanParser)
    addCustomParser(java.lang.Boolean::class.java, booleanParser)

    val doubleParser = DoubleParser()
    addCustomParser(Double::class.java, doubleParser)
    addCustomParser(java.lang.Double::class.java, doubleParser)

    val floatParser = FloatParser()
    addCustomParser(Float::class.java, floatParser)
    addCustomParser(java.lang.Float::class.java, floatParser)

    val intParser = IntParser()
    addCustomParser(Int::class.java, intParser)
    addCustomParser(java.lang.Integer::class.java, intParser)

    val longParser = LongParser()
    addCustomParser(Long::class.java, longParser)
    addCustomParser(java.lang.Long::class.java, longParser)

    // JDA entities
    val inviteParser = InviteParser()
    addCustomParser(Invite::class.java, inviteParser)
    addCustomParser(net.dv8tion.jda.api.entities.Invite::class.java, inviteParser)

    //addCustomParser(MemberParser())
    addCustomParser(UserParser())
    addCustomParser(RoleParser())
    addCustomParser(TextChannelParser())
    addCustomParser(VoiceChannelParser())

    // Custom entities
    addCustomParser(EmojiParser())
    addCustomParser(StringParser())
    addCustomParser(SnowflakeParser())
    addCustomParser(UrlParser())

    return this
}
