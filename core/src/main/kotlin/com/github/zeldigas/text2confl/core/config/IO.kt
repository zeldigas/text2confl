package com.github.zeldigas.text2confl.core.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile


private val mapper = ObjectMapper(YAMLFactory())
    .registerKotlinModule()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

private val CONFIG_FILE_NAMES = listOf(".text2confl.yml", ".text2confl.yaml")

fun readDirectoryConfig(dirOfFile: Path): DirectoryConfig {
    val resolver: (String) -> Path = if (dirOfFile.isRegularFile()) {
        dirOfFile::resolveSibling
    } else {
        dirOfFile::resolve
    }

    val directoryConfig = CONFIG_FILE_NAMES.asSequence()
        .map(resolver)
        .filter { it.exists() }
        .map { mapper.readValue<DirectoryConfig>(it.toFile()) }
        .firstOrNull() ?: DirectoryConfig()
    directoryConfig.docsDir = if (dirOfFile.isRegularFile()) dirOfFile.absolute().parent else dirOfFile
    return directoryConfig
}