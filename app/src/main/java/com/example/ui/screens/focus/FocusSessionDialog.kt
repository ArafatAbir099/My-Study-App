package com.example.ui.screens.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SubjectEntity
import com.example.data.local.TopicEntity
import kotlinx.coroutines.delay

@Composable
fun FocusSessionDialog(
    initialTopic: TopicEntity? = null,
    initialSubject: SubjectEntity? = null,
    onDismiss: () -> Unit,
    onFinishSession: (subjectId: Long?, topicId: Long?, title: String, minutes: Int, understanding: String?) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(30) }
    var secondsRemaining by remember { mutableIntStateOf(selectedMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }
    var showRatingStep by remember { mutableStateOf(false) }
    var selectedUnderstanding by remember { mutableStateOf<String?>("OKAY") }

    LaunchedEffect(isRunning) {
        while (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
            if (secondsRemaining <= 0) {
                isRunning = false
                showRatingStep = true
            }
        }
    }

    val displayMin = secondsRemaining / 60
    val displaySec = secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", displayMin, displaySec)
    val taskTitle = initialTopic?.name ?: initialSubject?.name ?: "Study Focus Session"

    AlertDialog(
        onDismissRequest = {
            if (!isRunning) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showRatingStep) "Session Completed!" else "Focus Session",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            if (!showRatingStep) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = taskTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!hasStarted) {
                        Text(
                            text = "Select Duration:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 45, 60).forEach { mins ->
                                FilterChip(
                                    selected = selectedMinutes == mins,
                                    onClick = {
                                        selectedMinutes = mins
                                        secondsRemaining = mins * 60
                                    },
                                    label = { Text("${mins}m") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Circular Timer Display
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeFormatted,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isRunning) "Focusing..." else if (hasStarted) "Paused" else "Ready",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Timer Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!hasStarted) {
                            Button(
                                onClick = {
                                    hasStarted = true
                                    isRunning = true
                                },
                                modifier = Modifier.testTag("start_focus_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start")
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { isRunning = !isRunning }
                            ) {
                                Icon(
                                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isRunning) "Pause" else "Resume")
                            }

                            Button(
                                onClick = {
                                    isRunning = false
                                    showRatingStep = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.testTag("finish_focus_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Finish")
                            }
                        }
                    }
                }
            } else {
                // Post-session understanding survey
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Great job finishing your study session! How was your understanding of this topic?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    listOf(
                        Triple("STRONG", "Strong - I have full grasp of the concepts", MaterialTheme.colorScheme.primary),
                        Triple("OKAY", "Okay - Understood core ideas, need regular revision", MaterialTheme.colorScheme.secondary),
                        Triple("WEAK", "Weak - Found parts difficult, need more practice", MaterialTheme.colorScheme.error)
                    ).forEach { (level, desc, color) ->
                        OutlinedCard(
                            onClick = { selectedUnderstanding = level },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = if (selectedUnderstanding == level) {
                                CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else CardDefaults.outlinedCardColors()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedUnderstanding == level,
                                    onClick = { selectedUnderstanding = level }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = level,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = color
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showRatingStep) {
                Button(
                    onClick = {
                        val minutesSpent = ((selectedMinutes * 60 - secondsRemaining) / 60).coerceAtLeast(1)
                        onFinishSession(
                            initialSubject?.id,
                            initialTopic?.id,
                            taskTitle,
                            minutesSpent,
                            selectedUnderstanding
                        )
                        onDismiss()
                    }
                ) {
                    Text("Save & Log Activity")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (showRatingStep) "Skip" else "Cancel")
            }
        }
    )
}
