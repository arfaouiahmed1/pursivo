package io.github.arfaouiahmed1.pursivo.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PursivoDao {
    @Query("SELECT * FROM pipeline_stages ORDER BY sortOrder")
    fun observeStages(): Flow<List<StageEntity>>

    @Query("SELECT * FROM pipeline_stages ORDER BY sortOrder")
    suspend fun getStages(): List<StageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStages(stages: List<StageEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStage(stage: StageEntity): Long

    @Query("SELECT * FROM applications ORDER BY updatedAt DESC")
    fun observeApplications(): Flow<List<ApplicationEntity>>

    @Query("SELECT * FROM applications ORDER BY updatedAt DESC")
    suspend fun getApplications(): List<ApplicationEntity>

    @Query("SELECT * FROM applications WHERE id = :id")
    suspend fun getApplication(id: Long): ApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApplication(application: ApplicationEntity): Long

    @Update
    suspend fun updateApplication(application: ApplicationEntity)

    @Query("DELETE FROM applications WHERE id = :id")
    suspend fun deleteApplication(id: Long)

    @Query("SELECT * FROM application_events ORDER BY createdAt DESC")
    fun observeEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM application_events ORDER BY createdAt DESC")
    suspend fun getEvents(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("SELECT * FROM tasks ORDER BY completed, dueAt")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY completed, dueAt")
    suspend fun getTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity): Long

    @Query("SELECT * FROM career_profile WHERE id = 1")
    fun observeProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM career_profile WHERE id = 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM vault_documents ORDER BY active DESC, createdAt DESC")
    fun observeDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM vault_documents ORDER BY active DESC, createdAt DESC")
    suspend fun getDocuments(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: DocumentEntity): Long

    @Query("DELETE FROM vault_documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)

    @Query("UPDATE vault_documents SET active = 0")
    suspend fun clearActiveDocuments()

    @Query("UPDATE vault_documents SET active = 1 WHERE id = :id")
    suspend fun activateDocument(id: Long)

    @Transaction
    suspend fun setActiveDocument(id: Long) {
        clearActiveDocuments()
        activateDocument(id)
    }

    @Query("SELECT * FROM provider_configs ORDER BY displayName")
    fun observeProviderConfigs(): Flow<List<ProviderConfigEntity>>

    @Query("SELECT * FROM provider_configs ORDER BY displayName")
    suspend fun getProviderConfigs(): List<ProviderConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProviderConfig(config: ProviderConfigEntity): Long

    @Query("DELETE FROM provider_configs WHERE id = :id")
    suspend fun deleteProviderConfig(id: Long)

    @Query("SELECT * FROM ai_insights ORDER BY createdAt DESC")
    fun observeInsights(): Flow<List<AiInsightEntity>>

    @Query("SELECT * FROM ai_insights ORDER BY createdAt DESC")
    suspend fun getInsights(): List<AiInsightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInsight(insight: AiInsightEntity): Long

    @Query("UPDATE ai_insights SET accepted = 1 WHERE id = :id")
    suspend fun acceptInsight(id: Long)

    @Query("DELETE FROM ai_insights WHERE id = :id")
    suspend fun deleteInsight(id: Long)

    @Query("DELETE FROM application_events") suspend fun clearEvents()
    @Query("DELETE FROM tasks") suspend fun clearTasks()
    @Query("DELETE FROM ai_insights") suspend fun clearInsights()
    @Query("DELETE FROM applications") suspend fun clearApplications()
    @Query("DELETE FROM pipeline_stages") suspend fun clearStages()
    @Query("DELETE FROM career_profile") suspend fun clearProfile()
    @Query("DELETE FROM vault_documents") suspend fun clearDocuments()
    @Query("DELETE FROM provider_configs") suspend fun clearProviderConfigs()

    @Transaction
    suspend fun replaceAll(
        stages: List<StageEntity>,
        applications: List<ApplicationEntity>,
        events: List<EventEntity>,
        tasks: List<TaskEntity>,
        profile: ProfileEntity?,
        documents: List<DocumentEntity>,
        providers: List<ProviderConfigEntity>,
        insights: List<AiInsightEntity>,
    ) {
        clearEvents(); clearTasks(); clearInsights(); clearApplications(); clearStages(); clearProfile(); clearDocuments(); clearProviderConfigs()
        upsertStages(stages)
        applications.forEach { upsertApplication(it) }
        insertEvents(events)
        tasks.forEach { upsertTask(it) }
        profile?.let { upsertProfile(it) }
        documents.forEach { upsertDocument(it) }
        providers.forEach { upsertProviderConfig(it) }
        insights.forEach { upsertInsight(it) }
    }
}
