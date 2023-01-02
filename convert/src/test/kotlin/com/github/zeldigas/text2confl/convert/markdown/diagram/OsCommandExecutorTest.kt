package com.github.zeldigas.text2confl.convert.markdown.diagram

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission.*
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText

@EnabledOnOs(OS.LINUX)
class OsCommandExecutorTest {

    private val executor = OsCommandExecutor()

    @Test
    fun `Check command in PATH var`() {
        val result = executor.commandAvailable("ls")

        assertThat(result).isTrue()
    }

    @Test
    fun `Check command by path`(@TempDir tempDir: Path) {
        val command = tempDir / "test-bin-file-for-text2confl-unit-tests"

        assertThat(executor.commandAvailable(command.toString())).isFalse()

        command.createFile()

        assertThat(executor.commandAvailable(command.toString())).isTrue()
    }

    @Test
    fun `Execute commnad`(@TempDir tempDir: Path) {
        val command = tempDir / "test.sh"

        command.writeText("""
            #/usr/bin/env sh
            
            echo "args: $@"
            >&2 echo -n "to stderr"
            cat -
            
        """.trimIndent())
        Files.setPosixFilePermissions(command, setOf(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE))

        val result = executor.execute(cmd(command.toString()){
            flag("--test")
            opt("hello", "world")

            stdin("test input")
        })

        assertThat(result).isEqualTo(
            ExecutionResult(0, """
            args: --test hello world
            test input
        """.trimIndent(), "to stderr")
        )
    }
}