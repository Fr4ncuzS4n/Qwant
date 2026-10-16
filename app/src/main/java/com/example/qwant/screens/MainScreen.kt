package com.example.qwant.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.ui.QwantTopBar
import com.example.qwant.ui.SectionCard
import com.example.qwant.ui.theme.SignalGreen
import com.example.qwant.ui.theme.SignalRed

/**
 * Точка входа: выбор личного кабинета.
 * По ТЗ кейса есть два кабинета — водителя (заявки: принять/выполнить) и
 * диспетчера (постановка заявок и контроль).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenDriver: () -> Unit,
    onOpenDispatcher: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            ServerStatusRow(state)
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
        androidx.compose.material3.Button(
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
