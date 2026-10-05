package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class QuickAddType {
    STUDY_TASK,
    SUBJECT,
    TOPIC,
    PYQ,
    REVISION,
    NOTE,
    RESOURCE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onSelectType: (QuickAddType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Quick Add to Student OS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Capture items quickly into your semester workspace",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val options = listOf(
                Triple(QuickAddType.STUDY_TASK, "Study Task", Icons.Default.EventAvailable),
                Triple(QuickAddType.SUBJECT, "Subject / Course", Icons.Default.MenuBook),
                Triple(QuickAddType.TOPIC, "Syllabus Topic", Icons.Default.FormatListBulleted),
                Triple(QuickAddType.PYQ, "Previous Year Question (PYQ)", Icons.Default.Quiz),
                Triple(QuickAddType.REVISION, "Revision Milestone", Icons.Default.Autorenew),
                Triple(QuickAddType.NOTE, "Academic Note", Icons.Default.EditNote),
                Triple(QuickAddType.RESOURCE, "Lecture Resource / Link", Icons.Default.AttachFile)
            )

            options.forEach { (type, label, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectType(type)
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .testTag("quick_add_${type.name}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            onSelectType(type)
                            onDismiss()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
