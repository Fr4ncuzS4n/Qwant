package com.example.qwant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.data.Electrolyzer
import com.example.qwant.data.OrderType
import com.example.qwant.data.Scheme
import com.example.qwant.ui.ErrorBox
import com.example.qwant.ui.LoadingBox
import com.example.qwant.ui.QwantTopBar
import com.example.qwant.ui.SectionCard
import com.example.qwant.ui.orderTypeTitle

/**
 * Постановка заявки диспетчером: тип заявки, электролизёр на схеме корпуса,
 * при необходимости — описание и заказчик.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onBack: () -> Unit,
    onCreated: (Int) -> Unit,
    viewModel: CreateOrderViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            QwantTopBar(
                title = stringResource(R.string.create_order_title),
                subtitle = stringResource(R.string.create_order_subtitle),
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

            else -> Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard(title = stringResource(R.string.order_type)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(OrderType.OBRABOTKA, OrderType.PODSYPKA, OrderType.REMONT).forEach { type ->
                            FilterChip(
                                selected = state.type == type,
                                onClick = { viewModel.selectType(type) },
                                label = { Text(orderTypeTitle(type)) },
                            )
                        }
                    }
                }

                SectionCard(title = stringResource(R.string.create_order_electrolyzer)) {
                    state.scheme?.let { scheme ->
                        RowSelector(
                            rows = scheme.rows,
                            selected = state.selectedRow,
                            onSelect = viewModel::selectRow,
                        )
                        ElectrolyzerGrid(
                            scheme = scheme,
                            row = state.selectedRow,
                            selectedId = state.electrolyzerId,
                            onSelect = viewModel::selectElectrolyzer,
                        )
                        Text(
                            text = state.electrolyzerId?.let {
                                stringResource(R.string.create_order_selected, it)
                            } ?: stringResource(R.string.create_order_select_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text(stringResource(R.string.create_order_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )

                OutlinedTextField(
                    value = state.requestedBy,
                    onValueChange = viewModel::updateRequestedBy,
                    label = { Text(stringResource(R.string.create_order_requested_by)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        viewModel.submit { result ->
                            result.onSuccess { order -> onCreated(order.id) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.electrolyzerId != null && !state.submitting,
                ) {
                    Text(
                        text = stringResource(R.string.create_order_submit),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowSelector(rows: Int, selected: Int, onSelect: (Int) -> Unit) {
    Text(
        text = stringResource(R.string.create_order_row),
        style = MaterialTheme.typography.labelLarge,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..rows).forEach { row ->
            FilterChip(
                selected = selected == row,
                onClick = { onSelect(row) },
                label = { Text(row.toString()) },
            )
        }
    }
}

/** Сетка номеров электролизёров выбранного ряда (по половинам A/B). */
@Composable
private fun ElectrolyzerGrid(
    scheme: Scheme,
    row: Int,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
) {
    val electrolyzers = scheme.electrolyzers.filter { it.row == row }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        electrolyzers.chunked(5).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                line.forEach { electrolyzer ->
                    ElectrolyzerCell(
                        electrolyzer = electrolyzer,
                        selected = electrolyzer.id == selectedId,
                        onClick = { onSelect(electrolyzer.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ElectrolyzerCell(
    electrolyzer: Electrolyzer,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = electrolyzer.number.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            Text(
                text = electrolyzer.half,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
    }
}
