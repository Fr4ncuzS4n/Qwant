package com.example.qwant.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.data.Electrolyzer
import com.example.qwant.data.Gate
import com.example.qwant.data.Scheme
import com.example.qwant.data.Zone
import com.example.qwant.data.ZoneKind
import com.example.qwant.ui.ErrorBox
import com.example.qwant.ui.LoadingBox
import com.example.qwant.ui.QwantTopBar
import com.example.qwant.ui.SectionCard
import com.example.qwant.ui.theme.ElectrolyzerService
import com.example.qwant.ui.theme.SignalGreen
import com.example.qwant.ui.theme.ZoneDanger
import com.example.qwant.ui.theme.ZoneDeadEnd

/**
 * Схема корпуса №4: электролизёры по рядам (каждый ряд делится на половины A/B),
 * отмечены опасные зоны и тупики, ворота можно открыть дистанционно.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeScreen(
    onBack: () -> Unit = {},
    onOpenGates: () -> Unit = {},
    viewModel: SchemeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            QwantTopBar(
                title = stringResource(R.string.scheme_title),
                subtitle = stringResource(R.string.scheme_subtitle),
                onBack = onBack,
                actions = {
                    androidx.compose.material3.TextButton(onClick = onOpenGates) {
                        Text(stringResource(R.string.menu_gates))
                    }
                },
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

            else -> state.scheme?.let { scheme ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SchemeCanvas(
                        scheme = scheme,
                        selectedId = state.selectedElectrolyzerId,
                        onElectrolyzerTap = viewModel::selectElectrolyzer,
                        onGateTap = viewModel::toggleGate,
                    )

                    state.scheme?.electrolyzers?.firstOrNull {
                        it.id == state.selectedElectrolyzerId
                    }?.let { selected ->
                        SectionCard(title = stringResource(R.string.scheme_selected_title)) {
                            Text(
                                text = stringResource(
                                    R.string.scheme_selected_value,
                                    selected.number,
                                    selected.row,
                                    selected.half,
                                    selected.series.orEmpty(),
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = if (selected.status == "SERVICE") {
                                    stringResource(R.string.scheme_status_service)
                                } else {
                                    stringResource(R.string.scheme_status_ok)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    SectionCard(title = stringResource(R.string.scheme_gates_title)) {
                        scheme.gates.forEach { gate ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${gate.name} · " + if (gate.isOpen) {
                                        stringResource(R.string.gate_opened)
                                    } else {
                                        stringResource(R.string.gate_closed)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                androidx.compose.material3.Button(
                                    onClick = { viewModel.toggleGate(gate) },
                                    enabled = state.busyGateId == null,
                                ) {
                                    Text(
                                        if (gate.isOpen) {
                                            stringResource(R.string.gate_close)
                                        } else {
                                            stringResource(R.string.gate_open)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    SchemeLegend(scheme)
                }
            }
        }
    }
}

@Composable
private fun SchemeCanvas(
    scheme: Scheme,
    selectedId: Int?,
    onElectrolyzerTap: (Int) -> Unit,
    onGateTap: (Gate) -> Unit,
) {
    // Хит-тесты, заполняются при отрисовке и читаются обработчиком нажатий.
    val electrolyzerHitZones = remember { mutableListOf<Pair<Rect, Electrolyzer>>() }
    val gateHitZones = remember { mutableListOf<Pair<Rect, Gate>>() }
    val textMeasurer = rememberTextMeasurer()

    val okColor = MaterialTheme.colorScheme.surfaceVariant
    val serviceColor = ElectrolyzerService
    val selectedColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val cellTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gateOpenColor = SignalGreen
    val gateClosedColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .pointerInput(scheme) {
                detectTapGestures { offset ->
                    electrolyzerHitZones.firstOrNull { it.first.contains(offset) }
                        ?.let { onElectrolyzerTap(it.second.id) }
                        ?: gateHitZones.firstOrNull { it.first.contains(offset) }
                            ?.let { onGateTap(it.second) }
                }
            },
    ) {
        val padding = 20f
        val rows = scheme.rows.coerceAtLeast(1)
        val perRow = scheme.perRow.coerceAtLeast(1)
        val availableWidth = size.width - padding * 2
        val availableHeight = size.height - padding * 2
        val rowHeight = availableHeight / rows
        val cellWidth = availableWidth / perRow

        electrolyzerHitZones.clear()
        gateHitZones.clear()

        // Ряды электролизёров
        scheme.electrolyzers.forEach { electrolyzer ->
            val rowIndex = (electrolyzer.row - 1).coerceAtLeast(0)
            val positionIndex = (electrolyzer.position - 1).coerceAtLeast(0)
            val left = padding + positionIndex * cellWidth
            val top = padding + rowIndex * rowHeight
            val rect = Rect(
                offset = Offset(left + 1f, top + 2f),
                size = Size(cellWidth - 2f, rowHeight - 5f),
            )
            val color = when {
                electrolyzer.id == selectedId -> selectedColor
                electrolyzer.status == "SERVICE" -> serviceColor
                else -> okColor
            }
            drawRoundRect(
                color = color,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
            )
            if (cellWidth > 24f) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = electrolyzer.number.toString(),
                    topLeft = Offset(rect.left + 5f, rect.top + 4f),
                    style = TextStyle(
                        color = cellTextColor,
                        fontSize = (cellWidth / 3).coerceIn(8f, 16f).sp,
                    ),
                )
            }
            electrolyzerHitZones.add(rect to electrolyzer)
        }

        // Границы половин ряда (A/B)
        for (rowIndex in 0 until rows) {
            val y = padding + rowIndex * rowHeight
            drawLine(
                color = lineColor,
                start = Offset(padding, y),
                end = Offset(size.width - padding, y),
                strokeWidth = 1f,
            )
            val halfX = padding + availableWidth / 2f
            if (perRow % 2 == 0) {
                drawLine(
                    color = lineColor,
                    start = Offset(halfX, y),
                    end = Offset(halfX, y + rowHeight),
                    strokeWidth = 2f,
                )
            }
        }

        // Опасные зоны и тупики
        scheme.zones.forEach { zone ->
            val rect = Rect(
                offset = Offset(zone.x * size.width, zone.y * size.height),
                size = Size(zone.w * size.width, zone.h * size.height),
            )
            val color = if (zone.kind == ZoneKind.DANGER) ZoneDanger else ZoneDeadEnd
            drawRect(
                color = color.copy(alpha = 0.18f),
                topLeft = rect.topLeft,
                size = rect.size,
            )
            drawRect(
                color = color,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 2f),
            )
            if (rect.width > 70f) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = zone.label,
                    topLeft = Offset(rect.left + 6f, rect.top + 6f),
                    style = TextStyle(color = color, fontSize = 10.sp),
                )
            }
        }

        // Ворота
        scheme.gates.forEach { gate ->
            val center = Offset(gate.x * size.width, gate.y * size.height)
            val gateSize = 34f
            val rect = Rect(
                offset = Offset(center.x - gateSize / 2, center.y - gateSize / 2),
                size = Size(gateSize, gateSize),
            )
            drawRect(
                color = (if (gate.isOpen) gateOpenColor else gateClosedColor),
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 3f),
            )
            drawText(
                textMeasurer = textMeasurer,
                text = if (gate.isOpen) "O" else "X",
                topLeft = Offset(rect.left + 10f, rect.top + 8f),
                style = TextStyle(
                    color = if (gate.isOpen) gateOpenColor else gateClosedColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            gateHitZones.add(rect to gate)
        }
    }
}

@Composable
private fun SchemeLegend(scheme: Scheme) {
    SectionCard(title = stringResource(R.string.scheme_legend_title)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LegendItem(MaterialTheme.colorScheme.surfaceVariant, stringResource(R.string.scheme_status_ok))
            LegendItem(ElectrolyzerService, stringResource(R.string.scheme_status_service))
            LegendItem(ZoneDanger, stringResource(R.string.scheme_legend_danger))
            LegendItem(ZoneDeadEnd, stringResource(R.string.scheme_legend_dead_end))
            LegendItem(SignalGreen, stringResource(R.string.scheme_legend_gate_open))
        }
        Text(
            text = stringResource(
                R.string.scheme_summary,
                scheme.electrolyzerCount,
                scheme.rows,
                scheme.halfSize,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
