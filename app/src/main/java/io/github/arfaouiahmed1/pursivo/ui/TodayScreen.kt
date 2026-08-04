package io.github.arfaouiahmed1.pursivo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arfaouiahmed1.pursivo.core.designsystem.MetricPill
import io.github.arfaouiahmed1.pursivo.core.designsystem.PursivoBlue
import io.github.arfaouiahmed1.pursivo.core.designsystem.PursivoGreen
import io.github.arfaouiahmed1.pursivo.core.designsystem.PursivoSaffron
import io.github.arfaouiahmed1.pursivo.core.designsystem.PursuitPath
import io.github.arfaouiahmed1.pursivo.core.model.ApplicationTask
import io.github.arfaouiahmed1.pursivo.core.model.SemanticStage
import java.text.DateFormat
import java.util.Date

@Composable
fun TodayScreen(state: PursivoUiState, viewModel: PursivoViewModel, modifier: Modifier = Modifier) {
    val brief by viewModel.dailyBrief.collectAsStateWithLifecycle()
    val activeTasks = state.tasks.filterNot { it.completed }.sortedWith(compareBy<ApplicationTask> { it.dueAt ?: Long.MAX_VALUE }.thenBy { it.createdAt })
    val completed = state.tasks.count { it.completed }
    val sevenDaysAgo = System.currentTimeMillis() - 7 * 86_400_000L
    val stageMap = state.stages.associateBy { it.id }
    val recentApplications = state.applications.count { it.createdAt >= sevenDaysAgo && stageMap[it.stageId]?.semanticKind == SemanticStage.APPLIED }
    val interviews = state.applications.count { stageMap[it.stageId]?.semanticKind == SemanticStage.INTERVIEWING && !it.archived }
    val responses = state.applications.count {
        stageMap[it.stageId]?.semanticKind in setOf(SemanticStage.SCREENING, SemanticStage.INTERVIEWING, SemanticStage.OFFER)
    }
    val firstName = state.profile?.displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("TODAY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                    Text(if (firstName == null) "Keep the path moving" else "Forward, $firstName", style = MaterialTheme.typography.headlineLarge)
                }
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 18.dp, bottomEnd = 34.dp, bottomStart = 18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    PursuitPath(
                        completed = completed,
                        total = completed + activeTasks.size,
                        modifier = Modifier.size(118.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Your pursuit path", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            if (activeTasks.isEmpty()) "Clear for now. Save the next move when it appears."
                            else "${activeTasks.size} next ${if (activeTasks.size == 1) "move" else "moves"} ready for attention.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill(recentApplications.toString(), "sent", PursivoBlue, Modifier.weight(1f))
                MetricPill(responses.toString(), "responses", PursivoGreen, Modifier.weight(1f))
                MetricPill(interviews.toString(), "interviews", PursivoSaffron, Modifier.weight(1f))
            }
        }

        item { SectionHeading("Next actions", if (activeTasks.isEmpty()) "Nothing overdue" else "${activeTasks.size} open") }
        if (activeTasks.isEmpty()) {
            item { CalmEmptyState("No pressure here", "Add a next action from an application when you know the right move.") }
        } else {
            items(activeTasks.take(5), key = { it.id }) { task -> TaskCard(task, viewModel::toggleTask) }
        }

        item { SectionHeading("AI brief", "On demand") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text("A grounded plan for today", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        brief ?: "Pursivo can summarize your open tasks and pipeline using your first configured provider. It runs only when you tap below.",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Button(onClick = viewModel::generateDailyBrief, enabled = state.providers.isNotEmpty()) {
                        Text(if (brief == null) "Generate brief" else "Refresh brief")
                    }
                }
            }
        }

        if (interviews > 0) {
            item { SectionHeading("In motion", "$interviews interviewing") }
            items(state.applications.filter { stageMap[it.stageId]?.semanticKind == SemanticStage.INTERVIEWING }.take(3), key = { it.id }) { application ->
                Card(shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(16.dp)) {
                        Text(application.role, style = MaterialTheme.typography.titleMedium)
                        Text(application.company, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: ApplicationTask, onToggle: (ApplicationTask) -> Unit) {
    val overdue = task.dueAt?.let { it < System.currentTimeMillis() } == true
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle(task) },
        colors = CardDefaults.cardColors(containerColor = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, contentDescription = "Complete", tint = Color.White) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                task.dueAt?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(
                            (if (overdue) "Overdue · " else "") + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionHeading(title: String, detail: String = "") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun CalmEmptyState(title: String, detail: String) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
