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

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CompletableFuture

object RequestUtil {
    private val httpClient = OkHttpClient()

    /* Media Types */
    val APPLICATION_JSON: MediaType = MediaType.get("application/json")

    fun request(options: Request.Builder.() -> Unit): PendingRequest {
        val request = Request.Builder()
            .apply(options)
            .build()

        return PendingRequest(request)
    }

    fun jsonObject(options: Request.Builder.() -> Unit) = jsonObject(options, false)

    fun jsonObject(options: Request.Builder.() -> Unit, checkStatus: Boolean): CompletableFuture<JSONObject> {
        return request(options).submit()
            .thenApply {
                if (checkStatus && !it.isSuccessful) {
                    throw IllegalStateException("Received invalid status code: ${it.code()}")
                }
                it
            }
            .thenApply { it.body()?.string() ?: throw IllegalStateException("ResponseBody was null!") }
            .thenApply(::JSONObject)
    }

    class PendingRequest(private val req: Request) {
        fun submit(): CompletableFuture<Response> {
            val future = CompletableFuture<Response>()

            httpClient.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    future.completeExceptionally(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    future.complete(response)
                }
            })

            return future
        }

//        suspend fun await(): RequestBody {
//            return submit().await()
//        }
        // Requires: kotlinx.coroutines
    }
}
