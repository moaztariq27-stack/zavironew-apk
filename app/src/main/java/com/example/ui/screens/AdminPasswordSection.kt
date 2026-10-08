package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

@Composable
fun AdminChangePasswordDialog(
    viewModel: ZaviroViewModel,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    var localMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            viewModel.clearPasswordChangeFeedback()
            onDismiss()
        },
        containerColor = ZaviroSurfaceCard,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = "Change Password",
                    tint = ZaviroRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Change Admin Password",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("change_password_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Enter your current password and choose a new password for future Admin Panel logins.",
                    color = ZaviroTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        localMessage = null
                    },
                    label = { Text("Current Password", color = ZaviroTextMuted) },
                    singleLine = true,
                    visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showCurrent = !showCurrent }) {
                            Icon(
                                imageVector = if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = ZaviroTextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroBackground,
                        unfocusedContainerColor = ZaviroBackground,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_pass_current_input")
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        localMessage = null
                    },
                    label = { Text("New Password", color = ZaviroTextMuted) },
                    singleLine = true,
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(
                                imageVector = if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = ZaviroTextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroBackground,
                        unfocusedContainerColor = ZaviroBackground,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_pass_new_input")
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localMessage = null
                    },
                    label = { Text("Confirm New Password", color = ZaviroTextMuted) },
                    singleLine = true,
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showConfirm = !showConfirm }) {
                            Icon(
                                imageVector = if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = ZaviroTextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroBackground,
                        unfocusedContainerColor = ZaviroBackground,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_pass_confirm_input")
                )

                if (localMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSuccess) ZaviroGreen.copy(alpha = 0.12f) else ZaviroRed.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (isSuccess) ZaviroGreen else ZaviroRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(if (isSuccess) "change_pass_success_msg" else "change_pass_error_msg")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isSuccess) ZaviroGreen else ZaviroRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = localMessage ?: "",
                                color = if (isSuccess) ZaviroGreen else ZaviroRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.resetAdminPasswordToDefault(autoUnlock = false)
                        isSuccess = true
                        localMessage = "Admin password has been reset to default."
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                    },
                    border = BorderStroke(1.dp, ZaviroBurgundy.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroBurgundy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_reset_password_default_btn")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Password to Default", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.changeAdminPassword(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword
                    ) { success, msg ->
                        isSuccess = success
                        localMessage = msg
                        if (success) {
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZaviroRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("change_pass_submit_btn")
            ) {
                Text("Update Password", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.clearPasswordChangeFeedback()
                    onDismiss()
                },
                modifier = Modifier.testTag("change_pass_cancel_btn")
            ) {
                Text(if (isSuccess) "Done" else "Cancel", color = ZaviroTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun AdminChangePasswordSection(viewModel: ZaviroViewModel) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val feedbackMsg by viewModel.passwordChangeMessage.collectAsState()
    val isSuccess by viewModel.passwordChangeSuccess.collectAsState()

    val safeFeedbackMsg = remember(feedbackMsg) {
        feedbackMsg?.replace(Regex(":\\s*\\S+$"), "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("admin_password_settings_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
            border = BorderStroke(1.dp, ZaviroSurfaceBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = ZaviroRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Change Admin Password",
                            color = ZaviroTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Update your secure password for future Admin Panel logins",
                            color = ZaviroTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        viewModel.clearPasswordChangeFeedback()
                    },
                    label = { Text("Current Password", color = ZaviroTextMuted) },
                    singleLine = true,
                    visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showCurrent = !showCurrent }) {
                            Icon(
                                imageVector = if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = ZaviroTextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroBackground,
                        unfocusedContainerColor = ZaviroBackground,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_current_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        viewModel.clearPasswordChangeFeedback()
                    },
                    label = { Text("New Password", color = ZaviroTextMuted) },
                    singleLine = true,
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(
                                imageVector = if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = ZaviroTextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroBackground,
                        unfocusedContainerColor = ZaviroBackground,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_new_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        viewModel.clearPasswordChangeFeedback()
                    },
                    label = { Text("Confirm New Password", color = ZaviroTextMuted) },
                    singleLine = true,
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showConfirm = !showConfirm }) {
                            Icon(
                                imageVector = if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = ZaviroTextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroBackground,
                        unfocusedContainerColor = ZaviroBackground,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_confirm_password_input")
                )

                if (safeFeedbackMsg != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSuccess) ZaviroGreen.copy(alpha = 0.12f) else ZaviroRed.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (isSuccess) ZaviroGreen else ZaviroRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(if (isSuccess) "settings_pass_success_msg" else "settings_pass_error_msg")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isSuccess) ZaviroGreen else ZaviroRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = safeFeedbackMsg,
                                color = if (isSuccess) ZaviroGreen else ZaviroRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val updated = viewModel.changeAdminPassword(
                            currentPassword = currentPassword,
                            newPassword = newPassword,
                            confirmPassword = confirmPassword
                        )
                        if (updated) {
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZaviroRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_change_password_submit_btn")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save New Password", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.resetAdminPasswordToDefault(autoUnlock = false)
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                    },
                    border = BorderStroke(1.dp, ZaviroBurgundy.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroBurgundy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("settings_reset_password_default_btn")
                ) {
                    Icon(imageVector = Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Password to Default", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
