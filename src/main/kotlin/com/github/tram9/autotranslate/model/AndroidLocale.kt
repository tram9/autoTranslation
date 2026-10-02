package com.github.tram9.autotranslate.model

import com.intellij.openapi.vfs.VirtualFile

data class AndroidLocale(
    val folder: VirtualFile,
    val folderName: String,
    val languageTag: String,
)
