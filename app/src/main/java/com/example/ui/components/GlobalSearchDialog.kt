package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.PlannerViewModel

@Composable
fun GlobalSearchDialog(
    viewModel: PlannerViewModel,
    onDismiss: () -> Unit,
    onSelectTopic: (Long) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val pyqs by viewModel.pyqs.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val resources by viewModel.resources.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    val matchedTopics = remember(topics, query) {
        if (query.length >= 2) topics.filter { it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) } else emptyList()
    }
    val matchedPyqs = remember(pyqs, query) {
        if (query.length >= 2) pyqs.filter { it.questionText.contains(query, ignoreCase = true) } else emptyList()
    }
    val matchedNotes = remember(notes, query) {
        if (query.length >= 2) notes.filter { it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) } else emptyList()
    }
    val matchedResources = remember(resources, query) {
        if (query.length >= 2) resources.filter { it.title.contains(query, ignoreCase = true) } else emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .testTag("global_search_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search topics, PYQs, notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (query.length < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Type at least 2 characters to search across your semester.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (matchedTopics.isEmpty() && matchedPyqs.isEmpty() && matchedNotes.isEmpty() && matchedResources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found for '$query'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Topics
                        if (matchedTopics.isNotEmpty()) {
                            item {
                                Text("Topics (${matchedTopics.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            items(matchedTopics) { t ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectTopic(t.id)
                                            onDismiss()
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(t.name, fontWeight = FontWeight.SemiBold)
                                        Text(t.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // PYQs
                        if (matchedPyqs.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("PYQs (${matchedPyqs.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                            items(matchedPyqs) { p ->
                                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("${p.examType} ${p.year}: ${p.questionText}", style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                    }
                                }
                            }
                        }

                        // Notes
                        if (matchedNotes.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Notes (${matchedNotes.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                            items(matchedNotes) { n ->
                                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(n.title, fontWeight = FontWeight.SemiBold)
                                        Text(n.content, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
