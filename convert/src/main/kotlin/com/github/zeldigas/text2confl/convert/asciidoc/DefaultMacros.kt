package com.github.zeldigas.text2confl.convert.asciidoc

import org.asciidoctor.Asciidoctor
import org.asciidoctor.jruby.extension.spi.ExtensionRegistry

class DefaultMacros : ExtensionRegistry {

    companion object {
        const val DIR = "/com/github/zeldigas/text2confl/asciidocmacros/"
    }

    override fun register(asciidoc: Asciidoctor) {
        val rubyExtensions = asciidoc.rubyExtensionRegistry();
        listOf("StatusMacro", "UserMacro", "SimpleInlineMacro").forEach { macro ->
            rubyExtensions.loadClass(this::class.java.getResourceAsStream("$DIR/$macro.rb"))
            rubyExtensions.inlineMacro(macro)
        }
    }
}