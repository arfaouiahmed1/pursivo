package io.github.arfaouiahmed1.pursivo.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SemanticStage { SAVED, PREPARING, APPLIED, SCREENING, INTERVIEWING, OFFER, CLOSED }

@Serializable
enum class Priority { LOW, MEDIUM, HIGH }

@Serializable
enum class WorkMode { ONSITE, HYBRID, REMOTE, UNSPECIFIED }

@Serializable
enum class EventType { CREATED, STAGE_CHANGED, NOTE_ADDED, TASK_CREATED, TASK_COMPLETED, DOCUMENT_ATTACHED, AI_GENERATED }

@Serializable
enum class ProviderType { OPENAI, ANTHROPIC, GEMINI, OPENAI_COMPATIBLE }

@Serializable
enum class UpdateChannel { STABLE, PRERELEASE }

@Serializable
data class JobApplication(
    val id: Long = 0,
    val company: String,
    val role: String,
    val stageId: Long,
    val priority: Priority = Priority.MEDIUM,
    val sourceUrl: String = "",
    val description: String = "",
    val location: String = "",
    val workMode: WorkMode = WorkMode.UNSPECIFIED,
    val salary: String = "",
    val deadlineEpochMillis: Long? = null,
    val nextAction: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class PipelineStage(
    val id: Long = 0,
    val label: String,
    val order: Int,
    val colorArgb: Long,
    val semanticKind: SemanticStage,
    val hidden: Boolean = false,
)

@Serializable
data class ApplicationEvent(
    val id: Long = 0,
    val applicationId: Long,
    val type: EventType,
    val title: String,
    val detail: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ApplicationTask(
    val id: Long = 0,
    val applicationId: Long? = null,
    val title: String,
    val dueAt: Long? = null,
    val reminderEnabled: Boolean = false,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class CareerProfile(
    val id: Long = 1,
    val displayName: String = "",
    val headline: String = "",
    val location: String = "",
    val targetRoles: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val preferences: String = "",
    val achievements: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class VaultDocument(
    val id: Long = 0,
    val name: String,
    val mimeType: String,
    val localPath: String,
    val normalizedText: String = "",
    val version: Int = 1,
    val active: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ProviderConfig(
    val id: Long = 0,
    val type: ProviderType,
    val displayName: String,
    val model: String,
    val baseUrl: String = "",
    val credentialAlias: String,
    val enabled: Boolean = true,
)

@Serializable
data class EvidenceItem(val claim: String, val evidence: String)

@Serializable
data class FitAnalysis(
    val score: Int,
    val summary: String,
    val strengths: List<EvidenceItem>,
    val gaps: List<EvidenceItem>,
    val risks: List<String>,
    val missingInformation: List<String>,
    val nextAction: String,
)

@Serializable
data class DraftPackage(
    val resumeBullets: List<String>,
    val coverLetter: String,
    val placeholders: List<String>,
)

@Serializable
data class AiInsight(
    val id: Long = 0,
    val applicationId: Long,
    val provider: ProviderType,
    val model: String,
    val promptVersion: String,
    val contextSummary: String,
    val fitAnalysis: FitAnalysis? = null,
    val draftPackage: DraftPackage? = null,
    val accepted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class UpdatePreference(
    val channel: UpdateChannel = UpdateChannel.STABLE,
    val dismissedVersion: String = "",
    val lastCheckedAt: Long = 0,
)

@Serializable
data class UpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val prerelease: Boolean,
    val notes: String,
    val releaseUrl: String,
    val apkUrl: String? = null,
    val sha256: String? = null,
    val sizeBytes: Long? = null,
)

@Serializable
data class PursivoBackup(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val applications: List<JobApplication>,
    val stages: List<PipelineStage>,
    val events: List<ApplicationEvent>,
    val tasks: List<ApplicationTask>,
    val profile: CareerProfile?,
    val documents: List<VaultDocument>,
    val insights: List<AiInsight>,
    val providerConfigs: List<ProviderConfig>,
)
