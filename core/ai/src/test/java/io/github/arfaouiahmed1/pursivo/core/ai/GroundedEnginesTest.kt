package io.github.arfaouiahmed1.pursivo.core.ai

import io.github.arfaouiahmed1.pursivo.core.model.CareerProfile
import io.github.arfaouiahmed1.pursivo.core.model.JobApplication
import io.github.arfaouiahmed1.pursivo.core.model.VaultDocument
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GroundedEnginesTest {
    private val context = AiContext(
        application = JobApplication(
            company = "Acme",
            role = "Android engineer",
            stageId = 1,
            description = "Build accessible Kotlin applications.",
        ),
        profile = CareerProfile(
            headline = "Mobile engineer",
            skills = listOf("Kotlin", "Compose"),
            achievements = "Shipped an offline-first Android application.",
        ),
        document = VaultDocument(
            name = "resume.txt",
            mimeType = "text/plain",
            localPath = "/private/resume.txt",
            normalizedText = "Verified mobile engineering evidence.",
        ),
    )

    @Test fun fitAnalysisAcceptsSchemaValidatedGroundedOutput() = runBlocking {
        val engine = GroundedInsightEngine()
        val analysis = engine.analyze(FakeProvider(validAnalysis), context)
        assertEquals(78, analysis.score)
        assertEquals("Kotlin", analysis.strengths.single().claim)
    }

    @Test fun malformedProviderJsonIsNeverPersistable() {
        assertThrows(SerializationException::class.java) {
            runBlocking { GroundedInsightEngine().analyze(FakeProvider("not-json"), context) }
        }
    }

    @Test fun outOfRangeScoresFailValidation() {
        val invalidScore = validAnalysis.replace("\"score\":78", "\"score\":101")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { GroundedInsightEngine().analyze(FakeProvider(invalidScore), context) }
        }
    }

    @Test fun contextPreviewNamesButDoesNotDumpSelectedDocument() {
        val preview = context.preview()
        assertTrue(preview.contains("resume.txt"))
        assertTrue(!preview.contains("Verified mobile engineering evidence"))
    }

    private class FakeProvider(private val response: String) : AiProvider {
        override suspend fun testConnection() = ConnectionResult(true, "ok")
        override suspend fun generateJson(systemPrompt: String, userPrompt: String) = response
    }

    private companion object {
        val validAnalysis = """
            {
              "score":78,
              "summary":"Evidence supports a useful match.",
              "strengths":[{"claim":"Kotlin","evidence":"Profile skill: Kotlin"}],
              "gaps":[],
              "risks":[],
              "missingInformation":["Years of experience"],
              "nextAction":"Confirm experience requirements."
            }
        """.trimIndent()
    }
}
