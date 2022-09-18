package com.github.zeldigas.text2confl.cli.config

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.github.zeldigas.text2confl.cli.upload.ChangeDetector
import com.github.zeldigas.text2confl.convert.markdown.MarkdownConfiguration

/**
 * Holder of data that can be put to `.text2confl.yml` configuration file that is located in root directory of directory structure
 */
@JsonNaming(value = PropertyNamingStrategies.KebabCaseStrategy::class)
data class DirectoryConfig(
    val server: String? = null,
    val skipSsl: Boolean = false,
    val space: String? = null,
    val defaultParentId: String? = null,
    val defaultParent: String? = null,
    val removeOrphans: Cleanup = Cleanup.Managed,
    val notifyWatchers: Boolean = true,
    val titlePrefix: String = "",
    val titlePostfix: String = "",
    val editorVersion: EditorVersion? = null,
    val modificationCheck: ChangeDetector = ChangeDetector.HASH,
    val docsLocation: String? = null,
    val addAutogeneratedNote: Boolean? = null,
    val autogeneratedNote: String? = null,
    val markdown: MarkdownParams = MarkdownParams()
)

data class MarkdownParams(
    val anyMacro: Boolean? = null,
    val enabledMacros:List<String> = emptyList(),
    val emoji: Boolean = true
) {
    fun toConfig(): MarkdownConfiguration {
        val any = anyMacro ?: enabledMacros.isEmpty()
        return MarkdownConfiguration(any, enabledMacros, emoji)
    }
}