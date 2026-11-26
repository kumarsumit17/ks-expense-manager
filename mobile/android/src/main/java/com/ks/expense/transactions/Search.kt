package com.ks.expense.transactions

/** What the search box and filters ask for. */
data class SearchFilter(
    val text: String = "",
    val categoryIds: Set<Long> = emptySet(),
    val fromMillis: Long? = null,
    val toMillis: Long? = null,
) {
    val isEmpty: Boolean get() = text.isBlank() && categoryIds.isEmpty() && fromMillis == null && toMillis == null
}

object Search {
    /** Rows shown at most; the newest come first. */
    const val LIMIT = 500

    /** A LIKE pattern that finds [text] anywhere, with `%`, `_` and `\` matched literally; null for blank text. */
    fun likePattern(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val escaped = buildString {
            trimmed.forEach { c ->
                if (c == '%' || c == '_' || c == '\\') append('\\')
                append(c)
            }
        }
        return "%$escaped%"
    }

    /** The text read as an amount of [currency] ("450", "1,23,456.50"), or null if it is not one. */
    fun amount(text: String, currency: String): Long? = Money.parse(text, currency)

    /** Whether a category's shown name matches the text (case-insensitive, anywhere). */
    fun nameMatches(name: String, text: String): Boolean {
        val t = text.trim()
        return t.isNotEmpty() && name.contains(t, ignoreCase = true)
    }
}
