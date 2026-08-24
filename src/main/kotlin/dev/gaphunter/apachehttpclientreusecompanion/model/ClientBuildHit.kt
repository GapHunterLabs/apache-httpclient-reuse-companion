package dev.gaphunter.apachehttpclientreusecompanion.model

import com.intellij.psi.PsiElement

/** One `HttpClients.createDefault()`/`HttpClients.custom().build()` call site built inside a non-constructor method. */
data class ClientBuildHit(val callElement: PsiElement)
