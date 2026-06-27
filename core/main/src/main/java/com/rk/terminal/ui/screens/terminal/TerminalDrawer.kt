package com.rk.terminal.ui.screens.terminal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
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
    ModalDrawerSheet(modifier = Modifier.width(drawerWidth)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(strings.session),
                    style = MaterialTheme.typography.titleLarge
                )

                Row {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    IconButton(onClick = {
                        navController.navigate(MainActivityRoutes.Settings.route)
                        keyboardController?.hide()
                    }) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                    }

                    IconButton(onClick = onAddSession) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    }
                }
            }

            sessionBinder?.getService()?.sessionList?.keys?.toList()?.let { sessions ->
                LazyColumn {
                    items(sessions) { sessionId ->
                        val isSelected = sessionId == sessionBinder.getService().currentSession.value.first
                        SelectableCard(
                            selected = isSelected,
                            onSelect = { onSessionSelected(sessionId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = sessionId, style = MaterialTheme.typography.bodyLarge)

                                if (!isSelected) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { sessionBinder.terminateSession(sessionId) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
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
}

@Composable
fun SelectableCard(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "containerColor"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 8.dp else 2.dp
        ),
        enabled = enabled,
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}
