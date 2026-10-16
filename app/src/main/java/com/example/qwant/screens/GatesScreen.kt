package com.example.qwant.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.qwant.data.Gate
import com.example.qwant.ui.ErrorBox
import com.example.qwant.ui.LoadingBox
import com.example.qwant.ui.QwantTopBar
import com.example.qwant.ui.SectionCard

/**
 * Дистанционное открытие ворот при выезде в корпус.
 * Ворота можно открыть/закрыть прямо с планшета.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatesScreen(
    onBack: () -> Unit,
    viewModel: GatesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            QwantTopBar(
                title = stringResource(R.string.gates_title),
                subtitle = stringResource(R.string.gate_hint),
                onBack = onBack,
            )
        }
    ) { innerPadding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(innerPadding))
            state.error != null -> ErrorBox(
                message = state.error!!,
                onRetry = viewModel::load,
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.gates, key = { it.id }) { gate ->
                    GateCard(
                        gate = gate,
                        busy = state.busyGateId == gate.id,
                        enabled = state.busyGateId == null,
                        onOpen = { viewModel.open(gate.id) },
                        onClose = { viewModel.close(gate.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GateCard(
    gate: Gate,
    busy: Boolean,
    enabled: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    SectionCard(title = gate.name) {
        Text(
            text = if (gate.isOpen) {
                stringResource(R.string.gate_opened)
            } else {
                stringResource(R.string.gate_closed)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (gate.isOpen) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = stringResource(R.string.gate_building, gate.building.orEmpty()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                enabled = enabled && !gate.isOpen,
            ) { Text(stringResource(R.string.gate_open)) }
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f),
                enabled = enabled && gate.isOpen,
            ) { Text(stringResource(R.string.gate_close)) }
        }
        if (busy) {
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
