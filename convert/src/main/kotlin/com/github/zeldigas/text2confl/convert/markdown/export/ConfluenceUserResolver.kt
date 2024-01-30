package com.github.zeldigas.text2confl.convert.markdown.export

interface ConfluenceUserResolver {

    companion object {
        val NOP = object : ConfluenceUserResolver {
            override fun resolve(userKey: String): String? = null
        }
    }

    fun resolve(userKey: String): String?

}