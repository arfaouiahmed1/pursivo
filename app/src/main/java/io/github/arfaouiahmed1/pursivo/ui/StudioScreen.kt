package io.github.arfaouiahmed1.pursivo.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arfaouiahmed1.pursivo.core.ai.AiContext
import io.github.arfaouiahmed1.pursivo.core.model.DraftPackage
import io.github.arfaouiahmed1.pursivo.core.model.EvidenceItem
import io.github.arfaouiahmed1.pursivo.core.model.FitAnalysis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(state: PursivoUiState, viewModel: PursivoViewModel, modifier: Modifier = Modifier) {
    val work by viewModel.aiWork.collectAsStateWithLifecycle()
    var applicationId by remember(state.applications) { mutableStateOf(state.applications.firstOrNull { !it.archived }?.id) }
    var providerId by remember(state.providers) { mutableStateOf(state.providers.firstOrNull { it.enabled }?.id) }
    var documentId by remember(state.documents) { mutableStateOf(state.documents.firstOrNull { it.active }?.id ?: state.documents.firstOrNull()?.id) }
    var applicationMenu by remember { mutableStateOf(false) }
    var providerMenu by remember { mutableStateOf(false) }
    var documentMenu by remember { mutableStateOf(false) }
    var previewVisible by remember { mutableStateOf(false) }
    val application = state.applications.firstOrNull { it.id == applicationId }
    val provider = state.providers.firstOrNull { it.id == providerId }
    val document = state.documents.firstOrNull { it.id == documentId }
    val context = if (application != null && state.profile != null) AiContext(application, state.profile, document) else null

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Application Studio", style = MaterialTheme.typography.headlineLarge)
            Text("Fit, gaps, and documents—grounded in evidence you choose.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(applicationMenu, { applicationMenu = !applicationMenu }) {
                        OutlinedTextField(
                            application?.let { "${it.role} · ${it.company}" }.orEmpty(), {}, readOnly = true,
                            label = { Text("Application") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(applicationMenu) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(applicationMenu, { applicationMenu = false }) {
                            state.applications.filterNot { it.archived }.forEach { item ->
                                DropdownMenuItem({ Text("${item.role} · ${item.company}") }, onClick = { applicationId = item.id; applicationMenu = false; viewModel.resetAi() })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(providerMenu, { providerMenu = !providerMenu }) {
                        OutlinedTextField(
                            provider?.let { "${it.displayName} · ${it.model}" }.orEmpty(), {}, readOnly = true,
                            label = { Text("Provider and model") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerMenu) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(providerMenu, { providerMenu = false }) {
                            state.providers.filter { it.enabled }.forEach { item ->
                                DropdownMenuItem({ Text("${item.displayName} · ${item.model}") }, onClick = { providerId = item.id; providerMenu = false; viewModel.resetAi() })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(documentMenu, { documentMenu = !documentMenu }) {
                        OutlinedTextField(
                            document?.name ?: "No document", {}, readOnly = true,
                            label = { Text("Career document (optional)") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(documentMenu) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(documentMenu, { documentMenu = false }) {
                            DropdownMenuItem({ Text("No document") }, onClick = { documentId = null; documentMenu = false })
                            state.documents.forEach { item -> DropdownMenuItem({ Text(item.name) }, onClick = { documentId = item.id; documentMenu = false }) }
                        }
                    }
                }
            }
        }

        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Nothing is sent yet", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Analysis runs only after you review the selected context and tap Analyze.")
                    OutlinedButton(onClick = { previewVisible = !previewVisible }, enabled = context != null) {
                        Text(if (previewVisible) "Hide transmitted context" else "Preview transmitted context")
                    }
                    if (previewVisible && context != null) {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)) {
                            Text(context.preview(), Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.analyze(requireNotNull(applicationId), requireNotNull(providerId), documentId) },
                enabled = applicationId != null && providerId != null && state.profile != null && work !is AiWorkState.Running,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze fit and next action")
            }
            if (state.providers.isEmpty()) Text("Configure an AI provider in Profile. Tracking remains fully available without one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when (val current = work) {
            is AiWorkState.Running -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(current.message, color = MaterialTheme.colorScheme.primary)
                }
            }
            is AiWorkState.Error -> item {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text(current.message, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            is AiWorkState.AnalysisReady -> {
                item { FitResult(current.analysis) }
                item {
                    Button(onClick = viewModel::draftFromAnalysis, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        Text("Draft resume bullets and cover letter")
                    }
                }
            }
            is AiWorkState.DraftReady -> {
                item { FitResult(current.analysis) }
                item { DraftResult(current.draft) { viewModel.acceptInsight(current.insightId) } }
            }
            AiWorkState.Idle -> Unit
        }

        if (state.insights.isNotEmpty()) {
            item { SectionHeading("Saved insights", "${state.insights.size} local") }
            items(state.insights.take(8), key = { it.id }) { insight ->
                Card(shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${insight.fitAnalysis?.score ?: "–"} evidence fit", style = MaterialTheme.typography.titleMedium)
                            Text("${insight.provider.name.replace('_', ' ')} · ${insight.model}${if (insight.accepted) " · accepted" else " · draft"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.deleteInsight(insight.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete insight")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FitResult(analysis: FitAnalysis) {
    Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(76.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Text(analysis.score.toString(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Evidence fit", style = MaterialTheme.typography.titleLarge)
                    Text("Decision support, not a hiring prediction", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(analysis.summary)
            EvidenceSection("Strengths", analysis.strengths, MaterialTheme.colorScheme.secondary)
            EvidenceSection("Gaps", analysis.gaps, MaterialTheme.colorScheme.tertiary)
            if (analysis.missingInformation.isNotEmpty()) {
                Text("Missing information", style = MaterialTheme.typography.titleMedium)
                analysis.missingInformation.forEach { Text("• $it") }
            }
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.tertiaryContainer) {
                Column(Modifier.padding(14.dp)) {
                    Text("Recommended next action", style = MaterialTheme.typography.labelLarge)
                    Text(analysis.nextAction)
                }
            }
        }
    }
}

@Composable
private fun EvidenceSection(title: String, evidence: List<EvidenceItem>, color: Color) {
    if (evidence.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium)
    evidence.forEach { item ->
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 6.dp).size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(item.claim, fontWeight = FontWeight.SemiBold)
                Text(item.evidence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DraftResult(draft: DraftPackage, onAccept: () -> Unit) {
    val context = LocalContext.current
    Card(shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeading("Draft package", "Local draft")
            Text("Resume bullets", style = MaterialTheme.typography.titleMedium)
            draft.resumeBullets.forEach { Text("• $it") }
            Text("Cover letter", style = MaterialTheme.typography.titleMedium)
            Text(draft.coverLetter)
            if (draft.placeholders.isNotEmpty()) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Confirm before using", style = MaterialTheme.typography.labelLarge)
                        draft.placeholders.forEach { Text("• $it") }
                    }
                }
            }
            OutlinedButton(onClick = {
                val text = draft.resumeBullets.joinToString("\n", postfix = "\n\n${draft.coverLetter}") { "• $it" }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Pursivo draft", text))
                onAccept()
            }) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copy approved draft")
            }
        }
    }
}
