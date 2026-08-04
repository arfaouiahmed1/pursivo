package io.github.arfaouiahmed1.pursivo.ui

import android.net.Uri
import android.content.Context
import org.json.JSONObject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.arfaouiahmed1.pursivo.ReminderWorker
import io.github.arfaouiahmed1.pursivo.BuildConfig
import io.github.arfaouiahmed1.pursivo.core.ai.AiContext
import io.github.arfaouiahmed1.pursivo.core.ai.DraftingEngine
import io.github.arfaouiahmed1.pursivo.core.ai.InsightEngine
import io.github.arfaouiahmed1.pursivo.core.ai.PROMPT_VERSION
import io.github.arfaouiahmed1.pursivo.core.ai.ProviderClientFactory
import io.github.arfaouiahmed1.pursivo.core.data.AppPreferenceState
import io.github.arfaouiahmed1.pursivo.core.data.AppPreferences
import io.github.arfaouiahmed1.pursivo.core.data.ThemePreference
import io.github.arfaouiahmed1.pursivo.core.data.BackupService
import io.github.arfaouiahmed1.pursivo.core.data.DocumentVault
import io.github.arfaouiahmed1.pursivo.core.data.PursivoRepository
import io.github.arfaouiahmed1.pursivo.core.data.SecretStore
import io.github.arfaouiahmed1.pursivo.core.data.UpdateService
import io.github.arfaouiahmed1.pursivo.core.model.AiInsight
import io.github.arfaouiahmed1.pursivo.core.model.ApplicationEvent
import io.github.arfaouiahmed1.pursivo.core.model.ApplicationTask
import io.github.arfaouiahmed1.pursivo.core.model.CareerProfile
import io.github.arfaouiahmed1.pursivo.core.model.DraftPackage
import io.github.arfaouiahmed1.pursivo.core.model.FitAnalysis
import io.github.arfaouiahmed1.pursivo.core.model.JobApplication
import io.github.arfaouiahmed1.pursivo.core.model.PipelineStage
import io.github.arfaouiahmed1.pursivo.core.model.ProviderConfig
import io.github.arfaouiahmed1.pursivo.core.model.ProviderType
import io.github.arfaouiahmed1.pursivo.core.model.UpdateChannel
import io.github.arfaouiahmed1.pursivo.core.model.UpdateInfo
import io.github.arfaouiahmed1.pursivo.core.model.VaultDocument
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContentState(
    val applications: List<JobApplication>,
    val stages: List<PipelineStage>,
    val tasks: List<ApplicationTask>,
    val profile: CareerProfile?,
    val documents: List<VaultDocument>,
    val events: List<ApplicationEvent>,
)

data class PursivoUiState(
    val applications: List<JobApplication> = emptyList(),
    val stages: List<PipelineStage> = emptyList(),
    val tasks: List<ApplicationTask> = emptyList(),
    val events: List<ApplicationEvent> = emptyList(),
    val profile: CareerProfile? = null,
    val documents: List<VaultDocument> = emptyList(),
    val providers: List<ProviderConfig> = emptyList(),
    val insights: List<AiInsight> = emptyList(),
    val preferences: AppPreferenceState = AppPreferenceState(),
)

data class SharedDraft(val sourceUrl: String = "", val description: String = "")

sealed interface AiWorkState {
    data object Idle : AiWorkState
    data class Running(val message: String) : AiWorkState
    data class AnalysisReady(val analysis: FitAnalysis, val context: AiContext, val providerId: Long) : AiWorkState
    data class DraftReady(val analysis: FitAnalysis, val draft: DraftPackage, val insightId: Long) : AiWorkState
    data class Error(val message: String) : AiWorkState
}

@HiltViewModel
class PursivoViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val repository: PursivoRepository,
    private val preferences: AppPreferences,
    private val secretStore: SecretStore,
    private val documentVault: DocumentVault,
    private val providerFactory: ProviderClientFactory,
    private val insightEngine: InsightEngine,
    private val draftingEngine: DraftingEngine,
    private val backupService: BackupService,
    private val updateService: UpdateService,
) : ViewModel() {
    private val baseContent = combine(
        repository.applications,
        repository.stages,
        repository.tasks,
        repository.profile,
        repository.documents,
    ) { applications, stages, tasks, profile, documents ->
        ContentState(applications, stages, tasks, profile, documents, emptyList())
    }
    private val content = combine(baseContent, repository.events) { base, events -> base.copy(events = events) }

    val uiState: StateFlow<PursivoUiState> = combine(
        content,
        repository.providers,
        repository.insights,
        preferences.state,
    ) { content, providers, insights, preferenceState ->
        PursivoUiState(
            applications = content.applications,
            stages = content.stages,
            tasks = content.tasks,
            profile = content.profile,
            documents = content.documents,
            events = content.events,
            providers = providers,
            insights = insights,
            preferences = preferenceState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PursivoUiState())

    val sharedDraft = MutableStateFlow<SharedDraft?>(null)
    val aiWork = MutableStateFlow<AiWorkState>(AiWorkState.Idle)
    val updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val verifiedUpdate = MutableStateFlow<Uri?>(null)
    val dailyBrief = MutableStateFlow<String?>(null)
    val transientMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { repository.ensureSeeded() }
    }

    fun acceptSharedText(text: String) {
        val url = Regex("https?://\\S+").find(text)?.value?.trimEnd('.', ',', ')').orEmpty()
        sharedDraft.value = SharedDraft(sourceUrl = url, description = if (text == url) "" else text)
    }

    fun consumeSharedDraft() { sharedDraft.value = null }
    fun clearMessage() { transientMessage.value = null }

    fun completeOnboarding() = viewModelScope.launch { preferences.completeOnboarding() }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { preferences.setDynamicColor(enabled) }
    fun setTheme(theme: ThemePreference) = viewModelScope.launch { preferences.setTheme(theme) }
    fun setUpdateChannel(channel: UpdateChannel) = viewModelScope.launch { preferences.setUpdateChannel(channel) }

    fun saveApplication(application: JobApplication, taskTitle: String = "", reminderAt: Long? = null) = viewModelScope.launch {
        runCatching {
            require(application.company.isNotBlank() && application.role.isNotBlank()) { "Company and role are required." }
            val id = repository.saveApplication(application)
            if (taskTitle.isNotBlank()) {
                val taskId = repository.saveTask(ApplicationTask(
                    applicationId = id,
                    title = taskTitle,
                    dueAt = reminderAt,
                    reminderEnabled = reminderAt != null,
                ))
                reminderAt?.let { ReminderWorker.schedule(appContext, taskId, taskTitle, it) }
            }
        }.onSuccess {
            transientMessage.value = "Application saved"
            consumeSharedDraft()
        }.onFailure { transientMessage.value = it.message }
    }

    fun archiveApplication(application: JobApplication, archived: Boolean) = viewModelScope.launch {
        repository.archiveApplication(application, archived)
    }

    fun saveStage(stage: PipelineStage) = viewModelScope.launch { repository.saveStage(stage) }
    fun toggleTask(task: ApplicationTask) = viewModelScope.launch { repository.toggleTask(task) }
    fun saveProfile(profile: CareerProfile) = viewModelScope.launch {
        repository.saveProfile(profile)
        transientMessage.value = "Profile saved"
    }

    fun importDocument(uri: Uri) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { documentVault.import(uri) } }
            .onSuccess { repository.saveDocument(it); transientMessage.value = "${it.name} imported" }
            .onFailure { transientMessage.value = it.message ?: "Document import failed" }
    }

    fun updateDocumentText(document: VaultDocument, text: String) = viewModelScope.launch {
        repository.saveDocument(document.copy(normalizedText = text))
    }

    fun setActiveDocument(document: VaultDocument) = viewModelScope.launch {
        repository.setActiveDocument(document.id)
        transientMessage.value = "${document.name} is now the active CV"
    }

    fun addProvider(type: ProviderType, displayName: String, model: String, baseUrl: String, apiKey: String) = viewModelScope.launch {
        runCatching {
            require(apiKey.isNotBlank()) { "API key is required." }
            require(type != ProviderType.OPENAI_COMPATIBLE || baseUrl.startsWith("https://")) { "Custom endpoints must use HTTPS." }
            val alias = "provider-${UUID.randomUUID()}"
            secretStore.put(alias, apiKey)
            repository.saveProvider(ProviderConfig(
                type = type,
                displayName = displayName.ifBlank { type.name.lowercase().replaceFirstChar { it.uppercase() } },
                model = model,
                baseUrl = baseUrl,
                credentialAlias = alias,
            ))
        }.onSuccess { transientMessage.value = "Provider saved securely" }
            .onFailure { transientMessage.value = it.message }
    }

    fun deleteProvider(config: ProviderConfig) = viewModelScope.launch {
        secretStore.remove(config.credentialAlias)
        repository.deleteProvider(config.id)
    }

    fun deleteDocument(document: VaultDocument) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) { documentVault.delete(document) }
            repository.deleteDocument(document.id)
        }.onSuccess { transientMessage.value = "Document deleted" }
            .onFailure { transientMessage.value = it.message ?: "Document deletion failed" }
    }

    fun moveStage(stage: PipelineStage, direction: Int) = viewModelScope.launch {
        val ordered = uiState.value.stages.sortedBy { it.order }
        val currentIndex = ordered.indexOfFirst { it.id == stage.id }
        val targetIndex = currentIndex + direction
        if (currentIndex !in ordered.indices || targetIndex !in ordered.indices) return@launch
        val target = ordered[targetIndex]
        repository.saveStage(stage.copy(order = target.order))
        repository.saveStage(target.copy(order = stage.order))
    }

    fun testProvider(config: ProviderConfig) = viewModelScope.launch {
        aiWork.value = AiWorkState.Running("Testing ${config.displayName}…")
        val key = secretStore.get(config.credentialAlias)
        if (key == null) {
            aiWork.value = AiWorkState.Error("The stored key could not be decrypted.")
            return@launch
        }
        val result = runCatching { providerFactory.create(config, key).testConnection() }
            .getOrElse { io.github.arfaouiahmed1.pursivo.core.ai.ConnectionResult(false, it.message ?: "Connection failed") }
        aiWork.value = if (result.success) AiWorkState.Idle else AiWorkState.Error(result.message)
        transientMessage.value = result.message
    }

    fun analyze(applicationId: Long, providerId: Long, documentId: Long?) = viewModelScope.launch {
        val state = uiState.value
        val application = state.applications.firstOrNull { it.id == applicationId }
        val providerConfig = state.providers.firstOrNull { it.id == providerId }
        val profile = state.profile
        val document = state.documents.firstOrNull { it.id == documentId }
        if (application == null || providerConfig == null || profile == null) {
            aiWork.value = AiWorkState.Error("Choose an application and provider, then complete your profile.")
            return@launch
        }
        val key = secretStore.get(providerConfig.credentialAlias)
        if (key == null) {
            aiWork.value = AiWorkState.Error("The provider key is unavailable.")
            return@launch
        }
        val context = AiContext(application, profile, document)
        aiWork.value = AiWorkState.Running("Comparing verified evidence…")
        runCatching {
            insightEngine.analyze(providerFactory.create(providerConfig, key), context)
        }.onSuccess { analysis ->
            repository.saveInsight(AiInsight(
                applicationId = application.id,
                provider = providerConfig.type,
                model = providerConfig.model,
                promptVersion = PROMPT_VERSION,
                contextSummary = context.preview(),
                fitAnalysis = analysis,
            ))
            aiWork.value = AiWorkState.AnalysisReady(analysis, context, providerId)
        }.onFailure { aiWork.value = AiWorkState.Error(it.message ?: "Analysis failed") }
    }

    fun draftFromAnalysis() = viewModelScope.launch {
        val current = aiWork.value as? AiWorkState.AnalysisReady ?: return@launch
        val providerConfig = uiState.value.providers.firstOrNull { it.id == current.providerId } ?: return@launch
        val key = secretStore.get(providerConfig.credentialAlias) ?: return@launch
        aiWork.value = AiWorkState.Running("Drafting from verified facts…")
        runCatching {
            draftingEngine.draft(providerFactory.create(providerConfig, key), current.context, current.analysis)
        }.onSuccess { draft ->
            val insightId = repository.saveInsight(AiInsight(
                applicationId = current.context.application.id,
                provider = providerConfig.type,
                model = providerConfig.model,
                promptVersion = PROMPT_VERSION,
                contextSummary = current.context.preview(),
                fitAnalysis = current.analysis,
                draftPackage = draft,
            ))
            aiWork.value = AiWorkState.DraftReady(current.analysis, draft, insightId)
        }.onFailure { aiWork.value = AiWorkState.Error(it.message ?: "Drafting failed") }
    }

    fun resetAi() { aiWork.value = AiWorkState.Idle }

    fun acceptInsight(id: Long) = viewModelScope.launch { repository.acceptInsight(id) }
    fun deleteInsight(id: Long) = viewModelScope.launch { repository.deleteInsight(id) }

    fun generateDailyBrief() = viewModelScope.launch {
        val state = uiState.value
        val provider = state.providers.firstOrNull()
        val profile = state.profile
        if (provider == null || profile == null) {
            transientMessage.value = "Add an AI provider and profile first."
            return@launch
        }
        val key = secretStore.get(provider.credentialAlias)
        if (key == null) {
            transientMessage.value = "The provider key is unavailable."
            return@launch
        }
        val due = state.tasks.filterNot { it.completed }.sortedBy { it.dueAt ?: Long.MAX_VALUE }.take(8)
        val pipeline = state.applications.filterNot { it.archived }.take(20)
        aiWork.value = AiWorkState.Running("Preparing your brief…")
        runCatching {
            val raw = providerFactory.create(provider, key).generateJson(
                "You are Pursivo's concise daily coach. Use only supplied facts. Return JSON with one string field named brief. Never shame the user or invent deadlines.",
                buildString {
                    appendLine("User: ${profile.displayName}")
                    appendLine("Open tasks: ${due.joinToString { it.title }}")
                    appendLine("Applications: ${pipeline.joinToString { "${it.role} at ${it.company}; next=${it.nextAction}" }}")
                },
            )
            JSONObject(raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```")).getString("brief")
        }.onSuccess {
            dailyBrief.value = it
            aiWork.value = AiWorkState.Idle
        }.onFailure { aiWork.value = AiWorkState.Error(it.message ?: "Brief failed") }
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { backupService.export(uri) } }
            .onSuccess { transientMessage.value = "Backup exported. Provider keys were excluded." }
            .onFailure { transientMessage.value = it.message ?: "Backup failed" }
    }

    fun restoreBackup(uri: Uri) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { backupService.restore(uri) } }
            .onSuccess { transientMessage.value = "Backup restored" }
            .onFailure { transientMessage.value = it.message ?: "Restore failed" }
    }

    fun checkForUpdates(manual: Boolean = true) = viewModelScope.launch {
        val preference = uiState.value.preferences.update
        if (!manual && System.currentTimeMillis() - preference.lastCheckedAt < 86_400_000L) return@launch
        runCatching { updateService.check(BuildConfig.VERSION_NAME, preference.channel) }
            .onSuccess {
                preferences.recordUpdateCheck(System.currentTimeMillis())
                val visible = it?.takeUnless { update -> update.versionName == preference.dismissedVersion }
                updateInfo.value = visible
                if (manual && visible == null) transientMessage.value = "Pursivo is up to date"
            }
            .onFailure { if (manual) transientMessage.value = it.message ?: "Update check failed" }
    }

    fun downloadUpdate(update: UpdateInfo) = viewModelScope.launch {
        transientMessage.value = "Downloading and verifying Pursivo ${update.versionName}"
        runCatching { updateService.downloadAndVerify(update) }
            .onSuccess {
                verifiedUpdate.value = it
                updateInfo.value = null
            }
            .onFailure { transientMessage.value = it.message ?: "Update download failed" }
    }

    fun consumeVerifiedUpdate() { verifiedUpdate.value = null }

    fun dismissUpdate(version: String) = viewModelScope.launch {
        preferences.dismissVersion(version)
        updateInfo.value = null
    }
}
