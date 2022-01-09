package com.github.zeldigas.kustantaja.cli

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import org.junit.jupiter.api.Test

internal class ConfluencePublisherTest {

    @Test
    internal fun `Service provider test`() {
        val command = MockCommand()
        val cli = ConfluencePublisher().subcommands(command)

        cli.parse(listOf("test"))

        assertThat(command.executed).isTrue()
    }

    class MockCommand : CliktCommand(name = "test") {

        val provider: ServiceProvider? by requireObject()

        var executed:Boolean = false

        override fun run() {
            assertThat(provider).isNotNull()
                .isInstanceOf(ServiceProviderImpl::class)
            executed = true
        }
    }

}