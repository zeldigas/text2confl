package com.github.zeldigas.text2confl.cli

import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.zeldigas.text2confl.core.export.ExportProgressTracker
import java.nio.file.Path

class PrintingExportProgressTracker(
    private val destination: Path,
    private val printer: (msg: String) -> Unit = ::println
) : ExportProgressTracker {

    override fun collectingTree(rootTitle: String) {
        printer("Collecting page tree of ${bold(rootTitle)}...")
    }

    override fun treeCollected(totalPages: Int) {
        printer("Found $totalPages page(s) to export")
    }

    override fun pageExported(index: Int, totalPages: Int, title: String, file: Path) {
        val counter = "[${index.toString().padStart(totalPages.toString().length)}/$totalPages]"
        printer("$counter ${green("Exported:")} $title -> ${relativeLocation(file)}")
    }

    override fun exportCompleted(totalPages: Int) {
        printer("Export completed: $totalPages page(s) exported to $destination")
    }

    private fun relativeLocation(file: Path): String = try {
        destination.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize()).toString()
    } catch (e: IllegalArgumentException) {
        file.toString()
    }
}
