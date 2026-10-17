package com.example.qwant.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.data.OrderType
import com.example.qwant.ui.ErrorBox
import com.example.qwant.ui.HallLayout
import com.example.qwant.ui.HallSchemeCanvas
import com.example.qwant.ui.LoadingBox
import com.example.qwant.ui.QwantTopBar
import com.example.qwant.ui.SchemeFragmentControls
import com.example.qwant.ui.SectionCard
import com.example.qwant.ui.orderTypeTitle
import com.example.qwant.ui.rememberHallFormat

/**
 * Постановка заявки диспетчером: тип заявки, электролизёр на схеме корпуса
 * (тот же фрагмент цеха с проездами техники), описание и заказчик.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    requestedBy: String,
    onBack: () -> Unit,
    onCreated: (Int) -> Unit,
    viewModel: CreateOrderViewModel = viewModel(key = "create-order-$requestedBy") {
        CreateOrderViewModel(requestedBy)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val format = rememberHallFormat()

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
                    // Клавиатура не перекрывает поля ввода: прокрутка до клавиатуры.
                    .imePadding()
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
                        val layout = remember(scheme.rows, scheme.perRow, format) {
                            HallLayout.of(scheme, format)
                        }
                        val fragment = remember(layout) { layout.fullFragment() }
                        LaunchedEffect(layout) {
                            if (state.fragment == null) viewModel.showFragment(fragment)
                        }
                        HallSchemeCanvas(
                            scheme = scheme,
                            layout = layout,
                            fragment = fragment,
                            selectedId = state.electrolyzerId,
                            onElectrolyzerTap = viewModel::selectElectrolyzer,
                            onGateTap = { _ -> },
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
