package com.github.zeldigas.text2confl.convert

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class UrlUtilsTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "file.txt|file.txt",
            "my%20file.txt|my file.txt",
            "dir%20one/my%20file.txt|dir one/my file.txt",
            "./file%20with%20spaces.txt|./file with spaces.txt",
            "c++.png|c++.png",
            "c%2B%2B.png|c++.png",
            "a+b%20c.png|a+b c.png",
            "50%25%20off.png|50% off.png",
            "%D1%84%D0%B0%D0%B9%D0%BB.txt|файл.txt",
            "my file.txt|my file.txt",
        ]
    )
    fun `Url encoded path is decoded`(path: String, expected: String) {
        assertThat(path.decodeFilePath()).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "50% off.png",
            "100%.png",
            "%",
            "a%2.png",
            "a%zz.png",
        ]
    )
    fun `Path with invalid percent sequence is kept as is`(path: String) {
        assertThat(path.decodeFilePath()).isEqualTo(path)
    }
}
