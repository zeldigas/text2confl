package com.github.zeldigas.text2confl.convert

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

interface PagesDetector {

    fun <T, R> scanDirectoryRecursively(
        dir: Path,
        filter: (Path) -> Boolean,
        converter: (file: Path) -> T,
        assembler: (file: Path, parent: T, children: List<R>) -> R
    ): List<R>

}

object FileNameBasedDetector : PagesDetector {

    override fun <T, R> scanDirectoryRecursively(
        dir: Path,
        filter: (Path) -> Boolean,
        converter: (Path) -> T,
        assembler: (file: Path, parent: T, children: List<R>) -> R
    ): List<R> {
        return dir.listDirectoryEntries().filter { filter(it) }.sorted()
            .map { file ->
                val content = converter(file)
                val subdirectory = file.parent.resolve(file.nameWithoutExtension)
                val children = if (Files.exists(subdirectory) && Files.isDirectory(subdirectory)) {
                    scanDirectoryRecursively(subdirectory, filter, converter, assembler)
                } else {
                    emptyList()
                }
                assembler(file, content, children)
            }
    }
}