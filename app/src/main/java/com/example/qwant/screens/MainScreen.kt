package com.example.qwant.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.data.ServerConfig
import com.example.qwant.ui.QwantTopBar
import com.example.qwant.ui.SectionCard
import com.example.qwant.ui.theme.SignalGreen
import com.example.qwant.ui.theme.SignalRed

/**
 * Точка входа: выбор личного кабинета (водитель / диспетчер)
 * и настройка адреса сервера.
 */
@Composable
fun MainScreen(
    onOpenDriver: () -> Unit,
    onOpenDispatcher: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editingServer by rememberSaveable { mutableStateOf(false) }
    var address by rememberSaveable { mutableStateOf(ServerConfig.baseUrl) }

    Scaffold(
        topBar = {
            QwantTopBar(
                title = stringResource(R.string.role_title),
                subtitle = stringResource(R.string.role_subtitle),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RoleCard(
                title = stringResource(R.string.role_driver),
                description = stringResource(R.string.role_driver_desc),
                onClick = onOpenDriver,
            )
            RoleCard(
                title = stringResource(R.string.role_dispatcher),
                description = stringResource(R.string.role_dispatcher_desc),
                onClick = onOpenDispatcher,
            )

            SectionCard(title = stringResource(R.string.server_card_title)) {
                ServerStatusRow(state)
                Text(
                    text = "${stringResource(R.string.server_address)}: ${ServerConfig.baseUrl}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )

                if (!editingServer) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editingServer = true }) {
                            Text(stringResource(R.string.server_change))
                        }
                        OutlinedButton(
                            onClick = viewModel::check,
                            enabled = !state.checking,
                        ) {
                            Text(stringResource(R.string.server_check))
                        }
                    }
                    if (!state.online && !state.checking) {
                        Text(
                            text = stringResource(R.string.server_check_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(stringResource(R.string.server_address)) },
                        placeholder = { Text(stringResource(R.string.server_address_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.server_default_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            address = ServerConfig.baseUrl
                            editingServer = false
                        }) {
                            Text(stringResource(R.string.server_cancel))
                        }
                        Button(onClick = {
                            ServerConfig.save(context, address)
                            address = ServerConfig.baseUrl
                            editingServer = false
                            viewModel.check()
                        }) {
                            Text(stringResource(R.string.server_save))
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.role_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RoleCard(title: String, description: String, onClick: () -> Unit) {
    SectionCard(title = null, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.role_enter))
        }
    }
}

@Composable
private fun ServerStatusRow(state: MainUiState) {
    val text = when {
        state.checking -> stringResource(R.string.server_checking)
        state.online -> stringResource(R.string.server_available)
        else -> stringResource(R.string.server_unavailable)
    }
    val color = when {
        state.checking -> MaterialTheme.colorScheme.onSurfaceVariant
        state.online -> SignalGreen
        else -> SignalRed
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "●", color = color, style = MaterialTheme.typography.titleMedium)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}
