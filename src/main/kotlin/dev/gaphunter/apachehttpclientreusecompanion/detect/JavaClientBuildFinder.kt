package dev.gaphunter.apachehttpclientreusecompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import dev.gaphunter.apachehttpclientreusecompanion.model.ClientBuildHit

/**
 * Finds `HttpClients.createDefault()`/`HttpClients.createSystem()`/
 * `HttpClients.custom().build()` (Apache HttpClient, both the 4.x
 * `org.apache.http` and 5.x `org.apache.hc.client5.http` packages use
 * `HttpClients` as the factory class) written inside a non-constructor
 * method body -- Apache's own javadoc annotates `CloseableHttpClient`
 * `@Contract(threading = ThreadingBehavior.SAFE)` and states client
 * instances "are expected to be thread safe" and "it is recommended
 * that the same instance of this class is reused for multiple request
 * executions". Building one inside a regular method means a brand new
 * connection pool on every call.
 *
 * **v0.1 scope, stated honestly:** matches by simple text, not real
 * type resolution, so it works whether the real Apache HttpClient jar
 * (4.x or 5.x) is on the classpath or not -- an unrelated `HttpClients`
 * class from a different library is a possible (rare) false positive.
 * Only the "build from scratch" shape is flagged; a client obtained by
 * reference from an existing shared instance/dependency injection is
 * never flagged.
 */
object JavaClientBuildFinder {

    private val FACTORY_METHOD_NAMES = setOf("createDefault", "createSystem", "createMinimal")

    fun findAll(file: PsiFile): List<ClientBuildHit> {
        val hits = mutableListOf<ClientBuildHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                hitForFactoryCall(expression)?.let { hits += it }
                hitForBuilderBuild(expression)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitForFactoryCall(call: PsiMethodCallExpression): ClientBuildHit? {
        val methodName = call.methodExpression.referenceName ?: return null
        if (methodName !in FACTORY_METHOD_NAMES) return null
        val qualifier = call.methodExpression.qualifierExpression ?: return null
        if (qualifier.text != "HttpClients") return null
        return hitIfNotInConstructor(call)
    }

    private fun hitForBuilderBuild(buildCall: PsiMethodCallExpression): ClientBuildHit? {
        if (buildCall.methodExpression.referenceName != "build") return null
        val qualifier = buildCall.methodExpression.qualifierExpression ?: return null
        // The chain between `HttpClients.custom()` and `.build()` may have any number of
        // `.setXxx(...)` calls, e.g. `HttpClients.custom().setMaxConnTotal(50).build()` -- matching the
        // full qualifier text's start avoids re-walking each intermediate call by hand.
        if (!qualifier.text.startsWith("HttpClients.custom(")) return null
        return hitIfNotInConstructor(buildCall)
    }

    private fun hitIfNotInConstructor(element: PsiElement): ClientBuildHit? {
        val containingMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return null
        if (containingMethod.isConstructor) return null
        return ClientBuildHit(leafOf(element))
    }

    private fun leafOf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) current = current.firstChild
        return current
    }
}
