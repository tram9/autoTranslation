package com.github.tram9.autotranslate.toolWindow

import com.github.tram9.autotranslate.detector.AndroidLocaleDetector
import com.github.tram9.autotranslate.parser.AndroidStringEscaper
import com.github.tram9.autotranslate.parser.StringsXmlParser
import com.github.tram9.autotranslate.parser.StringsXmlWriter
import com.github.tram9.autotranslate.translator.GoogleTranslateProvider
import com.github.tram9.autotranslate.translator.TranslationProvider
import com.github.tram9.autotranslate.validator.PlaceholderProtector
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class AutoTranslateAction : AnAction() {

    @Volatile
    private var isProcessing = false

    private val translationProvider: TranslationProvider = GoogleTranslateProvider()

    private enum class TranslationMode {
        MISSING_ONLY,
        REFRESH_ALL,
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (isProcessing) return

        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: e.getData(CommonDataKeys.PSI_FILE)?.virtualFile
            ?: return

        val fileName = virtualFile.name
        if (fileName != "strings.xml" && fileName != "string.xml") {
            Messages.showWarningDialog(
                project,
                "Right-click the base strings.xml or string.xml file inside res/values.",
                "Auto Translate Strings",
            )
            return
        }

        val valuesDir = virtualFile.parent ?: return
        if (valuesDir.name != "values") {
            Messages.showWarningDialog(
                project,
                "Please run Auto Translate from the base res/values/$fileName file.",
                "Auto Translate Strings",
            )
            return
        }

        val resDir = valuesDir.parent ?: return
        val targetLocales = AndroidLocaleDetector.detect(resDir)
        if (targetLocales.isEmpty()) {
            Messages.showWarningDialog(
                project,
                "No locale folders were found. Add folders such as values-vi, values-fr, values-th or values-pt-rBR first.",
                "No Languages Detected",
            )
            return
        }

        val mode = chooseTranslationMode(project) ?: return
        isProcessing = true

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Auto Translate Strings", true) {
                override fun run(indicator: ProgressIndicator) {
                    runTranslation(project, virtualFile, resDir, fileName, targetLocales, mode, indicator)
                }

                override fun onFinished() {
                    isProcessing = false
                }
            },
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = true
        e.presentation.isEnabled = !isProcessing
    }

    private fun runTranslation(
        project: Project,
        sourceFile: VirtualFile,
        resDir: VirtualFile,
        fileName: String,
        targetLocales: List<com.github.tram9.autotranslate.model.AndroidLocale>,
        mode: TranslationMode,
        indicator: ProgressIndicator,
    ) {
        try {
            indicator.text = "Reading base strings..."
            val sourceStrings = StringsXmlParser.parse(sourceFile)
            if (sourceStrings.isEmpty()) {
                showWarning(project, "The base $fileName contains no translatable <string> resources.")
                return
            }

            var translatedCount = 0
            var failedCount = 0

            targetLocales.forEachIndexed { localeIndex, locale ->
                if (indicator.isCanceled) return@forEachIndexed

                indicator.text = "Translating ${locale.folderName} (${localeIndex + 1}/${targetLocales.size})"
                indicator.fraction = localeIndex.toDouble() / targetLocales.size.coerceAtLeast(1)

                val targetFile = locale.folder.findChild(fileName)
                val targetStrings = targetFile?.let { StringsXmlParser.parse(it) } ?: linkedMapOf()
                val existingContent = targetFile?.let {
                    String(it.contentsToByteArray(), Charsets.UTF_8)
                } ?: StringsXmlWriter.emptyResourcesFile()

                val keysToTranslate = when (mode) {
                    TranslationMode.MISSING_ONLY -> sourceStrings.keys.filterNot(targetStrings::containsKey)
                    TranslationMode.REFRESH_ALL -> sourceStrings.keys.toList()
                }

                if (keysToTranslate.isEmpty()) return@forEachIndexed

                val translatedValues = linkedMapOf<String, String>()

                for (key in keysToTranslate) {
                    if (indicator.isCanceled) break
                    indicator.text2 = key

                    val source = sourceStrings[key]?.rawValue ?: continue
                    try {
                        val plainSource = AndroidStringEscaper.prepareForTranslation(source)
                        val protected = PlaceholderProtector.protect(plainSource)
                        val translatedProtected = translationProvider.translate(
                            text = protected.text,
                            sourceLanguage = "en",
                            targetLanguage = locale.languageTag,
                        )
                        val translated = protected.restore(translatedProtected)

                        if (!PlaceholderProtector.hasSamePlaceholders(plainSource, translated)) {
                            throw IllegalStateException("Placeholder mismatch for key '$key'")
                        }

                        translatedValues[key] = AndroidStringEscaper.escapeForXml(translated)
                        translatedCount++
                        Thread.sleep(250)
                    } catch (error: Exception) {
                        failedCount++
                        error.printStackTrace()
                    }
                }

                if (translatedValues.isNotEmpty() && !indicator.isCanceled) {
                    writeTranslations(
                        project = project,
                        resDir = resDir,
                        folderName = locale.folderName,
                        fileName = fileName,
                        existingContent = existingContent,
                        existingKeys = targetStrings.keys,
                        translatedValues = translatedValues,
                    )
                }
            }

            if (!indicator.isCanceled) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showInfoMessage(
                        project,
                        "Translated $translatedCount strings across ${targetLocales.size} detected locale folders." +
                            if (failedCount > 0) " Failed: $failedCount." else "",
                        "Translation Complete",
                    )
                }
            }
        } catch (error: Exception) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(project, error.message ?: "Unknown translation error", "Auto Translate Error")
            }
        }
    }

    private fun writeTranslations(
        project: Project,
        resDir: VirtualFile,
        folderName: String,
        fileName: String,
        existingContent: String,
        existingKeys: Set<String>,
        translatedValues: Map<String, String>,
    ) {
        ApplicationManager.getApplication().invokeAndWait {
            WriteCommandAction.runWriteCommandAction(project) {
                val dir = resDir.findChild(folderName) ?: resDir.createChildDirectory(this, folderName)
                val file = dir.findChild(fileName) ?: dir.createChildData(this, fileName)

                var content = existingContent
                translatedValues.forEach { (key, value) ->
                    content = if (key in existingKeys) {
                        StringsXmlWriter.replaceStringValue(content, key, value)
                    } else {
                        StringsXmlWriter.appendString(content, key, value)
                    }
                }

                file.setBinaryContent(content.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private fun chooseTranslationMode(project: Project): TranslationMode? {
        val choice = Messages.showDialog(
            project,
            "Detected locale folders automatically. Choose how existing translations should be handled.",
            "Auto Translate Strings",
            arrayOf("Translate missing strings only", "Recheck all strings"),
            0,
            Messages.getQuestionIcon(),
        )

        return when (choice) {
            0 -> TranslationMode.MISSING_ONLY
            1 -> TranslationMode.REFRESH_ALL
            else -> null
        }
    }

    private fun showWarning(project: Project, message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showWarningDialog(project, message, "Auto Translate Strings")
        }
    }
}
