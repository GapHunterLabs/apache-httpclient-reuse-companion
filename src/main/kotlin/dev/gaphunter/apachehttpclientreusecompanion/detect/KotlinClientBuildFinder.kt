package dev.gaphunter.apachehttpclientreusecompanion.detect

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import dev.gaphunter.apachehttpclientreusecompanion.model.ClientBuildHit
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/** Kotlin counterpart of [JavaClientBuildFinder]. */
object KotlinClientBuildFinder {

    private val FACTORY_METHOD_NAMES = setOf("createDefault", "createSystem", "createMinimal")

    fun findAll(file: PsiFile): List<ClientBuildHit> {
        if (file !is KtFile) return emptyList()
        val hits = mutableListOf<ClientBuildHit>()
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)
                hitForFactoryCall(expression)?.let { hits += it }
                hitForBuilderBuild(expression)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitForFactoryCall(expression: KtDotQualifiedExpression): ClientBuildHit? {
        val call = expression.selectorExpression as? KtCallExpression ?: return null
        val name = call.calleeExpression?.text ?: return null
        if (name !in FACTORY_METHOD_NAMES) return null
        if (expression.receiverExpression.text != "HttpClients") return null
        return hitIfNotInConstructor(expression)
    }

    private fun hitForBuilderBuild(expression: KtDotQualifiedExpression): ClientBuildHit? {
        val buildCall = expression.selectorExpression as? KtCallExpression ?: return null
        if (buildCall.calleeExpression?.text != "build") return null

        // The chain between `HttpClients.custom()` and `.build()` may have any number of
        // `.setXxx(...)` calls, e.g. `HttpClients.custom().setMaxConnTotal(50).build()` -- matching the
        // full receiver text's start avoids re-walking each intermediate call by hand.
        if (!expression.receiverExpression.text.startsWith("HttpClients.custom(")) return null

        return hitIfNotInConstructor(expression)
    }

    private fun hitIfNotInConstructor(element: PsiElement): ClientBuildHit? {
        if (PsiTreeUtil.getParentOfType(element, KtConstructor::class.java) != null) return null
        if (PsiTreeUtil.getParentOfType(element, KtNamedFunction::class.java) == null) return null
        return ClientBuildHit(leafOf(element))
    }

    private fun leafOf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) current = current.firstChild
        return current
    }
}
