package com.github.zeldigas.text2confl.core.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile


private val mapper = JsonMapper.builder(YAMLFactory())
    .addModule(kotlinModule())
    .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()

private val CONFIG_FILE_NAMES = listOf(".text2confl", "text2confl")

private val log = KotlinLogging.logger {  }

fun readDirectoryConfig(dirOfFile: Path): DirectoryConfig {
    val resolver: (String) -> Path = if (dirOfFile.isRegularFile()) {
        dirOfFile::resolveSibling
    } else {
        dirOfFile::resolve
    }
    val docsDir = if (dirOfFile.isRegularFile()) dirOfFile.absolute().parent else dirOfFile

    val configFile = CONFIG_FILE_NAMES.asSequence()
        .flatMap { listOf("$it.yml", "$it.yaml") }
        .map(resolver)
        .filter { it.exists() }
        .firstOrNull()
    val directoryConfig:DirectoryConfig = if (configFile != null) {
        log.debug { "Found config file $configFile" }
        mapper.readValue(configFile.toFile())
    } else {
        log.debug { "No config file found in $docsDir. Using defaults" }
        DirectoryConfig()
    }
    directoryConfig.docsDir = docsDir
    return directoryConfig
}