package com.github.tram9.autotranslate.parser

import com.github.tram9.autotranslate.model.StringResource
import com.intellij.openapi.vfs.VirtualFile

object StringsXmlParser {
    private val stringRegex = Regex(
        """<string\b([^>]*)\bname\s*=\s*["']([^"']+)["']([^>]*)>(.*?)</string>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parse(file: VirtualFile): LinkedHashMap<String, StringResource> =
        parse(String(file.contentsToByteArray(), Charsets.UTF_8))

    internal fun parse(content: String): LinkedHashMap<String, StringResource> {
        val result = linkedMapOf<String, StringResource>()
        for (match in stringRegex.findAll(content)) {
            val attributes = match.groupValues[1] + match.groupValues[3]
            if (Regex("""translatable\s*=\s*["']false["']""", RegexOption.IGNORE_CASE).containsMatchIn(attributes)) continue
            val name = match.groupValues[2]
            result[name] = StringResource(name, match.groupValues[4])
        }
        return result
    }
}
