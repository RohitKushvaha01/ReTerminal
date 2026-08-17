package com.rk.terminal.ui.screens.terminal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rk.resources.strings
import com.rk.terminal.service.SessionService
import com.rk.terminal.ui.routes.MainActivityRoutes

@Composable
fun TerminalDrawer(
    drawerWidth: androidx.compose.ui.unit.Dp,
    sessionBinder: SessionService.SessionBinder?,
    navController: NavController,
    onAddSession: () -> Unit,
    onSessionSelected: (String) -> Unit
) {
    var sessionToRename by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier.width(drawerWidth),
        drawerContainerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(strings.session),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val keyboardController = LocalSoftwareKeyboardController.current

                    // Sort menu button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = stringResource(strings.sort_sessions)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(strings.sort_az)) },
                                onClick = {
                                    showSortMenu = false
                                    sessionBinder?.sortSessions(ascending = true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(strings.sort_za)) },
                                onClick = {
                                    showSortMenu = false
                                    sessionBinder?.sortSessions(ascending = false)
                                }
                            )
                        }
                    }

                    // Settings button
                    IconButton(onClick = {
                        navController.navigate(MainActivityRoutes.Settings.route)
                        keyboardController?.hide()
                    }) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                    }

                    // Add Session button
                    IconButton(onClick = onAddSession) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    }
                }
            }

            val service = sessionBinder?.getService()
            val sessions: List<String> = if (service != null && service.sessionOrder.isNotEmpty()) {
                service.sessionOrder.toList()
            } else {
                service?.sessionList?.keys?.toList() ?: emptyList()
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = sessions,
                    key = { _, sessionId -> sessionId }
                ) { index, sessionId ->
                    val isSelected = sessionId == service?.currentSession?.value?.first
                    SelectableCard(
                        selected = isSelected,
                        onSelect = { onSessionSelected(sessionId) },
                        onLongClick = { sessionToRename = sessionId },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sessionId,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Move Up
                                if (index > 0) {
                                    IconButton(
                                        onClick = { sessionBinder?.moveSession(index, index - 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = stringResource(strings.move_up),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Move Down
                                if (index < sessions.size - 1) {
                                    IconButton(
                                        onClick = { sessionBinder?.moveSession(index, index + 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = stringResource(strings.move_down),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Rename Button
                                IconButton(
                                    onClick = { sessionToRename = sessionId },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(strings.rename),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete Button (if not only session)
                                if (!isSelected || sessions.size > 1) {
                                    IconButton(
                                        onClick = { sessionBinder?.terminateSession(sessionId) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Session Dialog
    sessionToRename?.let { currentName ->
        var newName by remember { mutableStateOf(currentName) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val alreadyExistsText = stringResource(strings.name_already_exists)
        val cannotBeEmptyText = stringResource(strings.name_cannot_be_empty)

        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text(text = stringResource(strings.rename_session)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            errorMessage = null
                        },
                        label = { Text(stringResource(strings.session_name)) },
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isEmpty()) {
                            errorMessage = cannotBeEmptyText
                        } else if (trimmed != currentName && sessionBinder?.getService()?.sessionList?.containsKey(trimmed) == true) {
                            errorMessage = alreadyExistsText
                        } else {
                            sessionBinder?.renameSession(currentName, trimmed)
                            sessionToRename = null
                        }
                    }
                ) {
                    Text(stringResource(strings.rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text(stringResource(strings.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableCard(
    selected: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "containerColor"
    )

    Surface(
        modifier = modifier.combinedClickable(
            onClick = onSelect,
            onLongClick = onLongClick,
            enabled = enabled
        ),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            content()
        }
    }
}
