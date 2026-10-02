package com.github.tram9.autotranslate.translator

interface TranslationProvider {
    fun translate(text: String, sourceLanguage: String, targetLanguage: String): String
}
