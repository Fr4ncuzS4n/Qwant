package com.example.qwant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qwant.R
import com.example.qwant.data.Order
import com.example.qwant.data.OrderStatus

/**
 * Карточка заявки, общая для кабинетов водителя и диспетчера.
 *
 * @param large крупный вариант — используется для всплывающего уведомления водителю.
 * @param showDriver показывать исполнителя (кабинет диспетчера).
 * @param actions кнопки действий внизу карточки.
 */
@Composable
fun OrderCard(
    order: Order,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    showDriver: Boolean = false,
    onClick: (() -> Unit)? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val titleStyle = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.order_id, order.id),
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                )
                StatusChip(order.status)
            }

            Text(
                text = orderTypeTitle(order.type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            if (large && order.status == OrderStatus.NEW) {
                Text(
                    text = stringResource(R.string.order_new_notification),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = order.place(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )

            InfoRow(stringResource(R.string.order_row), order.row?.toString())
            InfoRow(
                stringResource(R.string.order_half),
                order.half?.let { stringResource(R.string.order_half_value, it) },
            )
            order.description?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            if (showDriver) {
                InfoRow(stringResource(R.string.order_driver), order.driverName)
            }
            InfoRow(stringResource(R.string.order_requested_by), order.requestedBy)

            if (order.status == OrderStatus.ACCEPTED) {
                UrgencyChip(order.urgency, order.elapsedMinutes)
                if (order.urgency == null && order.elapsedMinutes != null) {
                    Text(
                        text = stringResource(R.string.order_elapsed, order.elapsedMinutes.asDuration()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (order.status == OrderStatus.DONE) {
                InfoRow(stringResource(R.string.order_deviation), order.deviationTagLabel)
                InfoRow(stringResource(R.string.order_comment), order.comment)
                order.motohoursDelta?.takeIf { it > 0 }?.let {
                    InfoRow(
                        stringResource(R.string.order_motohours_done),
                        stringResource(R.string.stats_motohours_short, it.asMotohours()),
                    )
                }
            }

            if (order.createdAtMillis > 0) {
                InfoRow(stringResource(R.string.order_created), order.createdAtMillis.asTime())
            }

            actions()
        }
    }

    if (onClick != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
            elevation = CardDefaults.cardElevation(defaultElevation = if (large) 6.dp else 2.dp),
            content = { content() },
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = if (large) 6.dp else 2.dp),
            content = { content() },
        )
    }
}
