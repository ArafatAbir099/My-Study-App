package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SubjectEntity
import com.example.data.local.TopicEntity
import com.example.ui.components.GlobalSearchDialog
import com.example.ui.components.QuickAddSheet
import com.example.ui.components.QuickAddType
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.exams.ExamsScreen
import com.example.ui.screens.focus.FocusSessionDialog
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.recommendation.WhatShouldIStudyDialog
import com.example.ui.screens.resources.ResourcesScreen
import com.example.ui.screens.revision.RevisionScreen
import com.example.ui.screens.semesters.SemestersScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.subjects.SubjectsScreen
import com.example.ui.viewmodel.PlannerViewModel

enum class NavigationSection(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    CALENDAR("Calendar", Icons.Default.CalendarMonth),
    SUBJECTS("Subjects", Icons.Default.MenuBook),
    REVISION("Revision", Icons.Default.Autorenew),
    EXAMS("Exams", Icons.Default.EventNote),
    PROGRESS("Progress", Icons.Default.TrendingUp),
    RESOURCES("Resources", Icons.Default.Folder),
    SEMESTERS("Semesters", Icons.Default.School),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: PlannerViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val recommendations by viewModel.studyRecommendations.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var currentSection by remember { mutableStateOf(NavigationSection.DASHBOARD) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showWhatShouldIStudyDialog by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    // Focus session dialog state
    var activeFocusSessionTopic by remember { mutableStateOf<TopicEntity?>(null) }
    var activeFocusSessionSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var showFocusDialog by remember { mutableStateOf(false) }

    // Handle system back button
    BackHandler(enabled = currentSection != NavigationSection.DASHBOARD) {
        currentSection = NavigationSection.DASHBOARD
    }

    if (currentUser == null) {
        AuthScreen(viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentSection.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Semester Study Planner",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Search Action
                    IconButton(
                        onClick = { showSearchDialog = true },
                        modifier = Modifier.testTag("action_search")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }

                    // Focus Session Launcher
                    IconButton(
                        onClick = {
                            activeFocusSessionTopic = null
                            activeFocusSessionSubject = null
                            showFocusDialog = true
                        },
                        modifier = Modifier.testTag("action_focus")
                    ) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = "Focus Mode", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Quick Add Action
                    IconButton(
                        onClick = { showQuickAddSheet = true },
                        modifier = Modifier.testTag("action_quick_add")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Quick Add", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val primaryNavItems = listOf(
                    NavigationSection.DASHBOARD,
                    NavigationSection.CALENDAR,
                    NavigationSection.SUBJECTS,
                    NavigationSection.REVISION
                )

                primaryNavItems.forEach { section ->
                    NavigationBarItem(
                        selected = currentSection == section,
                        onClick = {
                            if (currentSection != section) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentSection = section
                            }
                        },
                        icon = { Icon(section.icon, contentDescription = section.label) },
                        label = { Text(section.label, fontSize = 10.sp) },
                        modifier = Modifier.testTag("nav_${section.name.lowercase()}")
                    )
                }

                // More Menu Button
                NavigationBarItem(
                    selected = currentSection in listOf(
                        NavigationSection.EXAMS,
                        NavigationSection.PROGRESS,
                        NavigationSection.RESOURCES,
                        NavigationSection.SEMESTERS,
                        NavigationSection.SETTINGS
                    ),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showMoreSheet = true
                    },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                    label = { Text("More", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_more")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                     scaleIn(initialScale = 0.98f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    )
                },
                label = "screen_spring_transition"
            ) { section ->
                when (section) {
                    NavigationSection.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCalendar = { currentSection = NavigationSection.CALENDAR },
                        onNavigateToSubjects = { currentSection = NavigationSection.SUBJECTS },
                        onNavigateToRevision = { currentSection = NavigationSection.REVISION },
                        onNavigateToProgress = { currentSection = NavigationSection.PROGRESS },
                        onNavigateToExams = { currentSection = NavigationSection.EXAMS },
                        onOpenWhatShouldIStudy = { showWhatShouldIStudyDialog = true },
                        onStartFocus = { topic ->
                            activeFocusSessionTopic = topic
                            activeFocusSessionSubject = subjects.firstOrNull { it.id == topic.subjectId }
                            showFocusDialog = true
                        }
                    )
                    NavigationSection.CALENDAR -> CalendarScreen(
                        viewModel = viewModel,
                        onStartFocusSession = { title, duration, topicId, subjectId ->
                            activeFocusSessionTopic = topics.firstOrNull { it.id == topicId }
                            activeFocusSessionSubject = subjects.firstOrNull { it.id == subjectId }
                            showFocusDialog = true
                        }
                    )
                    NavigationSection.SUBJECTS -> SubjectsScreen(
                        viewModel = viewModel,
                        onStartFocus = { topic ->
                            activeFocusSessionTopic = topic
                            activeFocusSessionSubject = subjects.firstOrNull { it.id == topic.subjectId }
                            showFocusDialog = true
                        }
                    )
                    NavigationSection.REVISION -> RevisionScreen(viewModel = viewModel)
                    NavigationSection.EXAMS -> ExamsScreen(
                        viewModel = viewModel,
                        onNavigateToCalendar = { currentSection = NavigationSection.CALENDAR }
                    )
                    NavigationSection.PROGRESS -> ProgressScreen(viewModel = viewModel)
                    NavigationSection.RESOURCES -> ResourcesScreen(viewModel = viewModel)
                    NavigationSection.SEMESTERS -> SemestersScreen(viewModel = viewModel)
                    NavigationSection.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onLogout = { viewModel.logout() }
                    )
                }
            }
        }
    }

    // "More Sections" Modal Bottom Sheet
    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Additional Student OS Workspaces",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                listOf(
                    NavigationSection.EXAMS,
                    NavigationSection.PROGRESS,
                    NavigationSection.RESOURCES,
                    NavigationSection.SEMESTERS,
                    NavigationSection.SETTINGS
                ).forEach { sec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                currentSection = sec
                                showMoreSheet = false
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                currentSection = sec
                                showMoreSheet = false
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (currentSection == sec) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                sec.icon,
                                contentDescription = null,
                                tint = if (currentSection == sec) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = sec.label,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Global Search Dialog
    if (showSearchDialog) {
        GlobalSearchDialog(
            viewModel = viewModel,
            onDismiss = { showSearchDialog = false },
            onSelectTopic = {
                currentSection = NavigationSection.SUBJECTS
            }
        )
    }

    // Quick Add Sheet
    if (showQuickAddSheet) {
        QuickAddSheet(
            onDismiss = { showQuickAddSheet = false },
            onSelectType = { type ->
                when (type) {
                    QuickAddType.STUDY_TASK -> currentSection = NavigationSection.CALENDAR
                    QuickAddType.EXAM -> currentSection = NavigationSection.EXAMS
                    QuickAddType.SUBJECT, QuickAddType.TOPIC -> currentSection = NavigationSection.SUBJECTS
                    QuickAddType.REVISION -> currentSection = NavigationSection.REVISION
                    QuickAddType.NOTE, QuickAddType.RESOURCE -> currentSection = NavigationSection.RESOURCES
                }
            }
        )
    }

    // "What Should I Study?" Dialog
    if (showWhatShouldIStudyDialog) {
        WhatShouldIStudyDialog(
            recommendations = recommendations,
            onDismiss = { showWhatShouldIStudyDialog = false },
            onStartFocus = { topicId, topicName ->
                activeFocusSessionTopic = topics.firstOrNull { it.id == topicId }
                activeFocusSessionSubject = subjects.firstOrNull { it.id == activeFocusSessionTopic?.subjectId }
                showFocusDialog = true
            },
            onScheduleTask = { topicId, topicName ->
                currentSection = NavigationSection.CALENDAR
            }
        )
    }

    // Focus Session Dialog
    if (showFocusDialog) {
        FocusSessionDialog(
            initialTopic = activeFocusSessionTopic,
            initialSubject = activeFocusSessionSubject,
            onDismiss = { showFocusDialog = false },
            onFinishSession = { subId, topId, title, minutes, understanding ->
                viewModel.recordFocusSession(subId, topId, title, minutes, understanding)
            }
        )
    }
}
