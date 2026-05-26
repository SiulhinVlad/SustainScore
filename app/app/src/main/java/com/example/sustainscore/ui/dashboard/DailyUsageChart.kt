package com.example.sustainscore.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.ActivityType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.roundToInt

private const val MinutesPerDay = 24 * 60
private const val TimelinePageCount = 731
private const val InitialTimelinePage = TimelinePageCount / 2
private const val OneMinuteMillis = 60_000L
private const val OneDayMillis = 24L * 60L * OneMinuteMillis

internal data class UsageChartBlock(
    val id: String,
    val activityName: String,
    val color: Color,
    val startMinute: Int,
    val endMinute: Int,
    val impact: Double,
    val stackBase: Double
) {
    val durationMinutes: Int = endMinute - startMinute
    val segmentKey: UsageChartSegmentKey = UsageChartSegmentKey(id, startMinute, endMinute)
    val stackBottom: Double = stackBase + impact
}

internal data class UsageChartSegmentKey(
    val logId: String,
    val startMinute: Int,
    val endMinute: Int
)

internal data class HourDepthPoint(
    val hour: Int,
    val depth: Double
)

internal data class DailyUsageChartLayout(
    val blocks: List<UsageChartBlock>,
    val recommendedBlocks: List<UsageChartBlock>,
    val hourDepths: List<HourDepthPoint>,
    val recommendedHourDepths: List<HourDepthPoint>,
    val maxDepth: Double
)

private data class RawUsageBlock(
    val id: String,
    val activityName: String,
    val color: Color,
    val startMinute: Int,
    val endMinute: Int,
    val impact: Double
)

private data class StackedUsageLayout(
    val blocks: List<UsageChartBlock>,
    val hourDepths: List<HourDepthPoint>,
    val maxDepth: Double
)

private data class ChartRenderSpec(
    val leftPadding: Float,
    val topPadding: Float,
    val chartWidth: Float,
    val depthToPixels: Float,
    val pixelsPerMinute: Float
)

private data class RenderedUsageBlock(
    val block: UsageChartBlock,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    fun contains(point: Offset): Boolean {
        return point.x in left..(left + width) && point.y in top..(top + height)
    }
}

@Composable
internal fun DailyUsageChartCard(
    anchorDate: LocalDate,
    selectedDate: LocalDate,
    logs: List<ActivityLog>,
    activityTypes: List<ActivityType>,
    onSelectedDateChange: (LocalDate) -> Unit,
    onDeleteSegment: (LocalDate, UsageChartSegmentKey) -> Unit,
    onMoveSegment: (LocalDate, UsageChartSegmentKey, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialPage = remember(anchorDate, selectedDate) {
        InitialTimelinePage + daysBetween(anchorDate, selectedDate)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) {
        TimelinePageCount
    }
    var showGuideDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, anchorDate) {
        onSelectedDateChange(dateForPage(anchorDate, pagerState.currentPage))
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily usage chart",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = { showGuideDialog = true },
                    modifier = Modifier.testTag("daily_usage_chart_guide_button")
                ) {
                    Text(text = "Guid")
                }
            }

            if (showGuideDialog) {
                AlertDialog(
                    onDismissRequest = { showGuideDialog = false },
                    modifier = Modifier.testTag("daily_usage_chart_guide_dialog"),
                    confirmButton = {
                        Button(onClick = { showGuideDialog = false }) {
                            Text(text = "Close")
                        }
                    },
                    title = {
                        Text(text = "Chart guide")
                    },
                    text = {
                        Text(
                            text = "This chart covers one full day from 00:00 to 00:00. Swipe left or right to move between days. Colored blocks show actual logged usage, and the gray overlay shows the recommended version of that same usage. Tap a colored block to select it, drag it left or right to fix its time, or delete only that segment if it was logged by mistake."
                        )
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_usage_chart_pager"),
                beyondViewportPageCount = 1,
                pageSpacing = 12.dp,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { page ->
                val pageDate = dateForPage(anchorDate, page)
                val layout = remember(logs, pageDate, activityTypes) {
                    buildDailyUsageChartLayout(
                        date = pageDate,
                        logs = logs,
                        activityTypes = activityTypes
                    )
                }

                DailyUsageChart(
                    pageDate = pageDate,
                    layout = layout,
                    onDeleteSegment = onDeleteSegment,
                    onMoveSegment = onMoveSegment,
                    chartTestTag = if (page == pagerState.currentPage) {
                        "daily_usage_chart_canvas_selected"
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
private fun DailyUsageChart(
    pageDate: LocalDate,
    layout: DailyUsageChartLayout,
    onDeleteSegment: (LocalDate, UsageChartSegmentKey) -> Unit,
    onMoveSegment: (LocalDate, UsageChartSegmentKey, Int) -> Unit,
    chartTestTag: String?,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)
    val actualLineColor = MaterialTheme.colorScheme.onSurface
    val recommendedOverlayColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.95f)
    val density = LocalDensity.current
    var chartCanvasSize by remember(pageDate) { mutableStateOf(IntSize.Zero) }
    var selectedSegmentKey by remember(pageDate) { mutableStateOf<UsageChartSegmentKey?>(null) }
    var dragPreviewStartMinute by remember(pageDate) { mutableStateOf<Int?>(null) }

    val renderSpec = remember(chartCanvasSize, layout.maxDepth) {
        buildChartRenderSpec(chartCanvasSize, layout.maxDepth)
    }
    val renderedBlocks = remember(layout.blocks, renderSpec) {
        renderSpec?.let { spec ->
            layout.blocks.map { block -> renderUsageChartBlock(block, spec) }
        } ?: emptyList()
    }
    val selectedBlock = remember(layout.blocks, selectedSegmentKey) {
        layout.blocks.firstOrNull { block -> block.segmentKey == selectedSegmentKey }
    }
    val selectedDisplayBlock = remember(selectedBlock, dragPreviewStartMinute) {
        selectedBlock?.let { block ->
            val previewStart = dragPreviewStartMinute ?: block.startMinute
            block.copy(
                startMinute = previewStart,
                endMinute = previewStart + block.durationMinutes
            )
        }
    }
    val selectedRenderedBlock = remember(selectedDisplayBlock, renderSpec) {
        if (selectedDisplayBlock != null && renderSpec != null) {
            renderUsageChartBlock(selectedDisplayBlock, renderSpec)
        } else {
            null
        }
    }

    LaunchedEffect(layout.blocks, selectedSegmentKey) {
        if (selectedSegmentKey != null && selectedBlock == null) {
            selectedSegmentKey = null
            dragPreviewStartMinute = null
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = pageDate.format(DashboardDateFormatter),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .let { baseModifier ->
                    if (chartTestTag != null) {
                        baseModifier.testTag(chartTestTag)
                    } else {
                        baseModifier
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .onSizeChanged { chartCanvasSize = it }
                    .align(Alignment.TopCenter)
            ) {
                val spec = buildChartRenderSpec(
                    canvasSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    maxDepth = layout.maxDepth
                ) ?: return@Canvas
                val zeroY = spec.topPadding
                val selectedPreview = selectedDisplayBlock?.let { block ->
                    renderUsageChartBlock(block, spec)
                }

                drawLine(
                    color = outlineColor,
                    start = Offset(spec.leftPadding, zeroY),
                    end = Offset(spec.leftPadding + spec.chartWidth, zeroY),
                    strokeWidth = 3f
                )

                layout.blocks.forEach { block ->
                    val renderedBlock = renderUsageChartBlock(block, spec)

                    drawRect(
                        color = block.color.copy(alpha = 0.78f),
                        topLeft = Offset(renderedBlock.left, renderedBlock.top),
                        size = Size(renderedBlock.width, renderedBlock.height)
                    )
                    drawRect(
                        color = block.color.copy(alpha = 0.98f),
                        topLeft = Offset(renderedBlock.left, renderedBlock.top),
                        size = Size(renderedBlock.width, renderedBlock.height),
                        style = Stroke(width = 2f)
                    )
                }

                layout.recommendedBlocks.forEach { block ->
                    val renderedBlock = renderUsageChartBlock(block, spec)
                    val insetX = 2f
                    val recommendedWidth = (renderedBlock.width - insetX * 2).coerceAtLeast(2f)

                    drawRect(
                        color = recommendedOverlayColor,
                        topLeft = Offset(renderedBlock.left + insetX, renderedBlock.top),
                        size = Size(recommendedWidth, renderedBlock.height),
                        style = Stroke(width = 2f)
                    )
                }

                val recommendedLinePath = Path()
                layout.recommendedHourDepths.forEachIndexed { index, point ->
                    val x = spec.leftPadding + spec.chartWidth * ((point.hour * 60f) / MinutesPerDay.toFloat())
                    val y = zeroY + (point.depth.toFloat() * spec.depthToPixels)
                    if (index == 0) {
                        recommendedLinePath.moveTo(x, y)
                    } else {
                        recommendedLinePath.lineTo(x, y)
                    }
                }

                drawPath(
                    path = recommendedLinePath,
                    color = recommendedOverlayColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                layout.recommendedHourDepths.forEach { point ->
                    val x = spec.leftPadding + spec.chartWidth * ((point.hour * 60f) / MinutesPerDay.toFloat())
                    val y = zeroY + (point.depth.toFloat() * spec.depthToPixels)
                    drawCircle(
                        color = recommendedOverlayColor,
                        radius = 4.5f,
                        center = Offset(x, y)
                    )
                }

                val linePath = Path()
                layout.hourDepths.forEachIndexed { index, point ->
                    val x = spec.leftPadding + spec.chartWidth * ((point.hour * 60f) / MinutesPerDay.toFloat())
                    val y = zeroY + (point.depth.toFloat() * spec.depthToPixels)
                    if (index == 0) {
                        linePath.moveTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = actualLineColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                layout.hourDepths.forEach { point ->
                    val x = spec.leftPadding + spec.chartWidth * ((point.hour * 60f) / MinutesPerDay.toFloat())
                    val y = zeroY + (point.depth.toFloat() * spec.depthToPixels)
                    drawCircle(
                        color = actualLineColor,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                }

                if (selectedPreview != null && selectedBlock != null) {
                    drawRect(
                        color = selectedBlock.color.copy(alpha = 0.28f),
                        topLeft = Offset(selectedPreview.left, selectedPreview.top),
                        size = Size(selectedPreview.width, selectedPreview.height)
                    )
                    drawRect(
                        color = actualLineColor,
                        topLeft = Offset(selectedPreview.left, selectedPreview.top),
                        size = Size(selectedPreview.width, selectedPreview.height),
                        style = Stroke(width = 4f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .align(Alignment.TopCenter)
                    .pointerInput(renderedBlocks) {
                        detectTapGestures { offset ->
                            val tappedBlock = renderedBlocks.lastOrNull { renderedBlock ->
                                renderedBlock.contains(offset)
                            }
                            selectedSegmentKey = tappedBlock?.block?.segmentKey
                            dragPreviewStartMinute = null
                        }
                    }
            )

            if (selectedRenderedBlock != null && selectedBlock != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            IntOffset(
                                x = selectedRenderedBlock.left.roundToInt(),
                                y = selectedRenderedBlock.top.roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { selectedRenderedBlock.width.toDp() },
                            height = with(density) { selectedRenderedBlock.height.toDp() }
                        )
                        .pointerInput(selectedBlock, renderSpec) {
                            if (renderSpec == null) {
                                return@pointerInput
                            }
                            detectDragGestures(
                                onDragStart = {
                                    dragPreviewStartMinute = selectedBlock.startMinute
                                },
                                onDragEnd = {
                                    val previewStart = dragPreviewStartMinute
                                    if (
                                        previewStart != null &&
                                        previewStart != selectedBlock.startMinute
                                    ) {
                                        onMoveSegment(
                                            pageDate,
                                            selectedBlock.segmentKey,
                                            previewStart
                                        )
                                        selectedSegmentKey = null
                                    }
                                    dragPreviewStartMinute = null
                                },
                                onDragCancel = {
                                    dragPreviewStartMinute = null
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                val maxStartMinute = MinutesPerDay - selectedBlock.durationMinutes
                                val deltaMinutes = (
                                    dragAmount.x / renderSpec.pixelsPerMinute
                                    ).roundToInt()
                                if (deltaMinutes != 0) {
                                    val currentStart =
                                        dragPreviewStartMinute ?: selectedBlock.startMinute
                                    dragPreviewStartMinute = (
                                        currentStart + deltaMinutes
                                        ).coerceIn(0, maxStartMinute)
                                }
                            }
                        }
                )
            }

            if (layout.blocks.isEmpty()) {
                Text(
                    text = "No logged usage for this day yet.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "00:00").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (layout.blocks.isNotEmpty() && selectedBlock != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val displayStartMinute =
                        dragPreviewStartMinute ?: selectedBlock.startMinute
                    val displayEndMinute =
                        displayStartMinute + selectedBlock.durationMinutes

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawCircle(color = selectedBlock.color)
                        }
                        Text(
                            text = "${selectedBlock.activityName}, ${
                                formatMinuteRange(displayStartMinute, displayEndMinute)
                            }",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Drag the selected block left or right to correct its time, or delete only this segment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedSegmentKey = null
                                dragPreviewStartMinute = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Clear")
                        }
                        Button(
                            onClick = {
                                onDeleteSegment(pageDate, selectedBlock.segmentKey)
                                selectedSegmentKey = null
                                dragPreviewStartMinute = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Delete")
                        }
                    }
                }
            }
        }
    }
}

internal fun buildDailyUsageChartLayout(
    date: LocalDate,
    logs: List<ActivityLog>,
    activityTypes: List<ActivityType>,
    zoneId: ZoneId = ZoneId.systemDefault()
): DailyUsageChartLayout {
    val actualLayout = buildStackedUsageLayout(
        rawBlocks = buildRawUsageBlocks(
            date = date,
            logs = logs,
            activityTypes = activityTypes,
            zoneId = zoneId
        ) { activityType, log ->
            activityType.weight * log.quantity
        }
    )
    val recommendedLayout = buildStackedUsageLayout(
        rawBlocks = buildRawUsageBlocks(
            date = date,
            logs = logs,
            activityTypes = activityTypes,
            zoneId = zoneId
        ) { activityType, log ->
            activityType.weight * minOf(log.quantity, activityType.recommendedLimit)
        }
    )

    return DailyUsageChartLayout(
        blocks = actualLayout.blocks,
        recommendedBlocks = recommendedLayout.blocks,
        hourDepths = actualLayout.hourDepths,
        recommendedHourDepths = recommendedLayout.hourDepths,
        maxDepth = max(actualLayout.maxDepth, recommendedLayout.maxDepth)
    )
}

private fun buildRawUsageBlocks(
    date: LocalDate,
    logs: List<ActivityLog>,
    activityTypes: List<ActivityType>,
    zoneId: ZoneId,
    impactForLog: (ActivityType, ActivityLog) -> Double
): List<RawUsageBlock> {
    val activityTypesById = activityTypes.associateBy { it.id }
    val dayStartMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val dayEndMillis = dayStartMillis + OneDayMillis

    return logs.mapNotNull { log ->
        val activityType = activityTypesById[log.activityTypeId] ?: return@mapNotNull null
        val logStartMillis = log.startTimestampMillis
        val logEndMillis = logStartMillis + log.durationMinutes * OneMinuteMillis
        val clippedStartMillis = max(logStartMillis, dayStartMillis)
        val clippedEndMillis = minOf(logEndMillis, dayEndMillis)

        if (clippedEndMillis <= clippedStartMillis) {
            return@mapNotNull null
        }

        val impact = impactForLog(activityType, log)
        if (impact <= 0.0) {
            return@mapNotNull null
        }

        RawUsageBlock(
            id = log.id,
            activityName = activityType.name,
            color = colorForActivity(activityType.id),
            startMinute = ((clippedStartMillis - dayStartMillis) / OneMinuteMillis).toInt(),
            endMinute = ((clippedEndMillis - dayStartMillis) / OneMinuteMillis).toInt(),
            impact = impact
        )
    }.sortedWith(compareBy<RawUsageBlock> { it.startMinute }.thenByDescending { it.impact }.thenBy { it.id })
}

private fun buildStackedUsageLayout(rawBlocks: List<RawUsageBlock>): StackedUsageLayout {
    val timelineBoundaries = rawBlocks
        .flatMap { block -> listOf(block.startMinute, block.endMinute) }
        .distinct()
        .sorted()

    val stackedBlocks = mutableListOf<UsageChartBlock>()
    val lastBlockIndexById = mutableMapOf<String, Int>()

    timelineBoundaries.zipWithNext().forEach { (segmentStart, segmentEnd) ->
        val activeBlocks = rawBlocks
            .filter { block -> block.startMinute < segmentEnd && block.endMinute > segmentStart }
            .sortedWith(
                compareBy<RawUsageBlock> { it.startMinute }
                    .thenByDescending { it.impact }
                    .thenBy { it.id }
            )

        var currentStackBase = 0.0
        activeBlocks.forEach { block ->
            val segment = UsageChartBlock(
                id = block.id,
                activityName = block.activityName,
                color = block.color,
                startMinute = segmentStart,
                endMinute = segmentEnd,
                impact = block.impact,
                stackBase = currentStackBase
            )

            val previousIndex = lastBlockIndexById[block.id]
            if (previousIndex != null) {
                val previousBlock = stackedBlocks[previousIndex]
                if (
                    previousBlock.endMinute == segment.startMinute &&
                    previousBlock.stackBase == segment.stackBase &&
                    previousBlock.impact == segment.impact
                ) {
                    stackedBlocks[previousIndex] = previousBlock.copy(
                        endMinute = segment.endMinute
                    )
                } else {
                    stackedBlocks += segment
                    lastBlockIndexById[block.id] = stackedBlocks.lastIndex
                }
            } else {
                stackedBlocks += segment
                lastBlockIndexById[block.id] = stackedBlocks.lastIndex
            }

            currentStackBase += block.impact
        }
    }

    val hourDepths = (0..24).map { hour ->
        val minute = (hour * 60).coerceAtMost(MinutesPerDay)
        HourDepthPoint(
            hour = hour,
            depth = stackedBlocks
                .filter { block -> minute in block.startMinute until block.endMinute }
                .maxOfOrNull { it.stackBottom }
                ?: 0.0
        )
    }

    return StackedUsageLayout(
        blocks = stackedBlocks,
        hourDepths = hourDepths,
        maxDepth = stackedBlocks.maxOfOrNull { it.stackBottom } ?: 0.0
    )
}

internal fun logsForDate(
    logs: List<ActivityLog>,
    date: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<ActivityLog> {
    return logs.filter { log ->
        Instant.ofEpochMilli(log.startTimestampMillis)
            .atZone(zoneId)
            .toLocalDate() == date
    }
}

internal fun deleteChartSegment(
    logs: List<ActivityLog>,
    pageDate: LocalDate,
    segment: UsageChartSegmentKey,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<ActivityLog> {
    val split = splitLogForChartSegment(
        logs = logs,
        pageDate = pageDate,
        segment = segment,
        zoneId = zoneId
    ) ?: return logs

    return replaceLogWithSegments(
        logs = logs,
        sourceLogId = split.source.id,
        replacementLogs = listOfNotNull(split.before, split.after)
    )
}

internal fun moveChartSegment(
    logs: List<ActivityLog>,
    pageDate: LocalDate,
    segment: UsageChartSegmentKey,
    newStartMinute: Int,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<ActivityLog> {
    val split = splitLogForChartSegment(
        logs = logs,
        pageDate = pageDate,
        segment = segment,
        zoneId = zoneId
    ) ?: return logs

    val maxStartMinute = MinutesPerDay - split.selected.durationMinutes
    val clampedStartMinute = newStartMinute.coerceIn(0, maxStartMinute)
    val movedStartTimestampMillis =
        pageDate.atStartOfDay(zoneId).toInstant().toEpochMilli() +
            clampedStartMinute * OneMinuteMillis
    val movedSegment = split.selected.copy(
        id = retainedOrDerivedSegmentId(
            sourceLog = split.source,
            label = "moved",
            keepOriginalId = split.before == null && split.after == null,
            startTimestampMillis = movedStartTimestampMillis,
            durationMinutes = split.selected.durationMinutes
        ),
        startTimestampMillis = movedStartTimestampMillis
    )

    return replaceLogWithSegments(
        logs = logs,
        sourceLogId = split.source.id,
        replacementLogs = listOfNotNull(split.before, split.after) + movedSegment
    )
}

private data class SplitActivityLog(
    val source: ActivityLog,
    val before: ActivityLog?,
    val selected: ActivityLog,
    val after: ActivityLog?
)

private fun splitLogForChartSegment(
    logs: List<ActivityLog>,
    pageDate: LocalDate,
    segment: UsageChartSegmentKey,
    zoneId: ZoneId
): SplitActivityLog? {
    val sourceLog = logs.firstOrNull { log -> log.id == segment.logId } ?: return null
    val sourceStartMillis = sourceLog.startTimestampMillis
    val sourceEndMillis = sourceStartMillis + sourceLog.durationMinutes * OneMinuteMillis
    val dayStartMillis = pageDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val segmentStartMillis = dayStartMillis + segment.startMinute * OneMinuteMillis
    val segmentEndMillis = dayStartMillis + segment.endMinute * OneMinuteMillis
    val actualSegmentStartMillis = max(sourceStartMillis, segmentStartMillis)
    val actualSegmentEndMillis = minOf(sourceEndMillis, segmentEndMillis)

    if (actualSegmentEndMillis <= actualSegmentStartMillis) {
        return null
    }

    val selectedDurationMinutes =
        ((actualSegmentEndMillis - actualSegmentStartMillis) / OneMinuteMillis).toInt()
    val beforeDurationMinutes =
        ((actualSegmentStartMillis - sourceStartMillis) / OneMinuteMillis).toInt()
    val afterDurationMinutes =
        ((sourceEndMillis - actualSegmentEndMillis) / OneMinuteMillis).toInt()

    val beforeLog = if (beforeDurationMinutes > 0) {
        sourceLog.copy(
            id = derivedSegmentId(
                sourceLog = sourceLog,
                label = "before",
                startTimestampMillis = sourceStartMillis,
                durationMinutes = beforeDurationMinutes
            ),
            durationMinutes = beforeDurationMinutes
        )
    } else {
        null
    }

    val selectedLog = sourceLog.copy(
        id = retainedOrDerivedSegmentId(
            sourceLog = sourceLog,
            label = "selected",
            keepOriginalId = beforeLog == null && afterDurationMinutes == 0,
            startTimestampMillis = actualSegmentStartMillis,
            durationMinutes = selectedDurationMinutes
        ),
        startTimestampMillis = actualSegmentStartMillis,
        durationMinutes = selectedDurationMinutes
    )

    val afterLog = if (afterDurationMinutes > 0) {
        sourceLog.copy(
            id = derivedSegmentId(
                sourceLog = sourceLog,
                label = "after",
                startTimestampMillis = actualSegmentEndMillis,
                durationMinutes = afterDurationMinutes
            ),
            startTimestampMillis = actualSegmentEndMillis,
            durationMinutes = afterDurationMinutes
        )
    } else {
        null
    }

    return SplitActivityLog(
        source = sourceLog,
        before = beforeLog,
        selected = selectedLog,
        after = afterLog
    )
}

private fun replaceLogWithSegments(
    logs: List<ActivityLog>,
    sourceLogId: String,
    replacementLogs: List<ActivityLog>
): List<ActivityLog> {
    val replacedLogs = logs.filterNot { log -> log.id == sourceLogId } + replacementLogs
    return replacedLogs.sortedWith(
        compareBy<ActivityLog> { it.startTimestampMillis }
            .thenBy { it.id }
    )
}

private fun retainedOrDerivedSegmentId(
    sourceLog: ActivityLog,
    label: String,
    keepOriginalId: Boolean,
    startTimestampMillis: Long,
    durationMinutes: Int
): String {
    return if (keepOriginalId) {
        sourceLog.id
    } else {
        derivedSegmentId(
            sourceLog = sourceLog,
            label = label,
            startTimestampMillis = startTimestampMillis,
            durationMinutes = durationMinutes
        )
    }
}

private fun derivedSegmentId(
    sourceLog: ActivityLog,
    label: String,
    startTimestampMillis: Long,
    durationMinutes: Int
): String {
    return "${sourceLog.id}-$label-$startTimestampMillis-$durationMinutes"
}

private fun buildChartRenderSpec(
    canvasSize: IntSize,
    maxDepth: Double
): ChartRenderSpec? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) {
        return null
    }

    val leftPadding = 12f
    val rightPadding = 12f
    val topPadding = 18f
    val bottomPadding = 18f
    val chartWidth = canvasSize.width - leftPadding - rightPadding
    val chartHeight = canvasSize.height - topPadding - bottomPadding
    val depthScaleBase = max(maxDepth, 12.0)
    val depthToPixels = if (depthScaleBase == 0.0) 0f else (chartHeight / depthScaleBase).toFloat()

    return ChartRenderSpec(
        leftPadding = leftPadding,
        topPadding = topPadding,
        chartWidth = chartWidth,
        depthToPixels = depthToPixels,
        pixelsPerMinute = chartWidth / MinutesPerDay.toFloat()
    )
}

private fun renderUsageChartBlock(
    block: UsageChartBlock,
    renderSpec: ChartRenderSpec
): RenderedUsageBlock {
    val left =
        renderSpec.leftPadding + renderSpec.chartWidth * (block.startMinute / MinutesPerDay.toFloat())
    val width =
        (renderSpec.chartWidth * (block.durationMinutes / MinutesPerDay.toFloat())).coerceAtLeast(6f)
    val top = renderSpec.topPadding + (block.stackBase.toFloat() * renderSpec.depthToPixels)
    val height = (block.impact.toFloat() * renderSpec.depthToPixels).coerceAtLeast(10f)

    return RenderedUsageBlock(
        block = block,
        left = left,
        top = top,
        width = width,
        height = height
    )
}

private fun formatMinuteRange(
    startMinute: Int,
    endMinute: Int
): String {
    return "${formatMinuteOfDay(startMinute)} to ${formatMinuteOfDay(endMinute)}"
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val normalizedMinute = minuteOfDay.coerceIn(0, MinutesPerDay)
    val hour = (normalizedMinute / 60) % 24
    val minute = normalizedMinute % 60
    return String.format("%02d:%02d", hour, minute)
}

private fun dateForPage(anchorDate: LocalDate, page: Int): LocalDate {
    return anchorDate.plusDays((page - InitialTimelinePage).toLong())
}

private fun daysBetween(anchorDate: LocalDate, selectedDate: LocalDate): Int {
    return (selectedDate.toEpochDay() - anchorDate.toEpochDay()).toInt()
}

private fun colorForActivity(activityTypeId: String): Color {
    return when (activityTypeId) {
        "lighting" -> Color(0xFFD98C2E)
        "washing_machine" -> Color(0xFF3D74C5)
        "computer_tv" -> Color(0xFF2F8F83)
        "heating_cooling" -> Color(0xFFC44E39)
        else -> Color(0xFF6F7B77)
    }
}
