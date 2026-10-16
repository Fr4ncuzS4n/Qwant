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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.data.Stats
import com.example.qwant.data.StatsRow
import com.example.qwant.ui.ErrorBox
import com.example.qwant.ui.InfoRow
import com.example.qwant.ui.LoadingBox
import com.example.qwant.ui.QwantTopBar
import com.example.qwant.ui.SectionCard
import com.example.qwant.ui.asMotohours

/** Статистика наработки оборудования в моточасах + сводка по заявкам. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            QwantTopBar(
                title = stringResource(R.string.stats_title),
                subtitle = stringResource(R.string.stats_subtitle),
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

            else -> state.stats?.let { stats ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SummaryCard(stats)
                    OrdersSummaryCard(stats)
                    SectionCard(title = stringResource(R.string.stats_by_equipment)) {
                        stats.rows.forEach { row -> EquipmentStatsRow(row) }
                        if (stats.rows.isEmpty()) {
                            Text(
                                text = stringResource(R.string.order_none),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(stats: Stats) {
    SectionCard(title = stringResource(R.string.stats_total_motohours)) {
        Text(
            text = stringResource(R.string.stats_motohours_short, stats.totalMotohours.asMotohours()),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        stats.byType.forEach { (type, hours) ->
            InfoRow(
                label = type,
                value = stringResource(R.string.stats_motohours_short, hours.asMotohours()),
            )
        }
    }
}

@Composable
private fun OrdersSummaryCard(stats: Stats) {
    SectionCard(title = stringResource(R.string.stats_orders_total)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stats.ordersTotal.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.stats_orders_breakdown,
                    stats.ordersNew,
                    stats.ordersAccepted,
                    stats.ordersDone,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EquipmentStatsRow(row: StatsRow) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = "${row.equipment.name} · ${row.equipment.model.orEmpty()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        InfoRow(
            stringResource(R.string.stats_motohours_label),
            stringResource(R.string.stats_motohours_short, row.equipment.motohours.asMotohours()),
        )
        InfoRow(
            stringResource(R.string.stats_completed_orders),
            row.completedOrders.toString(),
        )
        row.avgExecutionMinutes?.let {
            InfoRow(
                stringResource(R.string.stats_avg_execution),
                stringResource(R.string.urgency_minutes, it.toInt()),
            )
        }
    }
}
