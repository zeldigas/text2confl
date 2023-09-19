package com.github.zeldigas.text2confl.convert.asciidoc

import org.asciidoctor.Asciidoctor
import org.asciidoctor.jruby.extension.spi.ExtensionRegistry

class DefaultMacros : ExtensionRegistry {

    companion object {
        const val DIR = "/com/github/zeldigas/text2confl/asciidocmacros"
    }

    override fun register(asciidoc: Asciidoctor) {
        val rubyExtensions = asciidoc.rubyExtensionRegistry()
        listOf("StatusMacro", "UserMacro", "SimpleInlineMacro").forEach { macro ->
            this::class.java.getResourceAsStream("$DIR/$macro.rb").use { rubyExtensions.loadClass(it) }
            rubyExtensions.inlineMacro(macro)
        }
    }
}