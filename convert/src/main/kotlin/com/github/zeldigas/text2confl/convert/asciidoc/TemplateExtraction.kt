package com.github.zeldigas.text2confl.convert.asciidoc

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path

fun extractTemplatesTo(dest: Path, dirWithTemplates: URI) {
    val fs = FileSystems.newFileSystem(dirWithTemplates, emptyMap<String, Any?>())
    //todo fix logic to copy content from jar file
//    val jarRoot: Path = fs.getPath(dirWithTemplates)
//
//    Files.walkFileTree(jarRoot, object : SimpleFileVisitor<Path>() {
//
//        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
//            dest.resolve(jarRoot.relativize(dir)).createDirectories()
//            return FileVisitResult.CONTINUE
//        }
//
//        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
//            file.copyTo(dest.resolve(jarRoot.relativize(file)), true)
//            return FileVisitResult.CONTINUE
//        }
//    })
}