package com.example.qwant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qwant.R
import com.example.qwant.data.OrderStatus
import com.example.qwant.data.OrderType
import com.example.qwant.data.Urgency
import com.example.qwant.ui.theme.SignalGreen
import com.example.qwant.ui.theme.SignalOrange
import com.example.qwant.ui.theme.SignalRed
import com.example.qwant.ui.theme.StatusAccepted
import com.example.qwant.ui.theme.StatusDone
import com.example.qwant.ui.theme.StatusNew

/** Шапка экрана с крупным заголовком, подписью и (опционально) кнопкой «Назад». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QwantTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        actions = actions,
    )
}

/** Русское название типа заявки. */
@Composable
fun orderTypeTitle(type: String): String = when (type) {
    OrderType.PODSYPKA -> stringResource(R.string.type_podsypka)
    OrderType.REMONT -> stringResource(R.string.type_remont)
    else -> stringResource(R.string.type_obrabotka)
}

/** Русское название статуса заявки. */
@Composable
fun orderStatusTitle(status: String): String = when (status) {
    OrderStatus.ACCEPTED -> stringResource(R.string.status_accepted)
    OrderStatus.DONE -> stringResource(R.string.status_done)
    OrderStatus.CANCELLED -> stringResource(R.string.status_cancelled)
    else -> stringResource(R.string.status_new)
}

@Composable
private fun statusColor(status: String): Color = when (status) {
    OrderStatus.ACCEPTED -> StatusAccepted
    OrderStatus.DONE -> StatusDone
    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.outline
    else -> StatusNew
}

private fun urgencyColor(urgency: String?): Color? = when (urgency) {
    Urgency.GREEN -> SignalGreen
    Urgency.ORANGE -> SignalOrange
    Urgency.RED -> SignalRed
    else -> null
}

private fun urgencyTitleRes(urgency: String): Int = when (urgency) {
    Urgency.ORANGE -> R.string.urgency_orange
    Urgency.RED -> R.string.urgency_red
    else -> R.string.urgency_green
}

/** Цветной «бейдж» с текстом. */
@Composable
fun ChipBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        contentColor = color,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    ChipBadge(text = orderStatusTitle(status), color = statusColor(status), modifier = modifier)
}

/**
 * Индикатор срочности («светофор») для заявок МРГ-подсыпки.
 * Показывается только когда сервер вернул `urgency`.
 */
@Composable
fun UrgencyChip(urgency: String?, elapsedMinutes: Int?, modifier: Modifier = Modifier) {
    val u = urgency ?: return
    val color = urgencyColor(u) ?: return
    val title = stringResource(urgencyTitleRes(u))

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .background(color, RoundedCornerShape(50))
                .padding(6.dp)
        )
        Text(
            text = if (elapsedMinutes != null) {
                "$title · ${stringResource(R.string.urgency_minutes, elapsedMinutes)}"
            } else {
                title
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/** Строка «подпись — значение» для карточек заявки/техники. */
@Composable
fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Индикатор загрузки на весь экран. */
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Экран ошибки с кнопкой «Повторить». */
@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.error_prefix, message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        if (onRetry != null) {
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

/** Блок с заголовком и содержимым (используется в списках). */
@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            content()
        }
    }
}

/** Прокручиваемый экран-заглушка для простых сообщений. */
@Composable
fun MessageBox(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
