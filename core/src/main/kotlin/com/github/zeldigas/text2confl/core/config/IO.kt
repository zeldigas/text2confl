package com.github.zeldigas.text2confl.core.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile


private val logger = KotlinLogging.logger { }

private val mapper = ObjectMapper(YAMLFactory())
    .registerKotlinModule()
    .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)


private val CONFIG_FILE_NAME = "text2confl.yml"
private val CONFIG_FILE_NAMES = listOf(CONFIG_FILE_NAME)

fun readDirectoryConfig(dirOfFile: Path): DirectoryConfig {
    val resolver: (String) -> Path = if (dirOfFile.isRegularFile()) {
        dirOfFile::resolveSibling
    } else {
        dirOfFile::resolve
    }

    var directoryConfig = CONFIG_FILE_NAMES.asSequence()
        .map(resolver)
        .filter { it.exists() }
        .map { mapper.readValue<DirectoryConfig>(it.toFile()) }
        .firstOrNull()


    if (directoryConfig == null) {
        logger.debug { "No Conf File in $dirOfFile" }
        logger.debug { "Try Loading conf from classpath" }

        val confFile = DirectoryConfig::class.java.classLoader.getResource(CONFIG_FILE_NAME)
        if (confFile != null) {
            logger.debug { "Found conf File : " + confFile.file }
            directoryConfig = mapper.readValue<DirectoryConfig>(confFile)
        } else {
            logger.debug { "Create default Directory Config" }
            directoryConfig = DirectoryConfig()
        }
    }

    directoryConfig.docsDir = if (dirOfFile.isRegularFile()) dirOfFile.absolute().parent else dirOfFile
    return directoryConfig
}