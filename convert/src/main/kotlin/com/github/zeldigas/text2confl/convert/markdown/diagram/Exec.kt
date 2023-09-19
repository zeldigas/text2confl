package com.github.zeldigas.text2confl.convert.markdown.diagram

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists

interface CommandExecutor {
    fun execute(command: Command): ExecutionResult
    fun commandAvailable(command: String): Boolean
}

class OsCommandExecutor : CommandExecutor {
    override fun execute(command: Command): ExecutionResult {
        val fullList = buildList {
            add(command.command)
            addAll(command.options)
        }
        val outFile = command.outputFile
        val builder = ProcessBuilder(
            fullList
        )
        outFile?.let {
            builder.redirectOutput(it.toFile())
        }
        val process = builder.start()
        command.inputData?.let {
            process.outputStream.apply {
                this.write(it)
                this.close()
            }
        }
        val result = process.waitFor()
        val output = if (outFile == null) process.inputStream.bufferedReader().readText() else ""
        val error = process.errorStream.bufferedReader().readText()

        return ExecutionResult(result, output, error)
    }

    override fun commandAvailable(command: String): Boolean {
        if (File.separator !in command) {
            val locations = System.getenv().get("PATH")?.split(File.pathSeparator) ?: return false
            return locations.asSequence().map { Path(it) / command }.any { it.exists() }
        } else {
            val commandAsPath = Path(command)
            return commandAsPath.exists()
        }
    }
}

data class Command(
    val command: String,
    val options: MutableList<String>,
    var inputData: ByteArray? = null,
    var outputFile: Path? = null
) {

    fun flag(name: String) {
        options.add(name)
    }

    fun opt(name: String, value: String) {
        options.add(name)
        options.add(value)
    }

    fun stdin(stringContent: String, encoding: Charset = Charsets.UTF_8) {
        inputData = stringContent.toByteArray(encoding)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Command

        if (command != other.command) return false
        if (options != other.options) return false
        if (outputFile != other.outputFile) return false
        if (inputData != null) {
            if (other.inputData == null) return false
            if (!inputData.contentEquals(other.inputData)) return false
        } else if (other.inputData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = command.hashCode()
        return 31 * result + options.hashCode()
    }

}

data class ExecutionResult(val status: Int, val output: String, val error: String)

fun cmd(command: String, block: Command.() -> Unit): Command {
    val instance = Command(command, mutableListOf())
    instance.block()
    return instance
}