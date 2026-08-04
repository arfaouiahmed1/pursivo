package io.github.arfaouiahmed1.pursivo.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.arfaouiahmed1.pursivo.core.data.local.PursivoDao
import io.github.arfaouiahmed1.pursivo.core.data.local.toEntity
import io.github.arfaouiahmed1.pursivo.core.data.local.toModel
import io.github.arfaouiahmed1.pursivo.core.model.PursivoBackup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface BackupService {
    suspend fun export(destination: Uri)
    suspend fun restore(source: Uri)
}

@Singleton
class ZipBackupService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: PursivoDao,
) : BackupService {
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    override suspend fun export(destination: Uri) {
        val snapshot = PursivoBackup(
            applications = dao.getApplications().map { it.toModel() },
            stages = dao.getStages().map { it.toModel() },
            events = dao.getEvents().map { it.toModel() },
            tasks = dao.getTasks().map { it.toModel() },
            profile = dao.getProfile()?.toModel(),
            documents = dao.getDocuments().map { it.toModel() },
            insights = dao.getInsights().map { it.toModel() },
            providerConfigs = dao.getProviderConfigs().map { it.toModel() },
        )
        context.contentResolver.openOutputStream(destination).use { output ->
            requireNotNull(output) { "Unable to open backup destination" }
            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("pursivo.json"))
                zip.write(json.encodeToString(snapshot).toByteArray())
                zip.closeEntry()
                snapshot.documents.forEach { document ->
                    val file = File(document.localPath)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry("documents/${document.id}-${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    override suspend fun restore(source: Uri) {
        val restoreDir = File(context.cacheDir, "restore-${System.currentTimeMillis()}").apply { mkdirs() }
        var snapshotJson: String? = null
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Unable to open backup" }
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "pursivo.json" -> snapshotJson = zip.readBytes().toString(Charsets.UTF_8)
                        entry.name.startsWith("documents/") && !entry.isDirectory -> {
                            val safeName = File(entry.name).name
                            File(restoreDir, safeName).outputStream().use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        val snapshot = json.decodeFromString<PursivoBackup>(requireNotNull(snapshotJson) { "Backup is missing pursivo.json" })
        require(snapshot.schemaVersion == 1) { "Unsupported backup schema ${snapshot.schemaVersion}" }

        val vault = File(context.filesDir, "vault").apply { mkdirs() }
        val restoredDocuments = snapshot.documents.map { document ->
            val restored = restoreDir.listFiles()?.firstOrNull { it.name.startsWith("${document.id}-") }
            if (restored != null) {
                val target = File(vault, restored.name.substringAfter('-'))
                restored.copyTo(target, overwrite = true)
                document.copy(localPath = target.absolutePath)
            } else document.copy(localPath = "")
        }

        dao.replaceAll(
            stages = snapshot.stages.map { it.toEntity() },
            applications = snapshot.applications.map { it.toEntity() },
            events = snapshot.events.map { it.toEntity() },
            tasks = snapshot.tasks.map { it.toEntity() },
            profile = snapshot.profile?.toEntity(),
            documents = restoredDocuments.map { it.toEntity() },
            providers = snapshot.providerConfigs.map { it.toEntity() },
            insights = snapshot.insights.map { it.toEntity() },
        )
        restoreDir.deleteRecursively()
    }
}
