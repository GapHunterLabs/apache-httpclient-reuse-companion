package dev.gaphunter.apachehttpclientreusecompanion.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import dev.gaphunter.apachehttpclientreusecompanion.detect.JavaClientBuildFinder
import dev.gaphunter.apachehttpclientreusecompanion.detect.KotlinClientBuildFinder
import dev.gaphunter.apachehttpclientreusecompanion.model.ClientBuildHit
import dev.gaphunter.apachehttpclientreusecompanion.review.ReviewPrompt

class ClientBuiltPerCallLineMarkerProvider : LineMarkerProviderDescriptor(), DumbAware {

    override fun getName(): String = "Apache HttpClient built inside a method"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: MutableList<out PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        val file = elements.firstOrNull()?.containingFile ?: return
        val hits = when (file.language.id) {
            "JAVA" -> JavaClientBuildFinder.findAll(file)
            "kotlin" -> KotlinClientBuildFinder.findAll(file)
            else -> emptyList()
        }
        if (hits.isEmpty()) return

        val hitsByElement = hits.associateBy { it.callElement }
        for (element in elements) {
            val hit = hitsByElement[element] ?: continue
            result.add(buildMarker(hit))

            val path = file.virtualFile?.path ?: continue
            val lineNumber = file.viewProvider.document?.getLineNumber(element.textRange.startOffset) ?: -1
            ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
        }
    }

    private fun buildMarker(hit: ClientBuildHit): LineMarkerInfo<PsiElement> {
        val tooltip = "This Apache HttpClient is built here inside a method -- Apache's own javadoc marks it " +
            "thread-safe and recommends the same instance be reused for multiple request executions; each " +
            "instance is expensive to create and holds its own connection pool"
        return LineMarkerInfo(
            hit.callElement,
            hit.callElement.textRange,
            ClientReuseIcons.RISK,
            { _: PsiElement -> tooltip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { tooltip },
        )
    }
}
