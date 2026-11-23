package com.github.zeldigas.text2confl.core.export

import java.nio.file.Path

/**
 * Receives progress notifications during page tree export.
 */
interface ExportProgressTracker {

    fun collectingTree(rootTitle: String) = Unit

    fun treeCollected(totalPages: Int) = Unit

    fun pageExported(index: Int, totalPages: Int, title: String, file: Path) = Unit

    fun exportCompleted(totalPages: Int) = Unit

    companion object {
        val NOP = object : ExportProgressTracker {}
    }
}
