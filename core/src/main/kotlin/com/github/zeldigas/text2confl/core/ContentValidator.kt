package com.github.zeldigas.text2confl.core

import com.github.zeldigas.text2confl.convert.Page
import com.github.zeldigas.text2confl.convert.Validation

class ContentValidationFailedException(val errors: List<String>) : RuntimeException()

interface ContentValidator {
    fun validate(content: List<Page>, autofix: Boolean)
}

class ContentValidatorImpl : ContentValidator {
    override fun validate(content: List<Page>,autofix: Boolean) {
        val foundIssues: MutableList<String> = arrayListOf()
        collectErrors(content, foundIssues, autofix)
        if (foundIssues.isNotEmpty()) {
            throw ContentValidationFailedException(foundIssues)
        }
    }

    private fun collectErrors(pages: List<Page>, foundIssues: MutableList<String>,autofix: Boolean) {
        for (page in pages) {
            val validationResult = page.content.validate(autofix)
            if (validationResult is Validation.Invalid) {
                foundIssues.add("${page.source}: ${validationResult.issue}")
            }
            collectErrors(page.children, foundIssues, autofix)
        }
    }
}