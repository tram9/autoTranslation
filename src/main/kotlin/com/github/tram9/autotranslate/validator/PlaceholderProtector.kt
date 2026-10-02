package com.github.tram9.autotranslate.validator

object PlaceholderProtector {
    private val formatRegex = Regex("""%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?[a-zA-Z%]""")

    data class ProtectedText(
        val text: String,
        val replacements: Map<String, String>,
    ) {
        fun restore(translated: String): String {
            var restored = translated
            replacements.forEach { (token, original) -> restored = restored.replace(token, original) }
            return restored
        }
    }

    fun protect(text: String): ProtectedText {
        val replacements = linkedMapOf<String, String>()
        var index = 0
        val protected = formatRegex.replace(text) { match ->
            val token = "ZXQAUTOTRANSLATE${index++}QXZ"
            replacements[token] = match.value
            token
        }
        return ProtectedText(protected, replacements)
    }

    fun placeholders(text: String): List<String> = formatRegex.findAll(text).map { it.value }.toList()

    fun hasSamePlaceholders(source: String, translated: String): Boolean =
        placeholders(source).sorted() == placeholders(translated).sorted()
}
