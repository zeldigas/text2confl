package com.github.zeldigas.text2confl.convert.asciidoc

import assertk.assertThat
import org.junit.jupiter.api.Test

internal class RenderingOfBasicTextFeatures : RenderingTestBase() {

    @Test
    internal fun `Paragraphs and basic style details`() {
        val result = toHtml(
            """            
            Test block **with** formatting
            and *many* 
            ^lines^.
            
            [%hardbreaks]
            Test block 
            **with enabled**
            hard linebreaks.
                        
            Rubies are red, +
            Topazes are blue.
            
            [.underline]#underlined text# and [.line-through]#strike through text#
            is supported too!
            
            Consisting of multiple
            paragraphs with image:https://myoctocat.com/assets/images/base-octocat.svg[Octocat,100]
        """.trimIndent()
        )

        assertThat(result).isEqualToConfluenceFormat(
            """          
            <p>Test block <strong>with</strong> formatting and <strong>many</strong> <sup>lines</sup>.</p>
            <p>Test block<br/> <strong>with enabled</strong><br/> hard linebreaks.</p>
            <p>Rubies are red,<br/> Topazes are blue.</p>
            <p><u>underlined text</u> and <del>strike through text</del> is supported too!</p>
            <p>Consisting of multiple paragraphs with <ac:image ac:width="100" ac:alt="Octocat"><ri:url ri:value="https://myoctocat.com/assets/images/base-octocat.svg" /></ac:image></p>
        """.trimIndent()
        )
    }

}