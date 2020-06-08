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

package gg.octave.bot.apis.patreon

import org.json.JSONObject

class PatreonUser(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val pledgeCents: Int,
    val isDeclined: Boolean,
    val discordId: Long?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PatreonUser) return false

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return 31 * id.hashCode()
    }

    companion object {
        fun fromJsonObject(userObj: JSONObject, pledgeObj: JSONObject): PatreonUser {
            val userAttr = userObj.getJSONObject("attributes")
            val pledgeAttr = pledgeObj.getJSONObject("attributes")

            val connections = userAttr.getJSONObject("social_connections")
            val discordId = connections.optJSONObject("discord")
                ?.getLong("user_id")

            return PatreonUser(
                userObj.getInt("id"),
                userAttr.getString("first_name"),
                userAttr.optString("last_name", ""),
                userAttr.getString("email"),
                pledgeAttr.getInt("amount_cents"),
                !pledgeAttr.isNull("declined_since"),
                discordId
            )
        }
    }
}
