package io.github.arfaouiahmed1.pursivo.core.ai

import io.github.arfaouiahmed1.pursivo.core.model.CareerProfile
import io.github.arfaouiahmed1.pursivo.core.model.DraftPackage
import io.github.arfaouiahmed1.pursivo.core.model.FitAnalysis
import io.github.arfaouiahmed1.pursivo.core.model.JobApplication
import io.github.arfaouiahmed1.pursivo.core.model.ProviderConfig
import io.github.arfaouiahmed1.pursivo.core.model.VaultDocument

data class ConnectionResult(val success: Boolean, val message: String)

data class AiContext(
    val application: JobApplication,
    val profile: CareerProfile,
    val document: VaultDocument?,
) {
    fun preview(): String = buildString {
        appendLine("Job: ${application.role} at ${application.company}")
        appendLine("Job description: ${application.description.take(1_500)}")
        appendLine("Profile: ${profile.headline}")
        appendLine("Skills: ${profile.skills.joinToString()}")
        appendLine("Preferences: ${profile.preferences.take(600)}")
        appendLine("Achievements: ${profile.achievements.take(1_200)}")
        document?.let { appendLine("Document: ${it.name} (${it.normalizedText.length} extracted characters)") }
    }.trim()
}

interface AiProvider {
    suspend fun testConnection(): ConnectionResult
    suspend fun generateJson(systemPrompt: String, userPrompt: String): String
}

interface ProviderClientFactory {
    fun create(config: ProviderConfig, apiKey: String): AiProvider
}

interface InsightEngine {
    suspend fun analyze(provider: AiProvider, context: AiContext): FitAnalysis
}

interface DraftingEngine {
    suspend fun draft(provider: AiProvider, context: AiContext, analysis: FitAnalysis): DraftPackage
}
