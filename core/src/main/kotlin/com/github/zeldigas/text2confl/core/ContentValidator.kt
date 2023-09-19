package com.github.zeldigas.text2confl.core

import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.Validation

class ContentValidationFailedException(val errors:List<String>): RuntimeException()

interface ContentValidator {
    fun validate(content: List<Page>)
}

class ContentValidatorImpl : ContentValidator {
    override fun validate(content: List<Page>) {
        val foundIssues:MutableList<String> = arrayListOf()
        collectErrors(content, foundIssues)
        if (foundIssues.isNotEmpty()) {
            throw ContentValidationFailedException(foundIssues)
        }
    }

    private fun collectErrors(pages: List<Page>, foundIssues: MutableList<String>) {
        for (page in pages){
            val validationResult = page.content.validate()
            if (validationResult is Validation.Invalid) {
                foundIssues.add("${page.source}: ${validationResult.issue}")
            }
            collectErrors(page.children, foundIssues)
        }
    }
}