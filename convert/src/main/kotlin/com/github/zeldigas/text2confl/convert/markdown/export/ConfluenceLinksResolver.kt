package com.github.zeldigas.text2confl.convert.markdown.export

interface ConfluenceLinksResolver {

    companion object {
        val NOP = object : ConfluenceLinksResolver {
            override fun resolve(space: String?, title: String): String = "wiki://$space/$title"
        }
    }

    fun resolve(space: String?, title: String): String

}