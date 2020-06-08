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

package gg.octave.bot.apis.statsposter.websites

import gg.octave.bot.utils.RequestUtil
import okhttp3.RequestBody
import okhttp3.Response
import java.util.concurrent.CompletableFuture

abstract class Website(
    val name: String,
    private val postUrl: String,
    private val countHeader: String,
    private val authorization: String
) {

    fun canPost(): Boolean {
        return postUrl.isNotEmpty() && countHeader.isNotEmpty() && authorization.isNotEmpty()
    }

    fun update(count: Long): CompletableFuture<Response> {
        return RequestUtil.request {
            url(postUrl)
            post(RequestBody.create(RequestUtil.APPLICATION_JSON, "{\"$countHeader\": $count}"))
            header("Authorization", authorization)
        }.submit().thenApply {
            check(it.isSuccessful) { "Posting to $name failed: ${it.code()} ${it.message()}" }
            it
        }
    }

}
