package io.github.arfaouiahmed1.pursivo.ui

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arfaouiahmed1.pursivo.core.designsystem.DockDestination
import io.github.arfaouiahmed1.pursivo.core.designsystem.PursivoDock
import io.github.arfaouiahmed1.pursivo.core.designsystem.PursivoTheme
import io.github.arfaouiahmed1.pursivo.core.designsystem.PursuitPath
import io.github.arfaouiahmed1.pursivo.core.data.ThemePreference
import io.github.arfaouiahmed1.pursivo.core.model.JobApplication
import io.github.arfaouiahmed1.pursivo.core.model.Priority
import io.github.arfaouiahmed1.pursivo.core.model.WorkMode
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun PursivoApp(viewModel: PursivoViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sharedDraft by viewModel.sharedDraft.collectAsStateWithLifecycle()
    val message by viewModel.transientMessage.collectAsStateWithLifecycle()
    val update by viewModel.updateInfo.collectAsStateWithLifecycle()
    val verifiedUpdate by viewModel.verifiedUpdate.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.preferences.onboardingComplete) {
        if (state.preferences.onboardingComplete) viewModel.checkForUpdates(manual = false)
    }

    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (state.preferences.theme) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    PursivoTheme(darkTheme = darkTheme, dynamicColor = state.preferences.dynamicColor) {
        if (!state.preferences.onboardingComplete) {
            OnboardingScreen(viewModel::completeOnboarding)
        } else {
            MainShell(state, viewModel, sharedDraft != null)
            sharedDraft?.let { draft ->
                AddApplicationDialog(state, draft, viewModel::consumeSharedDraft, viewModel::saveApplication)
            }
            update?.let { info ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUpdate(info.versionName) },
                    icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                    title = { Text("Pursivo ${info.versionName} is ready") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(info.notes.ifBlank { "A new ${if (info.prerelease) "prerelease" else "stable release"} is available." })
                            info.sizeBytes?.let { Text("Download size: ${formatBytes(it)}", style = MaterialTheme.typography.labelMedium) }
                            Text(if (info.sha256 == null) "This release cannot be installed in-app because its checksum is unavailable." else "The APK will be verified with SHA-256 before Android asks for installation approval.", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (info.apkUrl != null && info.sha256 != null) viewModel.downloadUpdate(info)
                            else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)))
                        }) { Text(if (info.apkUrl != null && info.sha256 != null) "Download & verify" else "View release") }
                    },
                    dismissButton = { TextButton(onClick = { viewModel.dismissUpdate(info.versionName) }) { Text("Later") } },
                )
            }
        }

        LaunchedEffect(verifiedUpdate) {
            verifiedUpdate?.let { uri ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
                    .onFailure { viewModel.transientMessage.value = "Android could not open the package installer" }
                viewModel.consumeVerifiedUpdate()
            }
        }

        LaunchedEffect(message) {
            message?.let {
                snackbar.showSnackbar(it)
                viewModel.clearMessage()
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            SnackbarHost(snackbar, modifier = Modifier.padding(bottom = 108.dp))
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

@Composable
private fun MainShell(state: PursivoUiState, viewModel: PursivoViewModel, addOpen: Boolean) {
    var destination by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    val destinations = remember {
        listOf(
            DockDestination("Today", Icons.Rounded.Today),
            DockDestination("Applications", Icons.Rounded.Work),
            DockDestination("Studio", Icons.Rounded.AutoAwesome),
            DockDestination("Profile", Icons.Rounded.Person),
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            PursivoDock(
                destinations = destinations,
                selectedIndex = destination,
                onSelect = { destination = it },
                onAdd = { showAdd = true },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                    initialOffsetX = { direction * it / 7 },
                ) + fadeIn()) togetherWith
                    (slideOutHorizontally(
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 520f),
                        targetOffsetX = { -direction * it / 10 },
                    ) + fadeOut())
            },
            label = "destination",
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                val screenModifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(padding)
                when (index) {
                    0 -> TodayScreen(state, viewModel, screenModifier)
                    1 -> ApplicationsScreen(state, viewModel, screenModifier)
                    2 -> StudioScreen(state, viewModel, screenModifier)
                    else -> ProfileScreen(state, viewModel, screenModifier)
                }
            }
        }
    }
    if (showAdd && !addOpen) {
        AddApplicationDialog(state, SharedDraft(), { showAdd = false }) { application, task, reminderAt ->
            viewModel.saveApplication(application, task, reminderAt)
            showAdd = false
        }
    }
}

@Composable
private fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple("Move every application forward", "Pursivo keeps opportunities, next actions, and documents together—without an account.", Icons.Rounded.AutoAwesome),
        Triple("Private by architecture", "Your tracker stays on this device. Nothing is sent unless you deliberately start an AI request.", Icons.Rounded.Lock),
        Triple("AI that asks permission", "Bring your own provider key, preview the exact context, and keep every suggestion as a draft until you approve it.", Icons.Rounded.CheckCircle),
    )
    Box(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.tertiaryContainer))),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("PURSiVO", letterSpacing = 4.sp, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            AnimatedContent(page, label = "onboardingPage") { targetPage ->
                val pageData = pages[targetPage]
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 22.dp, bottomEnd = 48.dp, bottomStart = 22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 4.dp,
                    ) {
                        Box(Modifier.size(152.dp).padding(15.dp), contentAlignment = Alignment.Center) {
                            PursuitPath(page + 1, pages.size, Modifier.fillMaxSize())
                            Surface(
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 10.dp, bottomEnd = 20.dp, bottomStart = 10.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Icon(pageData.third, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(13.dp).size(30.dp))
                            }
                        }
                    }
                    Text(pageData.first, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                    Text(pageData.second, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    pages.indices.forEach { index ->
                        Box(Modifier.padding(4.dp).size(if (index == page) 24.dp else 8.dp, 8.dp).background(if (index == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)))
                    }
                }
                Button(onClick = { if (page < pages.lastIndex) page++ else onFinish() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(if (page == pages.lastIndex) "Start locally" else "Continue")
                }
                if (page < pages.lastIndex) TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Skip setup") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddApplicationDialog(
    state: PursivoUiState,
    sharedDraft: SharedDraft,
    onDismiss: () -> Unit,
    onSave: (JobApplication, String, Long?) -> Unit,
) {
    var company by remember(sharedDraft) { mutableStateOf("") }
    var role by remember(sharedDraft) { mutableStateOf("") }
    var sourceUrl by remember(sharedDraft) { mutableStateOf(sharedDraft.sourceUrl) }
    var description by remember(sharedDraft) { mutableStateOf(sharedDraft.description) }
    var location by remember { mutableStateOf("") }
    var nextAction by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf("") }
    var reminderDate by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var workMode by remember { mutableStateOf(WorkMode.UNSPECIFIED) }
    var salary by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var stageId by remember(state.stages) { mutableStateOf(state.stages.firstOrNull()?.id ?: 0) }
    var expanded by remember { mutableStateOf(false) }
    val selectedStage = state.stages.firstOrNull { it.id == stageId }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val deadlineAt = parseLocalDate(deadlineDate)
    val reminderAt = parseLocalDate(reminderDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (sharedDraft.sourceUrl.isNotBlank() || sharedDraft.description.isNotBlank()) "Review shared opportunity" else "Add an opportunity") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(company, { company = it }, label = { Text("Company*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(role, { role = it }, label = { Text("Role*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                    OutlinedTextField(
                        selectedStage?.label.orEmpty(), {}, readOnly = true, label = { Text("Stage") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        state.stages.filterNot { it.hidden }.forEach { stage ->
                            DropdownMenuItem({ Text(stage.label) }, onClick = { stageId = stage.id; expanded = false })
                        }
                    }
                }
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { option ->
                        androidx.compose.material3.FilterChip(selected = priority == option, onClick = { priority = option }, label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(WorkMode.REMOTE, WorkMode.HYBRID, WorkMode.ONSITE).forEach { option ->
                        androidx.compose.material3.FilterChip(selected = workMode == option, onClick = { workMode = if (workMode == option) WorkMode.UNSPECIFIED else option }, label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
                OutlinedTextField(salary, { salary = it }, label = { Text("Salary or range") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma-separated") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sourceUrl, { sourceUrl = it }, label = { Text("Source URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Job description") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(deadlineDate, { deadlineDate = it }, label = { Text("Application deadline (YYYY-MM-DD)") }, isError = deadlineDate.isNotBlank() && deadlineAt == null, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(nextAction, { nextAction = it }, label = { Text("First next action") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reminderDate, { reminderDate = it }, label = { Text("Reminder date (YYYY-MM-DD)") }, enabled = nextAction.isNotBlank(), isError = reminderDate.isNotBlank() && reminderAt == null, modifier = Modifier.fillMaxWidth())
                Text("AI extraction stays optional. Review every field before saving.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reminderAt != null && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onSave(
                        JobApplication(
                            company = company,
                            role = role,
                            stageId = stageId,
                            sourceUrl = sourceUrl,
                            description = description,
                            location = location,
                            priority = priority,
                            workMode = workMode,
                            salary = salary,
                            tags = tags.split(',').map(String::trim).filter(String::isNotBlank).distinct(),
                            deadlineEpochMillis = deadlineAt,
                            nextAction = nextAction,
                        ),
                        nextAction,
                        reminderAt,
                    )
                },
                enabled = company.isNotBlank() && role.isNotBlank() && stageId != 0L && (deadlineDate.isBlank() || deadlineAt != null) && (reminderDate.isBlank() || reminderAt != null),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun parseLocalDate(value: String): Long? = value.trim().takeIf(String::isNotBlank)?.let { raw ->
    runCatching { LocalDate.parse(raw).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
}
