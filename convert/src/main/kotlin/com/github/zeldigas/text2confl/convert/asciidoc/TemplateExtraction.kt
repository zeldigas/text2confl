package com.github.zeldigas.text2confl.convert.asciidoc

import java.net.URI
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.createDirectories
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

fun extractTemplatesTo(dest: Path, dirWithTemplates: URI, pathInside: String) {
    val fs = FileSystems.newFileSystem(dirWithTemplates, emptyMap<String, Any?>())
    val jarRoot: Path = fs.getPath(pathInside)
    Files.walkFileTree(jarRoot, object : SimpleFileVisitor<Path>() {

        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            val relativize = jarRoot.relativize(dir).toString()
            dest.resolve(relativize).createDirectories()
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            val data = file.readBytes()
            val fileDestination = dest.resolve(jarRoot.relativize(file).toString())
            fileDestination.writeBytes(data)
            return FileVisitResult.CONTINUE
        }
    })
}