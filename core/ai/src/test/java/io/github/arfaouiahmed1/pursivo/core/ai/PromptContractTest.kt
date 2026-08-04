package io.github.arfaouiahmed1.pursivo.core.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class PromptContractTest {
    @Test fun fitPromptRequiresEvidenceAndRejectsLegalInference() {
        assertTrue(PromptContracts.fitSystem.contains("Every strength and gap must quote"))
        assertTrue(PromptContracts.fitSystem.contains("Never infer legal work authorization"))
    }

    @Test fun draftingPromptForbidsInventedAchievements() {
        assertTrue(PromptContracts.draftSystem.contains("Never invent employers"))
        assertTrue(PromptContracts.draftSystem.contains("[LIKE THIS]"))
    }
}
