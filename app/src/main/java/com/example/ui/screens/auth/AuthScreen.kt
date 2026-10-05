package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.PlannerViewModel

@Composable
fun AuthScreen(
    viewModel: PlannerViewModel
) {
    var isRegisterMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                )

                Text(
                    text = "Semester Study Planner",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (isRegisterMode) "Create your personal Student OS workspace" else "Sign in to your semester planner",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("auth_name")
                    )

                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        label = { Text("Student ID (e.g. 253-15-830)") },
                        modifier = Modifier.fillMaxWidth().testTag("auth_student_id")
                    )

                    OutlinedTextField(
                        value = university,
                        onValueChange = { university = it },
                        label = { Text("University / Institution") },
                        modifier = Modifier.fillMaxWidth().testTag("auth_university")
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("University Email") },
                    modifier = Modifier.fillMaxWidth().testTag("auth_email")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isRegisterMode) "Set Password (Optional)" else "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password")
                )

                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (isRegisterMode) {
                            if (email.isBlank()) {
                                isError = true
                                statusMessage = "Please enter your email address."
                                return@Button
                            }
                            viewModel.register(name, email, password, university, studentId) { success, msg ->
                                isError = !success
                                statusMessage = msg
                            }
                        } else {
                            if (email.isBlank()) {
                                isError = true
                                statusMessage = "Please enter your email."
                                return@Button
                            }
                            viewModel.login(email, password) { success, msg ->
                                isError = !success
                                statusMessage = msg
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("auth_submit_btn")
                ) {
                    Text(if (isRegisterMode) "Start My Study Planner" else "Sign In")
                }

                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        statusMessage = null
                    }
                ) {
                    Text(
                        text = if (isRegisterMode) "Already have an account? Sign in" else "New student? Create your Student OS",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
