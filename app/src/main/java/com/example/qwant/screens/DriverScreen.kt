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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.data.Order
import com.example.qwant.ui.ErrorBox
import com.example.qwant.ui.LoadingBox
import com.example.qwant.ui.OrderCard
import com.example.qwant.ui.QwantTopBar
import com.example.qwant.ui.SectionCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Личный кабинет водителя.
 *
 * Ключевой UX кейса: минимум действий —
 * 1) крупное уведомление о новой заявке + кнопка «Взять в работу»;
 * 2) по завершении — кнопка «Выполнено» (при отклонении — один тап по тегу).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(
    driverId: Int,
    driverName: String,
    equipmentName: String?,
    onOpenOrder: (Int) -> Unit,
    onOpenScheme: () -> Unit,
    onOpenGates: () -> Unit,
    onOpenStats: () -> Unit,
    onChangeDriver: () -> Unit,
    viewModel: DriverViewModel = viewModel(key = "driver-$driverId") { DriverViewModel(driverId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val errorGeneric = stringResource(R.string.error_generic)

    var completingOrder by remember { mutableStateOf<Order?>(null) }
    var motohoursText by remember { mutableStateOf("") }

    // Периодический опрос сервера: время в работе и «светофор» считает сервер.
    LaunchedEffect(driverId) {
        while (true) {
            viewModel.refresh()
            delay(10_000)
        }
    }

    Scaffold(
        topBar = {
            QwantTopBar(
                title = stringResource(R.string.driver_cabinet_title),
                subtitle = listOfNotNull(driverName, equipmentName).joinToString(" · "),
                actions = {
                    TextButton(onClick = onChangeDriver) {
                        Text(stringResource(R.string.driver_change))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(innerPadding))
            state.error != null && state.newOrders.isEmpty() && state.myOrders.isEmpty() ->
                ErrorBox(
                    message = state.error!!,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(innerPadding),
                )

            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.driver_my_order),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (state.myOrders.isEmpty()) {
                    item {
                        SectionCard(title = null) {
                            Text(
                                text = stringResource(R.string.driver_no_active_order),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(state.myOrders, key = { "my-${it.id}" }) { order ->
                        OrderCard(
                            order = order,
                            large = true,
                            onClick = { onOpenOrder(order.id) },
                            actions = {
                                Button(
                                    onClick = {
                                        motohoursText = ""
                                        completingOrder = order
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.busy,
                                ) {
                                    Text(
                                        text = stringResource(R.string.order_complete),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            },
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.driver_new_orders),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (state.newOrders.isEmpty()) {
                    item {
                        SectionCard(title = null) {
                            Text(
                                text = stringResource(R.string.driver_no_new_orders),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    itemsIndexedNewOrders(state.newOrders) { index, order ->
                        OrderCard(
                            order = order,
                            large = index == 0,
                            onClick = { onOpenOrder(order.id) },
                            actions = {
                                Button(
                                    onClick = {
                                        viewModel.accept(order.id) { result ->
                                            result.onFailure { throwable ->
                                                scope.launch {
                                                    snackbar.showSnackbar(
                                                        throwable.message ?: errorGeneric
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.busy,
                                ) {
                                    Text(
                                        text = stringResource(R.string.order_take),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            },
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenScheme,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.menu_scheme)) }
                        OutlinedButton(
                            onClick = onOpenGates,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.menu_gates)) }
                        OutlinedButton(
                            onClick = onOpenStats,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.menu_stats)) }
                    }
                }
            }
        }
    }

    completingOrder?.let { order ->
        ModalBottomSheet(onDismissRequest = { completingOrder = null }) {
            CompleteOrderSheet(
                title = stringResource(R.string.order_complete),
                tags = state.tags.map { it.label },
                motohoursText = motohoursText,
                onMotohoursChange = { motohoursText = it },
                busy = state.busy,
                onNoDeviation = {
                    val hours = motohoursText.replace(',', '.').toDoubleOrNull()
                    viewModel.complete(order.id, tag = null, motohours = hours) { result ->
                        result.onSuccess { completingOrder = null }
                    }
                },
                onTag = { index ->
                    val tag = state.tags.getOrNull(index)
                    val hours = motohoursText.replace(',', '.').toDoubleOrNull()
                    viewModel.complete(order.id, tag = tag, motohours = hours) { result ->
                        result.onSuccess { completingOrder = null }
                    }
                },
            )
        }
    }
}

/** Разложение списка новых заявок на элементы LazyColumn. */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedNewOrders(
    orders: List<Order>,
    content: @Composable (Int, Order) -> Unit,
) {
    orders.forEachIndexed { index, order ->
        item(key = "new-${order.id}") { content(index, order) }
    }
}

@Composable
private fun CompleteOrderSheet(
    title: String,
    tags: List<String>,
    motohoursText: String,
    onMotohoursChange: (String) -> Unit,
    busy: Boolean,
    onNoDeviation: () -> Unit,
    onTag: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.order_deviation_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = motohoursText,
            onValueChange = onMotohoursChange,
            label = { Text(stringResource(R.string.order_motohours_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.order_quick_tags),
            style = MaterialTheme.typography.labelLarge,
        )
        tags.forEachIndexed { index, label ->
            FilterChip(
                selected = false,
                onClick = { if (!busy) onTag(index) },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = { if (!busy) onNoDeviation() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        ) {
            Text(stringResource(R.string.order_complete_no_deviation))
        }
    }
}
