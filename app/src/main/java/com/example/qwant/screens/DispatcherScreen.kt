package com.example.qwant.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.ui.ErrorBox
import com.example.qwant.ui.LoadingBox
import com.example.qwant.ui.OrderCard
import com.example.qwant.ui.QwantTopBar
import kotlinx.coroutines.delay

/** Личный кабинет диспетчера: постановка заявок и контроль исполнения. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatcherScreen(
    dispatcherName: String,
    onCreateOrder: () -> Unit,
    onOpenOrder: (Int) -> Unit,
    onOpenScheme: () -> Unit,
    onOpenGates: () -> Unit,
    onOpenStats: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DispatcherViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Контроль времени исполнения: список обновляется, сервер пересчитывает «светофор».
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refresh()
            delay(10_000)
        }
    }

    Scaffold(
        topBar = {
            QwantTopBar(
                title = stringResource(R.string.dispatcher_title),
                subtitle = listOf(dispatcherName, stringResource(R.string.dispatcher_subtitle))
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                actions = {
                    TextButton(onClick = onCreateOrder) {
                        Text(stringResource(R.string.dispatcher_create))
                    }
                    TextButton(onClick = onLogout) {
                        Text(stringResource(R.string.logout))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DispatcherFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filterLabel(filter)) },
                    )
                }
            }

            when {
                state.loading -> LoadingBox()
                state.error != null -> ErrorBox(
                    message = state.error!!,
                    onRetry = { viewModel.refresh() },
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.orders.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.order_none),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(state.orders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            showDriver = true,
                            onClick = { onOpenOrder(order.id) },
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(onClick = onOpenScheme, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.menu_scheme))
                            }
                            OutlinedButton(onClick = onOpenGates, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.menu_gates))
                            }
                            OutlinedButton(onClick = onOpenStats, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.menu_stats))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun filterLabel(filter: DispatcherFilter) = when (filter) {
    DispatcherFilter.ALL -> stringResource(R.string.filter_all)
    DispatcherFilter.NEW -> stringResource(R.string.filter_new)
    DispatcherFilter.ACCEPTED -> stringResource(R.string.filter_accepted)
    DispatcherFilter.DONE -> stringResource(R.string.filter_done)
    DispatcherFilter.REPAIR -> stringResource(R.string.filter_repair)
}
