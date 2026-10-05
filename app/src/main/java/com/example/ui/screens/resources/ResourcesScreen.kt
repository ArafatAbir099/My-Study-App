package com.example.ui.screens.resources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.StudyNoteEntity
import com.example.data.local.StudyResourceEntity
import com.example.ui.viewmodel.PlannerViewModel

@Composable
fun ResourcesScreen(
    viewModel: PlannerViewModel
) {
    val resources by viewModel.resources.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("RESOURCES") } // "RESOURCES", "NOTES"
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var showAddResourceDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (activeTab == "RESOURCES") showAddResourceDialog = true else showAddNoteDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (activeTab == "RESOURCES") "Add Resource" else "New Note") },
                modifier = Modifier.testTag("fab_add_resource")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("resources_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library & Academic Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("RESOURCES", "NOTES").forEach { tab ->
                        FilterChip(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            label = { Text(tab.lowercase().capitalize(), fontSize = 11.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject Filter
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedSubjectId == null,
                        onClick = { selectedSubjectId = null },
                        label = { Text("All Subjects", fontSize = 11.sp) }
                    )
                }
                items(subjects) { s ->
                    FilterChip(
                        selected = selectedSubjectId == s.id,
                        onClick = { selectedSubjectId = s.id },
                        label = { Text(s.name, fontSize = 11.sp, maxLines = 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeTab == "RESOURCES") {
                val filteredRes = remember(resources, selectedSubjectId) {
                    if (selectedSubjectId != null) resources.filter { it.subjectId == selectedSubjectId } else resources
                }

                if (filteredRes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No resources uploaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredRes, key = { it.id }) { res ->
                            val sub = subjects.firstOrNull { it.id == res.subjectId }
                            val top = topics.firstOrNull { it.id == res.topicId }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledIconButton(
                                        onClick = {},
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (res.resourceType) {
                                                "PDF" -> Icons.Default.PictureAsPdf
                                                "LINK" -> Icons.Default.Link
                                                "SLIDES" -> Icons.Default.Slideshow
                                                else -> Icons.Default.Description
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(res.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "${res.resourceType} • ${sub?.name ?: "General"} ${if (top != null) "-> ${top.name}" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (res.urlOrContent.isNotEmpty()) {
                                            Text(
                                                text = res.urlOrContent,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    IconButton(onClick = { viewModel.deleteResource(res.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Notes View
                val filteredNotes = remember(notes, selectedSubjectId) {
                    if (selectedSubjectId != null) notes.filter { it.subjectId == selectedSubjectId } else notes
                }

                if (filteredNotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No study notes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            val sub = subjects.firstOrNull { it.id == note.subjectId }
                            val top = topics.firstOrNull { it.id == note.topicId }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            if (sub != null || top != null) {
                                                Text(
                                                    text = "${sub?.name ?: ""} ${if (top != null) "-> ${top.name}" else ""}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Resource Dialog
    if (showAddResourceDialog) {
        var title by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("PDF") }
        var content by remember { mutableStateOf("") }
        var subId by remember { mutableStateOf<Long?>(subjects.firstOrNull()?.id) }

        AlertDialog(
            onDismissRequest = { showAddResourceDialog = false },
            title = { Text("Add Study Resource", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Resource Title") }, modifier = Modifier.fillMaxWidth())
                    Text("Type:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PDF", "SLIDES", "DOC", "LINK", "NOTE").forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t, fontSize = 10.sp) })
                        }
                    }
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Link URL or Notes") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addResource(subId, null, title, type, content)
                            showAddResourceDialog = false
                        }
                    }
                ) { Text("Save Resource") }
            },
            dismissButton = {
                TextButton(onClick = { showAddResourceDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var subId by remember { mutableStateOf<Long?>(subjects.firstOrNull()?.id) }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Create Academic Note", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Note Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Notes / Formulas / Key takeaways") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addNote(subId, null, title, content)
                            showAddNoteDialog = false
                        }
                    }
                ) { Text("Save Note") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
