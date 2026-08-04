package io.github.arfaouiahmed1.pursivo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.arfaouiahmed1.pursivo.core.model.ApplicationEvent
import io.github.arfaouiahmed1.pursivo.core.model.JobApplication
import io.github.arfaouiahmed1.pursivo.core.model.PipelineStage
import io.github.arfaouiahmed1.pursivo.core.model.Priority
import io.github.arfaouiahmed1.pursivo.core.model.WorkMode
import java.text.DateFormat
import java.util.Date

@Composable
fun ApplicationsScreen(state: PursivoUiState, viewModel: PursivoViewModel, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var boardMode by remember { mutableStateOf(false) }
    var selectedStage by remember { mutableStateOf<Long?>(null) }
    var selectedPriority by remember { mutableStateOf<Priority?>(null) }
    var selectedWorkMode by remember { mutableStateOf<WorkMode?>(null) }
    var deadlineSoon by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var selectedApplication by remember { mutableStateOf<JobApplication?>(null) }
    val filtered = state.applications.filter { application ->
        (showArchived || !application.archived) &&
            (selectedStage == null || application.stageId == selectedStage) &&
            (selectedPriority == null || application.priority == selectedPriority) &&
            (selectedWorkMode == null || application.workMode == selectedWorkMode) &&
            (!deadlineSoon || application.deadlineEpochMillis?.let { it in System.currentTimeMillis()..(System.currentTimeMillis() + 30L * 86_400_000L) } == true) &&
            (query.isBlank() || listOf(application.company, application.role, application.location, application.tags.joinToString()).any { it.contains(query, true) })
    }

    Column(modifier.padding(top = 18.dp)) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Applications", style = MaterialTheme.typography.headlineLarge)
            Text("One place for every opportunity and next move.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                query, { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                label = { Text("Search roles, companies, tags") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!boardMode) Button({ boardMode = false }, Modifier.weight(1f)) { Icon(Icons.AutoMirrored.Rounded.List, null); Spacer(Modifier.width(6.dp)); Text("List") }
                else OutlinedButton({ boardMode = false }, Modifier.weight(1f)) { Icon(Icons.AutoMirrored.Rounded.List, null); Spacer(Modifier.width(6.dp)); Text("List") }
                if (boardMode) Button({ boardMode = true }, Modifier.weight(1f)) { Icon(Icons.Rounded.GridView, null); Spacer(Modifier.width(6.dp)); Text("Board") }
                else OutlinedButton({ boardMode = true }, Modifier.weight(1f)) { Icon(Icons.Rounded.GridView, null); Spacer(Modifier.width(6.dp)); Text("Board") }
            }
        }

        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selectedStage == null, { selectedStage = null }, { Text("All") }) }
            items(state.stages.filterNot { it.hidden }, key = { it.id }) { stage ->
                FilterChip(selectedStage == stage.id, { selectedStage = if (selectedStage == stage.id) null else stage.id }, { Text(stage.label) })
            }
            item { FilterChip(showArchived, { showArchived = !showArchived }, { Text("Archived") }, leadingIcon = { Icon(Icons.Rounded.Archive, null, Modifier.size(18.dp)) }) }
            Priority.entries.forEach { priority ->
                item { FilterChip(selectedPriority == priority, { selectedPriority = if (selectedPriority == priority) null else priority }, { Text(priority.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
            }
            listOf(WorkMode.REMOTE, WorkMode.HYBRID, WorkMode.ONSITE).forEach { mode ->
                item { FilterChip(selectedWorkMode == mode, { selectedWorkMode = if (selectedWorkMode == mode) null else mode }, { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
            }
            item { FilterChip(deadlineSoon, { deadlineSoon = !deadlineSoon }, { Text("Deadline in 30 days") }) }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(20.dp)) { CalmEmptyState("A clean slate", "Add an opportunity or share a job posting into Pursivo.") }
        } else if (boardMode) {
            StageBoard(filtered, state.stages, Modifier.weight(1f), onApplication = { selectedApplication = it })
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { application ->
                    ApplicationCard(application, state.stages.firstOrNull { it.id == application.stageId }) { selectedApplication = application }
                }
            }
        }
    }

    selectedApplication?.let { application ->
        ApplicationDetailSheet(
            application,
            state.stages,
            state.events.filter { it.applicationId == application.id },
            onDismiss = { selectedApplication = null },
            onUpdate = { updated -> viewModel.saveApplication(updated); selectedApplication = updated },
            onArchive = { viewModel.archiveApplication(application, !application.archived); selectedApplication = null },
        )
    }
}

@Composable
private fun StageBoard(
    applications: List<JobApplication>,
    stages: List<PipelineStage>,
    modifier: Modifier,
    onApplication: (JobApplication) -> Unit,
) {
    LazyRow(modifier = modifier, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 130.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(stages.filterNot { it.hidden }, key = { it.id }) { stage ->
            val stageApplications = applications.filter { it.stageId == stage.id }
            Surface(modifier = Modifier.width(286.dp).fillMaxHeight(), shape = MaterialTheme.shapes.large, color = Color(stage.colorArgb).copy(alpha = 0.10f)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stage.label, style = MaterialTheme.typography.titleMedium)
                        Surface(shape = CircleShape, color = Color(stage.colorArgb)) {
                            Text(stageApplications.size.toString(), Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    stageApplications.take(8).forEach { application -> CompactApplicationCard(application) { onApplication(application) } }
                    if (stageApplications.isEmpty()) Text("No applications", Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(application: JobApplication, stage: PipelineStage?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(stage?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.outline, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(application.role, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(application.company, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (application.nextAction.isNotBlank()) Text("Next · ${application.nextAction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                stage?.let { Text(it.label, style = MaterialTheme.typography.labelSmall, color = Color(it.colorArgb)) }
                if (application.priority == Priority.HIGH) Text("High", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CompactApplicationCard(application: JobApplication, onClick: () -> Unit) {
    Card(onClick = onClick, shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(application.role, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(application.company, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (application.nextAction.isNotBlank()) Text(application.nextAction, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationDetailSheet(
    application: JobApplication,
    stages: List<PipelineStage>,
    events: List<ApplicationEvent>,
    onDismiss: () -> Unit,
    onUpdate: (JobApplication) -> Unit,
    onArchive: () -> Unit,
) {
    var stageExpanded by remember { mutableStateOf(false) }
    var stageId by remember(application) { mutableStateOf(application.stageId) }
    var nextAction by remember(application) { mutableStateOf(application.nextAction) }
    var notes by remember(application) { mutableStateOf(application.notes) }
    val stage = stages.firstOrNull { it.id == stageId }
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(application.role, style = MaterialTheme.typography.headlineMedium)
                Text(application.company, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (application.location.isNotBlank()) Text(application.location, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                ExposedDropdownMenuBox(stageExpanded, { stageExpanded = !stageExpanded }) {
                    OutlinedTextField(
                        stage?.label.orEmpty(), {}, readOnly = true, label = { Text("Stage") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(stageExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(stageExpanded, { stageExpanded = false }) {
                        stages.filterNot { it.hidden }.forEach { item ->
                            DropdownMenuItem({ Text(item.label) }, onClick = { stageId = item.id; stageExpanded = false })
                        }
                    }
                }
            }
            item { OutlinedTextField(nextAction, { nextAction = it }, label = { Text("Next action") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            if (application.description.isNotBlank()) item {
                Column {
                    SectionHeading("Job description")
                    Spacer(Modifier.height(8.dp))
                    Text(application.description, maxLines = 10, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (application.sourceUrl.isNotBlank()) item {
                AssistChip(
                    onClick = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(application.sourceUrl))) },
                    label = { Text("Open source posting") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp)) },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button({ onUpdate(application.copy(stageId = stageId, nextAction = nextAction, notes = notes)) }, Modifier.weight(1f)) { Text("Save changes") }
                    OutlinedButton(onArchive, Modifier.weight(1f)) { Text(if (application.archived) "Restore" else "Archive") }
                }
            }
            item { SectionHeading("Timeline", "${events.size} events") }
            if (events.isEmpty()) item { Text("The timeline begins when this application changes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(events.sortedByDescending { it.createdAt }, key = { it.id }) { event ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 5.dp).size(9.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(event.title, style = MaterialTheme.typography.titleSmall)
                        if (event.detail.isNotBlank()) Text(event.detail, style = MaterialTheme.typography.bodySmall)
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(event.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
