package com.example.sustainscore.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.ActivityType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

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
    val stackBottom: Double = stackBase + impact
}

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

internal data class ActivityLegend(
    val label: String,
    val color: Color
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

@Composable
internal fun DailyUsageChartCard(
    anchorDate: LocalDate,
    selectedDate: LocalDate,
    logs: List<ActivityLog>,
    activityTypes: List<ActivityType>,
    onSelectedDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialPage = remember(anchorDate, selectedDate) {
        InitialTimelinePage + daysBetween(anchorDate, selectedDate)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) {
        TimelinePageCount
    }
    val legendItems = remember(activityTypes) {
        activityTypes.map { activityType ->
            ActivityLegend(
                label = activityType.name,
                color = colorForActivity(activityType.id)
            )
        }
    }

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
            Text(
                text = "Daily usage chart",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Swipe horizontally to move from one day to the next. Colored blocks show logged usage, while the gray overlay shows the recommended version of the same usage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ActivityLegendSection(items = legendItems)

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
private fun ActivityLegendSection(items: List<ActivityLegend>) {
    BoxWithConstraints {
        val compactLayout = maxWidth < 420.dp
        if (compactLayout) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendRow(items = items.take(2))
                LegendRow(items = items.drop(2))
            }
        } else {
            LegendRow(items = items)
        }
    }
}

@Composable
private fun LegendRow(items: List<ActivityLegend>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            LegendItem(
                item = item,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LegendItem(
    item: ActivityLegend,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = item.color.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(color = item.color)
            }
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DailyUsageChart(
    pageDate: LocalDate,
    layout: DailyUsageChartLayout,
    chartTestTag: String?,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)
    val actualLineColor = MaterialTheme.colorScheme.onSurface
    val recommendedOverlayColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.95f)
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
                    .align(Alignment.TopCenter)
            ) {
                val leftPadding = 12f
                val rightPadding = 12f
                val topPadding = 18f
                val bottomPadding = 18f
                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding
                val depthScaleBase = max(layout.maxDepth, 12.0)
                val depthToPixels = if (depthScaleBase == 0.0) 0f else (chartHeight / depthScaleBase).toFloat()
                val zeroY = topPadding

                drawLine(
                    color = outlineColor,
                    start = Offset(leftPadding, zeroY),
                    end = Offset(leftPadding + chartWidth, zeroY),
                    strokeWidth = 3f
                )

                layout.blocks.forEach { block ->
                    val x = leftPadding + chartWidth * (block.startMinute / MinutesPerDay.toFloat())
                    val width = chartWidth * ((block.endMinute - block.startMinute) / MinutesPerDay.toFloat())
                    val y = zeroY + (block.stackBase.toFloat() * depthToPixels)
                    val height = (block.impact.toFloat() * depthToPixels).coerceAtLeast(10f)

                    drawRect(
                        color = block.color.copy(alpha = 0.78f),
                        topLeft = Offset(x, y),
                        size = Size(width.coerceAtLeast(6f), height)
                    )
                    drawRect(
                        color = block.color.copy(alpha = 0.98f),
                        topLeft = Offset(x, y),
                        size = Size(width.coerceAtLeast(6f), height),
                        style = Stroke(width = 2f)
                    )
                }

                layout.recommendedBlocks.forEach { block ->
                    val x = leftPadding + chartWidth * (block.startMinute / MinutesPerDay.toFloat())
                    val width = chartWidth * ((block.endMinute - block.startMinute) / MinutesPerDay.toFloat())
                    val y = zeroY + (block.stackBase.toFloat() * depthToPixels)
                    val height = (block.impact.toFloat() * depthToPixels).coerceAtLeast(10f)
                    val insetX = 2f
                    val recommendedWidth = (width.coerceAtLeast(6f) - insetX * 2).coerceAtLeast(2f)

                    drawRect(
                        color = recommendedOverlayColor,
                        topLeft = Offset(x + insetX, y),
                        size = Size(recommendedWidth, height),
                        style = Stroke(width = 2f)
                    )
                }

                val recommendedLinePath = Path()
                layout.recommendedHourDepths.forEachIndexed { index, point ->
                    val x = leftPadding + chartWidth * ((point.hour * 60f) / MinutesPerDay.toFloat())
                    val y = zeroY + (point.depth.toFloat() * depthToPixels)
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
                    val x = leftPadding + chartWidth * ((point.hour * 60f) / MinutesPerDay.toFloat())
                    val y = zeroY + (point.depth.toFloat() * depthToPixels)
                    drawCircle(
                        color = recommendedOverlayColor,
                        radius = 4.5f,
                        center = Offset(x, y)
                    )
                }

                val linePath = Path()
                layout.hourDepths.forEachIndexed { index, point ->
                    val x = leftPadding + chartWidth * ((point.hour * 60f) / MinutesPerDay.toFloat())
                    val y = zeroY + (point.depth.toFloat() * depthToPixels)
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
                    val x = leftPadding + chartWidth * ((point.hour * 60f) / MinutesPerDay.toFloat())
                    val y = zeroY + (point.depth.toFloat() * depthToPixels)
                    drawCircle(
                        color = actualLineColor,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                }
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
