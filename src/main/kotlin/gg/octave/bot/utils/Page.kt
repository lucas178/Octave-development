package gg.octave.bot.utils

import gg.octave.bot.utils.extensions.iterate
import kotlin.math.ceil

class Page(val content: String, val elementCount: Int, val page: Int, val maxPages: Int) {
    companion object {
        fun <T> paginate(elements: List<T>, selectedPage: Int = 1, emptyMessage: String? = null, builder: (Int, T) -> String): Page {
            val content = StringBuilder()

            val maxPages = ceil(elements.size.toDouble() / 10).toInt().coerceAtLeast(1)
            val page = selectedPage.coerceIn(1, maxPages)

            if (elements.isEmpty()) {
                content.append(emptyMessage ?: "`Nothing to display.`")
            } else {
                val begin = (page - 1) * 10
                val end = (begin + 10).coerceAtMost(elements.size)

                for ((i, e) in elements.iterate(begin..end)) {
                    content.append(builder(i, e))
                }
            }

            return Page(content.toString(), elements.size, page, maxPages)
        }
    }
}
