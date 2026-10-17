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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * Служебный экран «Для разработчика».
 *
 * Сюда вынесена карточка настройки сервера (адрес + проверка связи), которая
 * раньше была на экране входа. Доступен без авторизации: адрес сервера задаётся
 * **до** входа, чтобы экран входа не был перегружен.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    onBack: () -> Unit,
    viewModel: DeveloperViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            QwantTopBar(
                title = stringResource(R.string.developer_title),
                subtitle = stringResource(R.string.developer_subtitle),
                onBack = onBack,
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
            SectionCard(title = stringResource(R.string.server_card_title)) {
                ServerStatusRow(checking = state.checkingServer, online = state.serverOnline)
                Text(
                    text = "${stringResource(R.string.server_address)}: ${state.baseUrl}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )

                if (!state.editing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::startEdit) {
                            Text(stringResource(R.string.server_change))
                        }
                        OutlinedButton(
                            onClick = viewModel::checkServer,
                            enabled = !state.checkingServer,
                        ) {
                            Text(stringResource(R.string.server_check))
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = state.addressInput,
                        onValueChange = viewModel::updateAddress,
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
                        TextButton(onClick = viewModel::cancelEdit) {
                            Text(stringResource(R.string.server_cancel))
                        }
                        Button(onClick = {
                            viewModel.save { url -> ServerConfig.save(context, url) }
                        }) {
                            Text(stringResource(R.string.server_save))
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.developer_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ServerStatusRow(checking: Boolean, online: Boolean) {
    val text = when {
        checking -> stringResource(R.string.server_checking)
        online -> stringResource(R.string.server_available)
        else -> stringResource(R.string.server_unavailable)
    }
    val color = when {
        checking -> MaterialTheme.colorScheme.onSurfaceVariant
        online -> SignalGreen
        else -> SignalRed
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "●", color = color, style = MaterialTheme.typography.titleMedium)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
