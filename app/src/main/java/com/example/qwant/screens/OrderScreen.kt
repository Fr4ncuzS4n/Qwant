package com.example.qwant.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.data.OrderStatus
import com.example.qwant.ui.ErrorBox
import com.example.qwant.ui.InfoRow
import com.example.qwant.ui.LoadingBox
import com.example.qwant.ui.OrderCard
import com.example.qwant.ui.QwantTopBar
import com.example.qwant.ui.SectionCard
import com.example.qwant.ui.asDateTime
import com.example.qwant.ui.asDuration

/** Карточка одной заявки: полная информация и контроль времени исполнения. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    orderId: Int,
    onBack: () -> Unit,
    viewModel: OrderViewModel = viewModel(key = "order-$orderId") { OrderViewModel(orderId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            QwantTopBar(
                title = stringResource(R.string.order_id, orderId),
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

            else -> state.order?.let { order ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OrderCard(order = order, large = true, showDriver = true)

                    SectionCard(title = stringResource(R.string.order_timeline)) {
                        order.createdAtMillis.takeIf { it > 0 }?.let {
                            InfoRow(stringResource(R.string.order_created), it.asDateTime())
                        }
                        order.acceptedAtMillis?.let {
                            InfoRow(stringResource(R.string.order_accepted), it.asDateTime())
                        }
                        order.elapsedMinutes?.let {
                            InfoRow(
                                stringResource(R.string.order_elapsed_label),
                                it.asDuration(),
                            )
                        }
                        if (order.status == OrderStatus.DONE) {
                            InfoRow(
                                stringResource(R.string.order_deviation),
                                order.deviationTagLabel ?: stringResource(R.string.order_no_deviation),
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = viewModel::load,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.refresh))
                    }
                }
            }
        }
    }
}
