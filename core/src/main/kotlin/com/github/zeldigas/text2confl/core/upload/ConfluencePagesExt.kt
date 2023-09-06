package com.github.zeldigas.text2confl.core.upload

import com.github.zeldigas.confclient.model.ConfluencePage

internal val ConfluencePage.managedPage: Boolean
    get() = pageProperty(HASH_PROPERTY)?.value != null

internal val ConfluencePage.parent: ConfluencePage?
    get() = ancestors?.lastOrNull()