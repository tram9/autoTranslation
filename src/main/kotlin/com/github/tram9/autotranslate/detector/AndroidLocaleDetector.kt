package com.github.tram9.autotranslate.detector

import com.github.tram9.autotranslate.model.AndroidLocale
import com.intellij.openapi.vfs.VirtualFile

object AndroidLocaleDetector {
    private val nonLanguageQualifiers = setOf(
        "car", "desk", "television", "watch", "vrheadset",
        "land", "port", "night", "notnight", "ldrtl", "ldltr",
        "small", "normal", "large", "xlarge",
    )

    fun detect(resDir: VirtualFile): List<AndroidLocale> =
        resDir.children.asSequence()
            .filter { it.isDirectory && it.name.startsWith("values-") }
            .mapNotNull { folder ->
                val tag = localeFromValuesFolder(folder.name) ?: return@mapNotNull null
                AndroidLocale(folder, folder.name, tag)
            }
            .distinctBy { it.folderName }
            .sortedBy { it.folderName }
            .toList()

    internal fun localeFromValuesFolder(folderName: String): String? {
        if (!folderName.startsWith("values-")) return null
        val qualifier = folderName.removePrefix("values-")
        if (qualifier.isBlank()) return null

        if (qualifier.startsWith("b+")) {
            val bcp47 = qualifier.substringBefore('-').removePrefix("b+").replace('+', '-')
            return normalizeLegacyLanguage(bcp47.takeIf { it.isNotBlank() } ?: return null)
        }

        val parts = qualifier.split('-')
        val language = parts.firstOrNull() ?: return null
        if (language in nonLanguageQualifiers || !language.matches(Regex("[a-z]{2,3}"))) return null

        val region = parts.firstOrNull { it.matches(Regex("r[A-Z]{2}|r\\d{3}")) }?.removePrefix("r")
        val tag = if (region != null) "$language-$region" else language
        return normalizeLegacyLanguage(tag)
    }

    private fun normalizeLegacyLanguage(tag: String): String = when {
        tag == "in" -> "id"
        tag.startsWith("in-") -> "id" + tag.removePrefix("in")
        tag == "iw" -> "he"
        tag.startsWith("iw-") -> "he" + tag.removePrefix("iw")
        tag == "ji" -> "yi"
        tag.startsWith("ji-") -> "yi" + tag.removePrefix("ji")
        else -> tag
    }
}
