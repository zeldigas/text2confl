package com.github.zeldigas.text2confl.convert

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}
private val JSON_PARSER = ObjectMapper()

fun parseAttribute(value: Any): Any {
    return if (value is String) {
        try {
            if (value.enclosedIn('{', '}')) {
                JSON_PARSER.readValue<Map<String, *>>(value)
            } else if (value.enclosedIn('[', ']')) {
                JSON_PARSER.readValue<List<String>>(value)
            } else {
                value
            }
        } catch (e: JsonParseException) {
            logger.debug(e) { "Failed to parse value $value, looks like not a valid json" }
            value
        }
    } else {
        value
    }
}

private fun String.enclosedIn(start: Char, end: Char): Boolean {
    return this.startsWith(start) && this.endsWith(end)
}