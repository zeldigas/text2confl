package com.github.zeldigas.text2confl.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.LoggerFactory


class StdOutFilter : Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent): FilterReply {
        return if (event.level.isGreaterOrEqual(Level.WARN)) {
            FilterReply.DENY
        } else {
            FilterReply.ACCEPT
        }
    }
}

class StdErrFilter : Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent): FilterReply {
        return if (event.level.isGreaterOrEqual(Level.WARN)) {
            FilterReply.ACCEPT
        } else {
            FilterReply.DENY
        }
    }
}

fun configureLogging(verbosity: Int) {
    if (verbosity == 0) return

    val rootLogger = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as Logger
    rootLogger.level = when (verbosity) {
        1 -> rootLogger.level
        2 -> Level.INFO
        else -> Level.DEBUG
    }

    val text2conflRoot = LoggerFactory.getLogger("com.github.zeldigas.text2confl") as Logger
    text2conflRoot.level = when (verbosity) {
        1 -> Level.INFO
        else -> Level.DEBUG
    }
}

fun enableHttpLogging() {
    val rootLogger = LoggerFactory.getLogger("io.ktor.client") as Logger
    rootLogger.level = Level.INFO
}