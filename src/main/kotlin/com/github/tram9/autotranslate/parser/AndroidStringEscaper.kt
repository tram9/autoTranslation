package com.github.tram9.autotranslate.parser

object AndroidStringEscaper {
    fun prepareForTranslation(rawText: String): String =
        rawText.replace("<![CDATA[", "")
            .replace("]]>", "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", """)
            .replace("&apos;", "'")
            .replace("\\'", "'")
            .replace("\\"", """)
            .replace("\\n", "\n")

    fun escapeForXml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("'", "\\'")
            .replace(""", "\\"")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
}
