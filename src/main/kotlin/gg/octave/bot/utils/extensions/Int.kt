package gg.octave.bot.utils.extensions

fun Int.plural(text: String): String {
    return if (this == 1) "$this $text" else "$this ${text}s"
}
