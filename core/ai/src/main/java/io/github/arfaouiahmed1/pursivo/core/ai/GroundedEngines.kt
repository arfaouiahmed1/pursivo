package io.github.arfaouiahmed1.pursivo.core.ai

import io.github.arfaouiahmed1.pursivo.core.model.DraftPackage
import io.github.arfaouiahmed1.pursivo.core.model.FitAnalysis
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

const val PROMPT_VERSION = "pursivo-grounded-v1"

internal object PromptContracts {
    val fitSystem = """
        You are Pursivo's evidence-first job application analyst.
        Use only facts present in the supplied job, profile, and document context.
        Never infer legal work authorization, credentials, years of experience, or achievements.
        Every strength and gap must quote or precisely point to supplied evidence.
        A fit score is decision support, not a hiring prediction.
        Return valid JSON only with: score (0-100), summary, strengths [{claim,evidence}],
        gaps [{claim,evidence}], risks [string], missingInformation [string], nextAction.
    """.trimIndent()

    val draftSystem = """
        You are Pursivo's grounded application writer.
        Never invent employers, dates, metrics, technologies, education, or accomplishments.
        Rephrase only evidence supplied in context. Put unresolved facts in placeholders using [LIKE THIS].
        Return valid JSON only with: resumeBullets [string], coverLetter, placeholders [string].
    """.trimIndent()
}

@Singleton
class GroundedInsightEngine @Inject constructor() : InsightEngine {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun analyze(provider: AiProvider, context: AiContext): FitAnalysis {
        require(context.application.description.isNotBlank()) { "Add a job description before analysis." }
        val response = provider.generateJson(PromptContracts.fitSystem, contextPayload(context))
        val analysis = json.decodeFromString<FitAnalysis>(cleanJson(response))
        require(analysis.score in 0..100) { "Provider returned an invalid fit score." }
        return analysis
    }
}

@Singleton
class GroundedDraftingEngine @Inject constructor() : DraftingEngine {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun draft(provider: AiProvider, context: AiContext, analysis: FitAnalysis): DraftPackage {
        val prompt = contextPayload(context) + "\n\nVERIFIED ANALYSIS:\n" + json.encodeToString(FitAnalysis.serializer(), analysis)
        return json.decodeFromString(cleanJson(provider.generateJson(PromptContracts.draftSystem, prompt)))
    }
}

internal fun contextPayload(context: AiContext) = buildString {
    appendLine("JOB")
    appendLine("Company: ${context.application.company}")
    appendLine("Role: ${context.application.role}")
    appendLine("Location: ${context.application.location}")
    appendLine("Description:\n${context.application.description}")
    appendLine("\nPROFILE")
    appendLine("Headline: ${context.profile.headline}")
    appendLine("Skills: ${context.profile.skills.joinToString()}")
    appendLine("Preferences: ${context.profile.preferences}")
    appendLine("Verified achievements:\n${context.profile.achievements}")
    context.document?.let {
        appendLine("\nSELECTED DOCUMENT: ${it.name}")
        appendLine(it.normalizedText)
    }
}

private fun cleanJson(value: String): String = value.trim()
    .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
