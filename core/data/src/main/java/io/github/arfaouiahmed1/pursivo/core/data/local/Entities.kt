package io.github.arfaouiahmed1.pursivo.core.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.arfaouiahmed1.pursivo.core.model.AiInsight
import io.github.arfaouiahmed1.pursivo.core.model.ApplicationEvent
import io.github.arfaouiahmed1.pursivo.core.model.ApplicationTask
import io.github.arfaouiahmed1.pursivo.core.model.CareerProfile
import io.github.arfaouiahmed1.pursivo.core.model.EventType
import io.github.arfaouiahmed1.pursivo.core.model.FitAnalysis
import io.github.arfaouiahmed1.pursivo.core.model.JobApplication
import io.github.arfaouiahmed1.pursivo.core.model.PipelineStage
import io.github.arfaouiahmed1.pursivo.core.model.Priority
import io.github.arfaouiahmed1.pursivo.core.model.ProviderConfig
import io.github.arfaouiahmed1.pursivo.core.model.ProviderType
import io.github.arfaouiahmed1.pursivo.core.model.SemanticStage
import io.github.arfaouiahmed1.pursivo.core.model.VaultDocument
import io.github.arfaouiahmed1.pursivo.core.model.WorkMode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val databaseJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Entity(tableName = "pipeline_stages")
data class StageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val sortOrder: Int,
    val colorArgb: Long,
    val semanticKind: String,
    val hidden: Boolean,
)

@Entity(
    tableName = "applications",
    foreignKeys = [ForeignKey(
        entity = StageEntity::class,
        parentColumns = ["id"],
        childColumns = ["stageId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("stageId"), Index("company"), Index("updatedAt")],
)
data class ApplicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val company: String,
    val role: String,
    val stageId: Long,
    val priority: String,
    val sourceUrl: String,
    val description: String,
    val location: String,
    val workMode: String,
    val salary: String,
    val deadlineEpochMillis: Long?,
    val nextAction: String,
    val notes: String,
    val tagsJson: String,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "application_events", indices = [Index("applicationId")])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long,
    val type: String,
    val title: String,
    val detail: String,
    val createdAt: Long,
)

@Entity(tableName = "tasks", indices = [Index("applicationId"), Index("dueAt")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long?,
    val title: String,
    val dueAt: Long?,
    val reminderEnabled: Boolean,
    val completed: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "career_profile")
data class ProfileEntity(
    @PrimaryKey val id: Long = 1,
    val displayName: String,
    val headline: String,
    val location: String,
    val targetRolesJson: String,
    val skillsJson: String,
    val preferences: String,
    val achievements: String,
    val updatedAt: Long,
)

@Entity(tableName = "vault_documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mimeType: String,
    val localPath: String,
    val normalizedText: String,
    val version: Int,
    val active: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "provider_configs")
data class ProviderConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val displayName: String,
    val model: String,
    val baseUrl: String,
    val credentialAlias: String,
    val enabled: Boolean,
)

@Entity(tableName = "ai_insights", indices = [Index("applicationId")])
data class AiInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long,
    val provider: String,
    val model: String,
    val promptVersion: String,
    val contextSummary: String,
    val fitAnalysisJson: String?,
    val draftPackageJson: String?,
    val accepted: Boolean,
    val createdAt: Long,
)

fun StageEntity.toModel() = PipelineStage(id, label, sortOrder, colorArgb, SemanticStage.valueOf(semanticKind), hidden)
fun PipelineStage.toEntity() = StageEntity(id, label, order, colorArgb, semanticKind.name, hidden)

fun ApplicationEntity.toModel() = JobApplication(
    id, company, role, stageId, Priority.valueOf(priority), sourceUrl, description, location,
    WorkMode.valueOf(workMode), salary, deadlineEpochMillis, nextAction, notes,
    databaseJson.decodeFromString(tagsJson), archived, createdAt, updatedAt,
)
fun JobApplication.toEntity() = ApplicationEntity(
    id, company, role, stageId, priority.name, sourceUrl, description, location, workMode.name,
    salary, deadlineEpochMillis, nextAction, notes, databaseJson.encodeToString(tags), archived, createdAt, updatedAt,
)

fun EventEntity.toModel() = ApplicationEvent(id, applicationId, EventType.valueOf(type), title, detail, createdAt)
fun ApplicationEvent.toEntity() = EventEntity(id, applicationId, type.name, title, detail, createdAt)

fun TaskEntity.toModel() = ApplicationTask(id, applicationId, title, dueAt, reminderEnabled, completed, createdAt)
fun ApplicationTask.toEntity() = TaskEntity(id, applicationId, title, dueAt, reminderEnabled, completed, createdAt)

fun ProfileEntity.toModel() = CareerProfile(
    id, displayName, headline, location,
    databaseJson.decodeFromString(targetRolesJson), databaseJson.decodeFromString(skillsJson),
    preferences, achievements, updatedAt,
)
fun CareerProfile.toEntity() = ProfileEntity(
    id, displayName, headline, location, databaseJson.encodeToString(targetRoles),
    databaseJson.encodeToString(skills), preferences, achievements, updatedAt,
)

fun DocumentEntity.toModel() = VaultDocument(id, name, mimeType, localPath, normalizedText, version, active, createdAt)
fun VaultDocument.toEntity() = DocumentEntity(id, name, mimeType, localPath, normalizedText, version, active, createdAt)

fun ProviderConfigEntity.toModel() = ProviderConfig(id, ProviderType.valueOf(type), displayName, model, baseUrl, credentialAlias, enabled)
fun ProviderConfig.toEntity() = ProviderConfigEntity(id, type.name, displayName, model, baseUrl, credentialAlias, enabled)

fun AiInsightEntity.toModel() = AiInsight(
    id, applicationId, ProviderType.valueOf(provider), model, promptVersion, contextSummary,
    fitAnalysisJson?.let { databaseJson.decodeFromString<FitAnalysis>(it) },
    draftPackageJson?.let { databaseJson.decodeFromString(it) }, accepted, createdAt,
)
fun AiInsight.toEntity() = AiInsightEntity(
    id, applicationId, provider.name, model, promptVersion, contextSummary,
    fitAnalysis?.let { databaseJson.encodeToString(it) },
    draftPackage?.let { databaseJson.encodeToString(it) }, accepted, createdAt,
)
