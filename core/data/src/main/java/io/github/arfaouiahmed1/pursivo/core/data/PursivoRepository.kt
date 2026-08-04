package io.github.arfaouiahmed1.pursivo.core.data

import io.github.arfaouiahmed1.pursivo.core.data.local.PursivoDao
import io.github.arfaouiahmed1.pursivo.core.data.local.toEntity
import io.github.arfaouiahmed1.pursivo.core.data.local.toModel
import io.github.arfaouiahmed1.pursivo.core.model.AiInsight
import io.github.arfaouiahmed1.pursivo.core.model.ApplicationEvent
import io.github.arfaouiahmed1.pursivo.core.model.ApplicationTask
import io.github.arfaouiahmed1.pursivo.core.model.CareerProfile
import io.github.arfaouiahmed1.pursivo.core.model.EventType
import io.github.arfaouiahmed1.pursivo.core.model.JobApplication
import io.github.arfaouiahmed1.pursivo.core.model.PipelineStage
import io.github.arfaouiahmed1.pursivo.core.model.ProviderConfig
import io.github.arfaouiahmed1.pursivo.core.model.SemanticStage
import io.github.arfaouiahmed1.pursivo.core.model.VaultDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PursivoRepository @Inject constructor(private val dao: PursivoDao) {
    val applications: Flow<List<JobApplication>> = dao.observeApplications().map { rows -> rows.map { it.toModel() } }
    val stages: Flow<List<PipelineStage>> = dao.observeStages().map { rows -> rows.map { it.toModel() } }
    val events: Flow<List<ApplicationEvent>> = dao.observeEvents().map { rows -> rows.map { it.toModel() } }
    val tasks: Flow<List<ApplicationTask>> = dao.observeTasks().map { rows -> rows.map { it.toModel() } }
    val profile: Flow<CareerProfile?> = dao.observeProfile().map { it?.toModel() }
    val documents: Flow<List<VaultDocument>> = dao.observeDocuments().map { rows -> rows.map { it.toModel() } }
    val providers: Flow<List<ProviderConfig>> = dao.observeProviderConfigs().map { rows -> rows.map { it.toModel() } }
    val insights: Flow<List<AiInsight>> = dao.observeInsights().map { rows -> rows.map { it.toModel() } }

    suspend fun ensureSeeded() {
        if (dao.getStages().isNotEmpty()) return
        defaultStages.forEach { dao.upsertStage(it.toEntity()) }
        dao.upsertProfile(CareerProfile().toEntity())
    }

    suspend fun saveApplication(application: JobApplication): Long {
        val now = System.currentTimeMillis()
        val existing = if (application.id == 0L) null else dao.getApplication(application.id)?.toModel()
        val normalized = application.copy(
            company = application.company.trim(),
            role = application.role.trim(),
            updatedAt = now,
        )
        val id = dao.upsertApplication(normalized.toEntity())
        val resolvedId = if (normalized.id == 0L) id else normalized.id
        if (existing == null) {
            dao.insertEvent(ApplicationEvent(
                applicationId = resolvedId,
                type = EventType.CREATED,
                title = "Application added",
                detail = "${normalized.role} at ${normalized.company}",
            ).toEntity())
        } else {
            if (existing.stageId != normalized.stageId) dao.insertEvent(ApplicationEvent(
                applicationId = resolvedId,
                type = EventType.STAGE_CHANGED,
                title = "Stage changed",
                detail = "Moved to ${dao.getStages().firstOrNull { it.id == normalized.stageId }?.label ?: "a custom stage"}",
            ).toEntity())
            if (existing.notes != normalized.notes && normalized.notes.isNotBlank()) dao.insertEvent(ApplicationEvent(
                applicationId = resolvedId,
                type = EventType.NOTE_ADDED,
                title = "Notes updated",
                detail = normalized.notes,
            ).toEntity())
        }
        return resolvedId
    }

    suspend fun archiveApplication(application: JobApplication, archived: Boolean) =
        saveApplication(application.copy(archived = archived))

    suspend fun deleteApplication(id: Long) = dao.deleteApplication(id)

    suspend fun saveStage(stage: PipelineStage) = dao.upsertStage(stage.toEntity())
    suspend fun saveProfile(profile: CareerProfile) = dao.upsertProfile(profile.copy(updatedAt = System.currentTimeMillis()).toEntity())
    suspend fun saveDocument(document: VaultDocument) = dao.upsertDocument(document.toEntity())
    suspend fun deleteDocument(id: Long) = dao.deleteDocument(id)
    suspend fun setActiveDocument(id: Long) = dao.setActiveDocument(id)

    suspend fun saveTask(task: ApplicationTask): Long {
        val id = dao.upsertTask(task.toEntity())
        val applicationId = task.applicationId
        if (task.id == 0L && applicationId != null) {
            dao.insertEvent(ApplicationEvent(
                applicationId = applicationId,
                type = EventType.TASK_CREATED,
                title = "Next action added",
                detail = task.title,
            ).toEntity())
        }
        return id
    }

    suspend fun toggleTask(task: ApplicationTask) {
        dao.upsertTask(task.copy(completed = !task.completed).toEntity())
        val applicationId = task.applicationId
        if (!task.completed && applicationId != null) {
            dao.insertEvent(ApplicationEvent(
                applicationId = applicationId,
                type = EventType.TASK_COMPLETED,
                title = "Next action completed",
                detail = task.title,
            ).toEntity())
        }
    }

    suspend fun saveProvider(config: ProviderConfig): Long = dao.upsertProviderConfig(config.toEntity())
    suspend fun deleteProvider(id: Long) = dao.deleteProviderConfig(id)
    suspend fun saveInsight(insight: AiInsight): Long = dao.upsertInsight(insight.toEntity())
    suspend fun acceptInsight(id: Long) = dao.acceptInsight(id)
    suspend fun deleteInsight(id: Long) = dao.deleteInsight(id)

    suspend fun application(id: Long): JobApplication? = dao.getApplication(id)?.toModel()

    companion object {
        val defaultStages = listOf(
            PipelineStage(label = "Saved", order = 0, colorArgb = 0xFF526581, semanticKind = SemanticStage.SAVED),
            PipelineStage(label = "Preparing", order = 1, colorArgb = 0xFF8B5E00, semanticKind = SemanticStage.PREPARING),
            PipelineStage(label = "Applied", order = 2, colorArgb = 0xFF315BA8, semanticKind = SemanticStage.APPLIED),
            PipelineStage(label = "Screening", order = 3, colorArgb = 0xFF6E4BA3, semanticKind = SemanticStage.SCREENING),
            PipelineStage(label = "Interviewing", order = 4, colorArgb = 0xFF00796B, semanticKind = SemanticStage.INTERVIEWING),
            PipelineStage(label = "Offer", order = 5, colorArgb = 0xFF257A52, semanticKind = SemanticStage.OFFER),
            PipelineStage(label = "Closed", order = 6, colorArgb = 0xFF8C4A4A, semanticKind = SemanticStage.CLOSED),
        )
    }
}
