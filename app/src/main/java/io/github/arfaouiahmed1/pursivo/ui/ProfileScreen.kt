package io.github.arfaouiahmed1.pursivo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.arfaouiahmed1.pursivo.core.data.ThemePreference
import io.github.arfaouiahmed1.pursivo.core.model.CareerProfile
import io.github.arfaouiahmed1.pursivo.core.model.PipelineStage
import io.github.arfaouiahmed1.pursivo.core.model.ProviderConfig
import io.github.arfaouiahmed1.pursivo.core.model.ProviderType
import io.github.arfaouiahmed1.pursivo.core.model.SemanticStage
import io.github.arfaouiahmed1.pursivo.core.model.UpdateChannel
import io.github.arfaouiahmed1.pursivo.core.model.VaultDocument

@Composable
fun ProfileScreen(state: PursivoUiState, viewModel: PursivoViewModel, modifier: Modifier = Modifier) {
    var editProfile by remember { mutableStateOf(false) }
    var addProvider by remember { mutableStateOf(false) }
    var editDocument by remember { mutableStateOf<VaultDocument?>(null) }
    var editStage by remember { mutableStateOf<PipelineStage?>(null) }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importDocument)
    }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::restoreBackup)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.headlineLarge)
            Text("Your evidence, preferences, and private workspace.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            ProfileHero(state.profile, onEdit = { editProfile = true })
        }

        item { SectionHeading("Document vault", "${state.documents.size} local") }
        if (state.documents.isEmpty()) {
            item { CalmEmptyState("Bring your evidence", "Import a PDF, DOCX, or TXT file. Pursivo stores it in app-private storage and lets you correct extracted text.") }
        } else {
            items(state.documents, key = { it.id }) { document ->
                DocumentCard(document, onEdit = { editDocument = document }, onActivate = { viewModel.setActiveDocument(document) }, onDelete = { viewModel.deleteDocument(document) })
            }
        }
        item {
            OutlinedButton(
                onClick = { documentPicker.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain")) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import a document")
            }
        }

        item { SectionHeading("AI providers", "Bring your own key") }
        item {
            PrivacyCard()
        }
        items(state.providers, key = { it.id }) { provider ->
            ProviderCard(provider, onTest = { viewModel.testProvider(provider) }, onDelete = { viewModel.deleteProvider(provider) })
        }
        item {
            Button(onClick = { addProvider = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Key, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add provider")
            }
        }

        item { SectionHeading("Pipeline", "Semantics stay fixed") }
        items(state.stages.sortedBy { it.order }, key = { it.id }) { stage ->
            StageRow(
                stage = stage,
                onEdit = { editStage = stage },
                onUp = { viewModel.moveStage(stage, -1) },
                onDown = { viewModel.moveStage(stage, 1) },
            )
        }
        item {
            OutlinedButton(onClick = { editStage = PipelineStage(label = "", order = state.stages.size, colorArgb = stageColors.first(), semanticKind = SemanticStage.SAVED) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add custom stage")
            }
        }

        item { SectionHeading("Appearance & updates") }
        item {
            SettingsCard {
                SettingSwitch(
                    icon = Icons.Rounded.Palette,
                    title = "Dynamic Material You color",
                    detail = "Use your device palette on Android 12+.",
                    checked = state.preferences.dynamicColor,
                    onChecked = viewModel::setDynamicColor,
                )
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreference.entries.forEach { theme ->
                        FilterChip(
                            selected = state.preferences.theme == theme,
                            onClick = { viewModel.setTheme(theme) },
                            label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                Text("Update channel", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.preferences.update.channel == UpdateChannel.STABLE,
                        onClick = { viewModel.setUpdateChannel(UpdateChannel.STABLE) },
                        label = { Text("Stable") },
                    )
                    FilterChip(
                        selected = state.preferences.update.channel == UpdateChannel.PRERELEASE,
                        onClick = { viewModel.setUpdateChannel(UpdateChannel.PRERELEASE) },
                        label = { Text("Prerelease") },
                    )
                }
                OutlinedButton(onClick = { viewModel.checkForUpdates() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Check for updates")
                }
            }
        }

        item { SectionHeading("Backup & restore", "Keys excluded") }
        item {
            SettingsCard {
                Text("A versioned ZIP contains your database records and document files. Provider credentials are never exported.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportBackup.launch("pursivo-backup-v1.zip") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Backup, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Export")
                    }
                    OutlinedButton(onClick = { restoreBackup.launch(arrayOf("application/zip", "application/octet-stream")) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Restore")
                    }
                }
            }
        }

        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Local-first by default", style = MaterialTheme.typography.titleMedium)
                        Text("No account, analytics, ads, background AI, or hidden sync.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (editProfile) ProfileDialog(state.profile ?: CareerProfile(), { editProfile = false }) {
        viewModel.saveProfile(it)
        editProfile = false
    }
    if (addProvider) ProviderDialog({ addProvider = false }) { type, name, model, baseUrl, key ->
        viewModel.addProvider(type, name, model, baseUrl, key)
        addProvider = false
    }
    editDocument?.let { document ->
        DocumentTextDialog(document, { editDocument = null }) {
            viewModel.updateDocumentText(document, it)
            editDocument = null
        }
    }
    editStage?.let { stage ->
        StageDialog(stage, { editStage = null }) {
            viewModel.saveStage(it)
            editStage = null
        }
    }
}

@Composable
private fun ProfileHero(profile: CareerProfile?, onEdit: () -> Unit) {
    Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile?.displayName?.ifBlank { "Build your evidence profile" } ?: "Build your evidence profile", style = MaterialTheme.typography.titleLarge)
                    Text(profile?.headline?.ifBlank { "Used only when you choose to draft or analyze" } ?: "Used only when you choose to draft or analyze", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit profile") }
            }
            if (!profile?.skills.isNullOrEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    profile.skills.take(3).forEach { AssistChip(onClick = onEdit, label = { Text(it, maxLines = 1) }) }
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(document: VaultDocument, onEdit: () -> Unit, onActivate: () -> Unit, onDelete: () -> Unit) {
    Card(shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.padding(11.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(document.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("v${document.version} · ${document.normalizedText.length} extracted characters${if (document.active) " · active CV" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!document.active) TextButton(onClick = onActivate) { Text("Use") }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Review extracted text") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete document") }
        }
    }
}

@Composable
private fun ProviderCard(provider: ProviderConfig, onTest: () -> Unit, onDelete: () -> Unit) {
    Card(shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(provider.displayName, style = MaterialTheme.typography.titleMedium)
                Text("${provider.type.name.replace('_', ' ')} · ${provider.model}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onTest) { Text("Test") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete provider") }
        }
    }
}

@Composable
private fun PrivacyCard() {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Security, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Keys are encrypted on this device", style = MaterialTheme.typography.titleMedium)
                Text("Pursivo shows the selected evidence before every request. Keys and request payloads are excluded from logs and backups.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StageRow(stage: PipelineStage, onEdit: () -> Unit, onUp: () -> Unit, onDown: () -> Unit) {
    Card(shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).background(Color(stage.colorArgb), CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stage.label, style = MaterialTheme.typography.titleMedium)
                Text("${stage.semanticKind.name.lowercase()}${if (stage.hidden) " · hidden" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onUp) { Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up") }
            IconButton(onClick = onDown) { Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down") }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit stage") }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
private fun SettingSwitch(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ProfileDialog(profile: CareerProfile, onDismiss: () -> Unit, onSave: (CareerProfile) -> Unit) {
    var name by remember { mutableStateOf(profile.displayName) }
    var headline by remember { mutableStateOf(profile.headline) }
    var location by remember { mutableStateOf(profile.location) }
    var roles by remember { mutableStateOf(profile.targetRoles.joinToString(", ")) }
    var skills by remember { mutableStateOf(profile.skills.joinToString(", ")) }
    var preferences by remember { mutableStateOf(profile.preferences) }
    var achievements by remember { mutableStateOf(profile.achievements) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
        title = { Text("Career evidence profile") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(headline, { headline = it }, label = { Text("Professional headline") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(roles, { roles = it }, label = { Text("Target roles, comma-separated") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(skills, { skills = it }, label = { Text("Skills, comma-separated") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(preferences, { preferences = it }, label = { Text("Work preferences") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(achievements, { achievements = it }, label = { Text("Verified work history & achievements") }, minLines = 5, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(profile.copy(
                    displayName = name.trim(), headline = headline.trim(), location = location.trim(),
                    targetRoles = csv(roles), skills = csv(skills), preferences = preferences.trim(), achievements = achievements.trim(),
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ProviderDialog(onDismiss: () -> Unit, onSave: (ProviderType, String, String, String, String) -> Unit) {
    var type by remember { mutableStateOf(ProviderType.OPENAI) }
    var typeMenu by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("OpenAI") }
    var model by remember { mutableStateOf("gpt-5.6-terra") }
    var baseUrl by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    fun defaults(next: ProviderType) {
        type = next
        name = when (next) { ProviderType.OPENAI -> "OpenAI"; ProviderType.ANTHROPIC -> "Anthropic"; ProviderType.GEMINI -> "Gemini"; ProviderType.OPENAI_COMPATIBLE -> "Custom provider" }
        model = when (next) { ProviderType.OPENAI -> "gpt-5.6-terra"; ProviderType.ANTHROPIC -> "claude-sonnet-4-5"; ProviderType.GEMINI -> "gemini-2.5-flash"; ProviderType.OPENAI_COMPATIBLE -> "" }
        baseUrl = ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
        title = { Text("Add AI provider") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(type.name.replace('_', ' ')) }
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        ProviderType.entries.forEach { option -> DropdownMenuItem(text = { Text(option.name.replace('_', ' ')) }, onClick = { defaults(option); typeMenu = false }) }
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(model, { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
                if (type == ProviderType.OPENAI_COMPATIBLE) OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("HTTPS base URL") }, placeholder = { Text("https://example.com/v1") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(key, { key = it }, label = { Text("API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Text("The key is encrypted with Android Keystore and cannot be exported.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { onSave(type, name, model, baseUrl, key) }, enabled = key.isNotBlank() && model.isNotBlank()) { Text("Save securely") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DocumentTextDialog(document: VaultDocument, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember(document) { mutableStateOf(document.normalizedText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review extracted text") },
        text = { OutlinedTextField(text, { text = it }, label = { Text(document.name) }, minLines = 12, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { onSave(text) }) { Text("Save text") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val stageColors = listOf(0xFF526581, 0xFF315BA8, 0xFF2F765A, 0xFFFFBA39, 0xFF6E4BA3, 0xFF8C4A4A)

@Composable
private fun StageDialog(stage: PipelineStage, onDismiss: () -> Unit, onSave: (PipelineStage) -> Unit) {
    var label by remember(stage) { mutableStateOf(stage.label) }
    var semantic by remember(stage) { mutableStateOf(stage.semanticKind) }
    var semanticMenu by remember { mutableStateOf(false) }
    var color by remember(stage) { mutableStateOf(stage.colorArgb) }
    var hidden by remember(stage) { mutableStateOf(stage.hidden) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Route, contentDescription = null) },
        title = { Text(if (stage.id == 0L) "Add pipeline stage" else "Edit pipeline stage") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(label, { label = it }, label = { Text("Stage label") }, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { if (stage.id == 0L) semanticMenu = true }, enabled = stage.id == 0L, modifier = Modifier.fillMaxWidth()) { Text("Analytics category: ${semantic.name}") }
                    if (stage.id == 0L) DropdownMenu(expanded = semanticMenu, onDismissRequest = { semanticMenu = false }) {
                        SemanticStage.entries.forEach { option -> DropdownMenuItem(text = { Text(option.name) }, onClick = { semantic = option; semanticMenu = false }) }
                    }
                }
                Text("The analytics category is stored separately from the label, so renaming this stage never changes metrics.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    stageColors.forEach { option ->
                        IconButton(onClick = { color = option }) {
                            Box(Modifier.size(if (color == option) 30.dp else 24.dp).background(Color(option), CircleShape))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Hide from active pipeline", modifier = Modifier.weight(1f))
                    Switch(checked = hidden, onCheckedChange = { hidden = it })
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(stage.copy(label = label.trim(), semanticKind = semantic, colorArgb = color, hidden = hidden)) }, enabled = label.isNotBlank()) { Text("Save stage") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun csv(value: String): List<String> = value.split(',').map(String::trim).filter(String::isNotBlank).distinct()
