package com.github.zeldigas.text2confl.convert.confluence

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.jupiter.api.Test

internal class LanguageMapperImplTest {

    @Test
    internal fun `Server mapper`() {
        val mapper = LanguageMapper.forServer("test")
        assertThat(mapper).isInstanceOf(LanguageMapperImpl::class).all {
            transform { it.defaultLanguage }.isEqualTo("test")
            prop(LanguageMapperImpl::mapping).isEqualTo(SERVER_REMAPPING)
            prop(LanguageMapperImpl::supportedLanguages).isEqualTo(CONFLUENCE_SERVER_LANGUAGES)
        }
    }

    @Test
    internal fun `Cloud mapper`() {
        val mapper = LanguageMapper.forCloud("test1")
        assertThat(mapper).isInstanceOf(LanguageMapperImpl::class).all {
            transform { it.defaultLanguage }.isEqualTo("test1")
            prop(LanguageMapperImpl::mapping).isEqualTo(CLOUD_REMAPPING)
            prop(LanguageMapperImpl::supportedLanguages).isEqualTo(CONFLUENCE_CLOUD_LANGUAGES)
        }
    }

    @Test
    internal fun `Existing language resolution`() {
        val mapper = LanguageMapperImpl(setOf("foo", "bar"), "fallback", mapOf("foo1" to "bar"))

        assertThat(mapper.mapToConfluenceLanguage("FOO")).isEqualTo("foo")
        assertThat(mapper.mapToConfluenceLanguage("foo")).isEqualTo("foo")
        assertThat(mapper.mapToConfluenceLanguage("foO")).isEqualTo("foo")
        assertThat(mapper.mapToConfluenceLanguage("baR")).isEqualTo("bar")
        assertThat(mapper.mapToConfluenceLanguage("FOO1")).isEqualTo("bar")
        assertThat(mapper.mapToConfluenceLanguage("foo1")).isEqualTo("bar")
    }

    @Test
    internal fun `Fallback to default`() {
        val mapper = LanguageMapperImpl(setOf("foo", "bar"), "fallback", mapOf("foo1" to "bar"))

        assertThat(mapper.mapToConfluenceLanguage("another")).isEqualTo("fallback")
    }
}