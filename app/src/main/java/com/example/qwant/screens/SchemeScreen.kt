package com.example.qwant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.data.Building
import com.example.qwant.data.DEFAULT_BUILDING
import com.example.qwant.data.Gate
import com.example.qwant.data.Scheme
import com.example.qwant.data.ZoneKind
import com.example.qwant.ui.ErrorBox
import com.example.qwant.ui.HallLayout
import com.example.qwant.ui.HallSchemeCanvas
import com.example.qwant.ui.HallSide
import com.example.qwant.ui.LoadingBox
import com.example.qwant.ui.QwantTopBar
import com.example.qwant.ui.SchemeFragment
import com.example.qwant.ui.SectionCard
import com.example.qwant.ui.rememberHallFormat
import com.example.qwant.ui.theme.ElectrolyzerService
import com.example.qwant.ui.theme.LaneDrive
import com.example.qwant.ui.theme.SignalGreen
import com.example.qwant.ui.theme.ZoneDanger
import com.example.qwant.ui.theme.ZoneDeadEnd

/**
 * Схема корпуса. Сверху — выбор корпуса (если корпусов несколько), ниже —
 * **весь корпус целиком**: все ряды и электролизёры (каждый стоит отдельно,
 * между соседями — проезд для техники), центральная дорога между линиями,
 * дороги вдоль стен и стены корпуса с единственным входом (слева или справа);
 * отмечены опасные зоны, тупики и ворота.
 *
 * Ячейка электролизёра — не мельче [HallLayout.MIN_CELL_DP] (≈100 px на
 * плотности 320), поэтому широкий корпус на телефоне **прокручивается по
 * горизонтали**, а не сжимается в нечитаемую мелкую схему.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeScreen(
    onBack: () -> Unit = {},
    onOpenGates: () -> Unit = {},
    viewModel: SchemeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val format = rememberHallFormat()

    Scaffold(
        topBar = {
            QwantTopBar(
                title = stringResource(R.string.scheme_title),
                subtitle = stringResource(
                    R.string.scheme_subtitle_building,
                    state.scheme?.building ?: state.buildingId ?: DEFAULT_BUILDING,
                ),
                onBack = onBack,
                actions = {
                    TextButton(onClick = onOpenGates) {
                        Text(stringResource(R.string.menu_gates))
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Выбор корпуса доступен всегда, в том числе пока грузится схема.
            if (state.buildings.size > 1) {
                BuildingPicker(
                    buildings = state.buildings,
                    selectedId = state.buildingId,
                    onSelect = viewModel::selectBuilding,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            when {
                state.scheme != null -> state.scheme?.let { scheme ->
                    val layout = remember(scheme.rows, scheme.perRow, format) {
                        HallLayout.of(scheme, format)
                    }
                    // Экран показывает весь корпус: все ряды и электролизёры.
                    val fragment = remember(layout) { layout.fullFragment() }

                    SchemeBody(
                        modifier = Modifier.weight(1f),
                        scheme = scheme,
                        layout = layout,
                        fragment = fragment,
                        selectedElectrolyzerId = state.selectedElectrolyzerId,
                        onSelectElectrolyzer = viewModel::selectElectrolyzer,
                        onToggleGate = viewModel::toggleGate,
                    )
                }

                state.error != null -> ErrorBox(
                    message = state.error!!,
                    onRetry = viewModel::load,
                    modifier = Modifier.weight(1f),
                )

                else -> LoadingBox(Modifier.weight(1f))
            }
        }
    }
}

/** Выбор корпуса цеха: чипы «Корпус N · количество электролизёров». */
@Composable
private fun BuildingPicker(
    buildings: List<Building>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(title = stringResource(R.string.scheme_building_title), modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            buildings.forEach { building ->
                FilterChip(
                    selected = building.id == selectedId,
                    onClick = { onSelect(building.id) },
                    label = {
                        Text(
                            stringResource(
                                R.string.scheme_building_chip,
                                building.id,
                                building.electrolyzerCount,
                            )
                        )
                    },
                )
            }
        }
    }
}

/**
 * Тело схемы. Схема занимает всю ширину, чтобы корпус был виден целиком;
 * сведения (выбранный электролизёр, зоны, обозначения) — ниже.
 */
@Composable
private fun SchemeBody(
    modifier: Modifier,
    scheme: Scheme,
    layout: HallLayout,
    fragment: SchemeFragment,
    selectedElectrolyzerId: Int?,
    onSelectElectrolyzer: (Int) -> Unit,
    onToggleGate: (Gate) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SchemeCard(
            scheme = scheme,
            layout = layout,
            fragment = fragment,
            selectedElectrolyzerId = selectedElectrolyzerId,
            onSelectElectrolyzer = onSelectElectrolyzer,
            onToggleGate = onToggleGate,
        )
        DetailsPanes(scheme, layout, selectedElectrolyzerId)
    }
}

@Composable
private fun SchemeCard(
    scheme: Scheme,
    layout: HallLayout,
    fragment: SchemeFragment,
    selectedElectrolyzerId: Int?,
    onSelectElectrolyzer: (Int) -> Unit,
    onToggleGate: (Gate) -> Unit,
) {
    SectionCard(title = stringResource(R.string.scheme_plan_title)) {
        HallSchemeCanvas(
            scheme = scheme,
            layout = layout,
            fragment = fragment,
            selectedId = selectedElectrolyzerId,
            onElectrolyzerTap = onSelectElectrolyzer,
            onGateTap = onToggleGate,
        )
    }
}

/** Сведения о схеме: выбранный электролизёр, зоны/тупики и обозначения. */
@Composable
private fun DetailsPanes(
    scheme: Scheme,
    layout: HallLayout,
    selectedElectrolyzerId: Int?,
) {
    scheme.electrolyzers.firstOrNull { it.id == selectedElectrolyzerId }?.let { selected ->
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

    SectionCard(title = stringResource(R.string.scheme_zones_title)) {
        scheme.zones.forEach { zone ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (zone.kind == ZoneKind.DANGER) ZoneDanger else ZoneDeadEnd
                        ),
                )
                Text(text = zone.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    SchemeLegend(scheme, layout)
}

@Composable
private fun SchemeLegend(scheme: Scheme, layout: HallLayout) {
    SectionCard(title = stringResource(R.string.scheme_legend_title)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LegendItem(MaterialTheme.colorScheme.surfaceVariant, stringResource(R.string.scheme_status_ok))
            LegendItem(ElectrolyzerService, stringResource(R.string.scheme_status_service))
            LegendItem(
                MaterialTheme.colorScheme.outline,
                stringResource(R.string.scheme_legend_block),
                outline = true,
            )
            LegendItem(LaneDrive, stringResource(R.string.scheme_legend_passage))
            LegendItem(ZoneDanger, stringResource(R.string.scheme_legend_danger))
            LegendItem(ZoneDeadEnd, stringResource(R.string.scheme_legend_dead_end))
            LegendItem(SignalGreen, stringResource(R.string.scheme_legend_gate_open))
            LegendItem(
                MaterialTheme.colorScheme.outline,
                stringResource(R.string.scheme_legend_entrance),
                outline = true,
            )
            LegendItem(LaneDrive, stringResource(R.string.scheme_legend_lane))
            LegendItem(LaneDrive, stringResource(R.string.scheme_legend_wall_road))
        }
        Text(
            text = stringResource(
                R.string.scheme_summary,
                scheme.building,
                scheme.electrolyzerCount,
                scheme.rows,
                scheme.halfSize,
                layout.blocksPerRow,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        scheme.gates.firstOrNull()?.let { gate ->
            Text(
                text = stringResource(
                    if (layout.entranceSide(gate.x) == HallSide.LEFT) {
                        R.string.scheme_entrance_left
                    } else {
                        R.string.scheme_entrance_right
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, outline: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (outline) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, color, RoundedCornerShape(3.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
