package com.example.qwant.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qwant.R
import com.example.qwant.data.Electrolyzer
import com.example.qwant.data.Gate
import com.example.qwant.data.Scheme
import com.example.qwant.data.ZoneKind
import com.example.qwant.ui.theme.ElectrolyzerService
import com.example.qwant.ui.theme.LaneDrive
import com.example.qwant.ui.theme.SignalGreen
import com.example.qwant.ui.theme.ZoneDanger
import com.example.qwant.ui.theme.ZoneDeadEnd

/**
 * Схема корпуса, вписанная в доступную ширину, но **не мельче** ячейки
 * [HallLayout.MIN_CELL_DP]: на узких экранах (телефон) схема не сжимается,
 * а прокручивается по горизонтали. Само рисование — в [HallSchemeDrawing].
 */
@Composable
fun HallSchemeCanvas(
    scheme: Scheme,
    layout: HallLayout,
    fragment: SchemeFragment,
    selectedId: Int?,
    onElectrolyzerTap: (Int) -> Unit,
    onGateTap: (Gate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = layout.viewBox(fragment)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val unit = layout.unitDp(maxWidth.value, view.w).dp
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            HallSchemeDrawing(
                scheme = scheme,
                layout = layout,
                fragment = fragment,
                selectedId = selectedId,
                onElectrolyzerTap = onElectrolyzerTap,
                onGateTap = onGateTap,
                modifier = Modifier.size(width = unit * view.w, height = unit * view.h),
            )
        }
    }
}

/**
 * Отрисовка схемы корпуса: две линии электролизёров, блоки и проходы между ними,
 * широкая **центральная дорога** между линиями, дороги вдоль стен и единственный
 * вход (слева или справа); поверх — опасные зоны, тупики и ворота.
 *
 * По умолчанию рисуется весь корпус целиком ([HallLayout.fullFragment]); при
 * передаче меньшего [SchemeFragment] схема показывает лишь его часть. Тяжёлые
 * детали — в [HallLayout].
 */
@Composable
private fun HallSchemeDrawing(
    scheme: Scheme,
    layout: HallLayout,
    fragment: SchemeFragment,
    selectedId: Int?,
    onElectrolyzerTap: (Int) -> Unit,
    onGateTap: (Gate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val electrolyzerHitZones = remember { mutableListOf<Pair<Rect, Electrolyzer>>() }
    val gateHitZones = remember { mutableListOf<Pair<Rect, Gate>>() }
    val textMeasurer = rememberTextMeasurer()

    val view = layout.viewBox(fragment)

    val okColor = MaterialTheme.colorScheme.surfaceVariant
    val serviceColor = ElectrolyzerService
    val selectedColor = MaterialTheme.colorScheme.primary
    val cellTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedTextColor = MaterialTheme.colorScheme.onPrimary
    val laneColor = LaneDrive
    val roadFill = laneColor.copy(alpha = 0.20f)
    val roadMark = laneColor.copy(alpha = 0.55f)
    val laneTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val blockColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    val wallColor = MaterialTheme.colorScheme.outline
    val gateOpenColor = SignalGreen
    val gateClosedColor = MaterialTheme.colorScheme.outline
    val turnLabel = stringResource(R.string.scheme_lane_turn)
    val sideLaneLabel = stringResource(R.string.scheme_lane_side)
    val aisleLabel = stringResource(R.string.scheme_lane_aisle)
    val entranceLabel = stringResource(R.string.scheme_entrance)

    // Единственный вход: сторона определяется первыми воротами корпуса.
    val entranceGate = scheme.gates.firstOrNull()
    val entranceSide = entranceGate?.let { layout.entranceSide(it.x) }

    val fragmentElectrolyzers = scheme.electrolyzers.filter {
        it.row in fragment.rowFrom..fragment.rowTo && it.position in fragment.colFrom..fragment.colTo
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .pointerInput(fragment, layout) {
                detectTapGestures { offset ->
                    // Зона касания крупнее ячейки: по мелкому электролизёру иначе
                    // не попасть пальцем. Сначала электролизёр, затем ворота.
                    val minTouch = SCHEME_MIN_TOUCH_DP.dp.toPx()
                    nearestTouch(electrolyzerHitZones, offset, minTouch)
                        ?.let { onElectrolyzerTap(it.id) }
                        ?: nearestTouch(gateHitZones, offset, minTouch)
                            ?.let { onGateTap(it) }
                }
            },
    ) {
        val scale = size.width / view.w

        fun x(coord: Float): Float = (coord - view.x) * scale
        fun y(coord: Float): Float = (coord - view.y) * scale
        fun rect(box: Box): Rect = Rect(
            offset = Offset(x(box.x), y(box.y)),
            size = Size(box.w * scale, box.h * scale),
        )
        fun inView(r: Rect): Boolean =
            r.right >= 0f && r.left <= size.width && r.bottom >= 0f && r.top <= size.height

        fun drawCentered(text: String, box: Box, style: TextStyle, color: androidx.compose.ui.graphics.Color) {
            val measured = textMeasurer.measure(text, style.copy(color = color))
            val r = rect(box)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    r.left + (r.width - measured.size.width) / 2f,
                    r.top + (r.height - measured.size.height) / 2f,
                ),
            )
        }

        /** Подпись, повёрнутая на [degrees] вокруг центра рамки (для дорог вдоль стены). */
        fun drawRotated(
            text: String,
            box: Box,
            style: TextStyle,
            color: androidx.compose.ui.graphics.Color,
            degrees: Float,
        ) {
            val measured = textMeasurer.measure(text, style.copy(color = color))
            val r = rect(box)
            val cx = r.left + r.width / 2f
            val cy = r.top + r.height / 2f
            withTransform({ rotate(degrees = degrees, pivot = Offset(cx, cy)) }) {
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        cx - measured.size.width / 2f,
                        cy - measured.size.height / 2f,
                    ),
                )
            }
        }

        electrolyzerHitZones.clear()
        gateHitZones.clear()

        // --- дороги вдоль стен корпуса: разворотные полосы сверху и снизу ---
        layout.turnLanes().forEach { lane ->
            val r = rect(lane)
            if (!inView(r)) return@forEach
            drawRect(color = roadFill, topLeft = r.topLeft, size = r.size)
            val centerY = r.top + r.height / 2f
            drawLine(
                color = roadMark,
                start = Offset(r.left, centerY),
                end = Offset(r.right, centerY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
            )
            if (r.width > 220f) {
                drawCentered(
                    text = turnLabel,
                    box = lane,
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = laneTextColor,
                )
            }
        }

        // --- дорога вдоль каждой боковой стены корпуса (параллельно стене) ---
        layout.sideLanes().forEach { lane ->
            val r = rect(lane)
            if (!inView(r)) return@forEach
            drawRect(color = roadFill, topLeft = r.topLeft, size = r.size)
            val centerX = r.left + r.width / 2f
            drawLine(
                color = roadMark,
                start = Offset(centerX, r.top),
                end = Offset(centerX, r.bottom),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
            )
            if (r.height > 240f && r.width > 26f) {
                drawRotated(
                    text = sideLaneLabel,
                    box = lane,
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = laneTextColor,
                    degrees = -90f,
                )
            }
        }

        // --- проезд техники между рядами ---
        (fragment.rowFrom..fragment.rowTo).forEach { row ->
            val aisle = layout.aisleBox(row)
            val r = rect(aisle)
            if (!inView(r)) return@forEach
            drawRect(color = roadFill, topLeft = r.topLeft, size = r.size)
            val centerY = r.top + r.height / 2f
            val dash = PathEffect.dashPathEffect(
                floatArrayOf(r.height * 0.45f, r.height * 0.45f),
            )
            drawLine(
                color = roadMark,
                start = Offset(r.left, centerY),
                end = Offset(r.right, centerY),
                strokeWidth = 2f,
                pathEffect = dash,
            )
            if (r.width > 160f) {
                drawCentered(
                    text = aisleLabel,
                    box = aisle,
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = laneTextColor,
                )
            }
        }

        // --- проходы между блоками (проходимы для техники) ---
        (fragment.rowFrom..fragment.rowTo).forEach { row ->
            (1 until layout.blocksPerRow).forEach { block ->
                val passage = layout.passageBox(row, layout.firstPositionOfBlock(block))
                val r = rect(passage)
                if (!inView(r)) return@forEach
                drawRect(color = laneColor.copy(alpha = 0.16f), topLeft = r.topLeft, size = r.size)
                val centerX = r.left + r.width / 2f
                drawLine(
                    color = laneColor.copy(alpha = 0.5f),
                    start = Offset(centerX, r.top),
                    end = Offset(centerX, r.bottom),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(r.height * 0.18f, r.height * 0.12f),
                    ),
                )
            }
        }

        // --- рамки блоков электролизёров ---
        layout.blockIndexes(fragment.colFrom, fragment.colTo).forEach { block ->
            (fragment.rowFrom..fragment.rowTo).forEach { row ->
                val blockBox = layout.blockBox(row, layout.firstPositionOfBlock(block))
                val r = rect(blockBox)
                if (!inView(r)) return@forEach
                drawRoundRect(
                    color = blockColor,
                    topLeft = r.topLeft,
                    size = r.size,
                    cornerRadius = CornerRadius(6f),
                    style = Stroke(width = 2f),
                )
            }
        }

        // --- граница половин ряда A/B ---
        if (layout.perRow > 1) {
            val boundary = layout.halfBoundaryX(scheme.halfSize)
            if (boundary in view.x..view.right) {
                (fragment.rowFrom..fragment.rowTo).forEach { row ->
                    val cell = layout.cellBox(row, fragment.colFrom)
                    val top = y(cell.y)
                    val bottom = y(cell.bottom)
                    drawLine(
                        color = dividerColor,
                        start = Offset(x(boundary), top),
                        end = Offset(x(boundary), bottom),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    )
                }
            }
        }

        // --- электролизёры ---
        fragmentElectrolyzers.forEach { electrolyzer ->
            val box = layout.cellBox(electrolyzer.row, electrolyzer.position)
            val r = rect(box)
            val selected = electrolyzer.id == selectedId
            val color = when {
                selected -> selectedColor
                electrolyzer.status == "SERVICE" -> serviceColor
                else -> okColor
            }
            drawRoundRect(
                color = color,
                topLeft = r.topLeft,
                size = r.size,
                cornerRadius = CornerRadius(4f),
            )
            if (r.width.toDp() >= 12.dp) {
                drawCentered(
                    text = electrolyzer.number.toString(),
                    box = box,
                    style = TextStyle(fontSize = (r.width.toDp().value / 3.2f).coerceIn(8f, 16f).sp),
                    color = if (selected) selectedTextColor else cellTextColor,
                )
            }
            electrolyzerHitZones.add(r to electrolyzer)
        }

        scheme.zones.forEach { zone ->
            val box = layout.zoneBox(zone.x, zone.y, zone.w, zone.h)
            val r = rect(box)
            if (!inView(r)) return@forEach
            // Отображаем только настоящие опасные зоны; проезды и тупики не показываем
            if (zone.kind != ZoneKind.DANGER) return@forEach
            val color = ZoneDanger
            drawRect(color = color.copy(alpha = 0.16f), topLeft = r.topLeft, size = r.size)
            drawRect(
                color = color,
                topLeft = r.topLeft,
                size = r.size,
                style = Stroke(width = 2f),
            )
            if (r.width > 90f) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = zone.label,
                    topLeft = Offset(r.left + 6f, r.top + 6f),
                    style = TextStyle(color = color, fontSize = 10.sp),
                )
            }
        }

        // --- ворота ---
        scheme.gates.forEach { gate ->
            val isEntrance = gate === entranceGate
            val (cx, cy) = if (isEntrance && entranceSide != null) {
                val e = layout.entranceBox(entranceSide, layout.gateCenter(gate.x, gate.y).second)
                (e.x + e.w / 2f) to (e.y + e.h / 2f)
            } else {
                layout.gateCenter(gate.x, gate.y)
            }
            val gateSize = 34f
            val center = Offset(x(cx), y(cy))
            val r = Rect(
                offset = Offset(center.x - gateSize / 2f, center.y - gateSize / 2f),
                size = Size(gateSize, gateSize),
            )
            val color = if (gate.isOpen) gateOpenColor else gateClosedColor
            drawRect(color = color, topLeft = r.topLeft, size = r.size, style = Stroke(width = 3f))
            drawText(
                textMeasurer = textMeasurer,
                text = if (gate.isOpen) "O" else "X",
                topLeft = Offset(r.left + 10f, r.top + 8f),
                style = TextStyle(color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
            gateHitZones.add(r to gate)

            // подпись входа рядом с воротами на стене
            if (isEntrance && entranceSide != null) {
                val labelBox = when (entranceSide) {
                    HallSide.LEFT -> Box(HallLayout.SIDE_LANE + 0.1f, cy - 0.18f, 1.6f, 0.36f)
                    HallSide.RIGHT -> Box(layout.totalWidth - HallLayout.SIDE_LANE - 1.7f, cy - 0.18f, 1.6f, 0.36f)
                }
                val lr = rect(labelBox)
                if (inView(lr)) {
                    drawCentered(
                        text = entranceLabel,
                        box = labelBox,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = wallColor,
                    )
                }
            }
        }

        // --- стены корпуса: единый контур с разрывом под единственный вход ---
        val wallStroke = 7f
        drawLine(wallColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = wallStroke)
        drawLine(wallColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = wallStroke)

        val gap = if (entranceSide != null && entranceGate != null) {
            val (_, cy) = layout.gateCenter(entranceGate.x, entranceGate.y)
            layout.entranceBox(entranceSide, cy)
        } else {
            null
        }
        fun wallSide(left: Boolean) {
            val xWall = if (left) 0f else size.width
            val hasGap = gap != null && (if (left) entranceSide == HallSide.LEFT else entranceSide == HallSide.RIGHT)
            if (hasGap && gap != null) {
                drawLine(wallColor, Offset(xWall, 0f), Offset(xWall, y(gap.y)), strokeWidth = wallStroke)
                drawLine(wallColor, Offset(xWall, y(gap.bottom)), Offset(xWall, size.height), strokeWidth = wallStroke)
            } else {
                drawLine(wallColor, Offset(xWall, 0f), Offset(xWall, size.height), strokeWidth = wallStroke)
            }
        }
        wallSide(left = true)
        wallSide(left = false)
    }
}

/** Формат схемы по размеру экрана: планшет — от 600dp по меньшей стороне. */
@Composable
fun rememberHallFormat(): HallFormat =
    if (LocalConfiguration.current.smallestScreenWidthDp >= 600) HallFormat.TABLET
    else HallFormat.PHONE

/** Перемещение по фрагменту корпуса: вдоль ряда и по парам рядов. */
@Composable
fun SchemeFragmentControls(
    layout: HallLayout,
    fragment: SchemeFragment,
    onShowFragment: (SchemeFragment) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onShowFragment(layout.shiftRows(fragment, -1)) },
                enabled = layout.canShiftRows(fragment, -1),
            ) { Text("\u25B2") }
            Text(
                text = stringResource(
                    R.string.scheme_fragment_rows,
                    fragment.rowFrom,
                    fragment.rowTo,
                    layout.rows,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
            TextButton(
                onClick = { onShowFragment(layout.shiftRows(fragment, 1)) },
                enabled = layout.canShiftRows(fragment, 1),
            ) { Text("\u25BC") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onShowFragment(layout.shiftCols(fragment, -1)) },
                enabled = layout.canShiftCols(fragment, -1),
            ) { Text("\u25C0") }
            Text(
                text = stringResource(
                    R.string.scheme_fragment_cols,
                    fragment.colFrom,
                    fragment.colTo,
                    layout.perRow,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
            TextButton(
                onClick = { onShowFragment(layout.shiftCols(fragment, 1)) },
                enabled = layout.canShiftCols(fragment, 1),
            ) { Text("\u25B6") }
        }
    }
}
