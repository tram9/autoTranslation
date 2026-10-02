package com.github.tram9.autotranslate.parser

object StringsXmlWriter {
    fun appendString(content: String, key: String, translatedText: String): String {
        val newString = "    <string name=\"$key\">$translatedText</string>\n"
        val endTagIndex = content.lastIndexOf("</resources>")
        return if (endTagIndex >= 0) {
            content.substring(0, endTagIndex) + newString + content.substring(endTagIndex)
        } else {
            "$content\n$newString</resources>"
        }
    }

    fun replaceStringValue(content: String, key: String, translatedText: String): String {
        val escapedKey = Regex.escape(key)
        val regex = Regex(
            """(<string\b(?=[^>]*\bname\s*=\s*["']$escapedKey["'])[^>]*>).*?(</string>)""",
            RegexOption.DOT_MATCHES_ALL,
        )
        return regex.replaceFirst(content) { it.groupValues[1] + translatedText + it.groupValues[2] }
    }

    fun emptyResourcesFile(): String = """<?xml version="1.0" encoding="utf-8"?>
<resources>
</resources>
"""
}
