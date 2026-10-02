package com.github.tram9.autotranslate.translator

import com.google.gson.JsonParser
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

class GoogleTranslateProvider : TranslationProvider {

    override fun translate(text: String, sourceLanguage: String, targetLanguage: String): String {
        val encodedText = URLEncoder.encode(text, Charsets.UTF_8.name())
        val url = URI.create(
            "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=$sourceLanguage&tl=$targetLanguage&dt=t&q=$encodedText",
        ).toURL()

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Google Translate HTTP ${connection.responseCode}")
            }

            val response = InputStreamReader(connection.inputStream, Charsets.UTF_8).use { it.readText() }
            val jsonArray = JsonParser.parseString(response).asJsonArray
            val sentences = jsonArray[0].asJsonArray

            return buildString {
                for (sentence in sentences) {
                    append(sentence.asJsonArray[0].asString)
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
