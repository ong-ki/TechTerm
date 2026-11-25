package com.techterms.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.techterms.app.data.db.Term
import com.techterms.app.data.db.TermCategories
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Term counts per category, in the fixed category order. */
data class HomeStats(
    val categoryCounts: List<Pair<String, Int>> = emptyList()
) {
    companion object {
        fun from(terms: List<Term>): HomeStats {
            val counts = terms.groupingBy { it.category }.eachCount()
            val ordered = TermCategories.all.map { it to (counts[it] ?: 0) }
            val extra = counts.keys.filter { it !in TermCategories.all }.sorted().map { it to counts.getValue(it) }
            return HomeStats(ordered + extra)
        }
    }
}

/** Filter that Home asks Browse to apply. */
data class BrowseRequest(
    val category: String? = null,
    val difficulty: String? = null,
    val devOnly: Boolean = false
)

private const val TWO_PI = (2 * PI).toFloat()
private const val WAVE_PERIOD_MS = 9_000

private fun categoryIcon(category: String): ImageVector = when (category) {
    "Networking" -> Icons.Outlined.Wifi
    "Database" -> Icons.Outlined.Storage
    "Security" -> Icons.Outlined.Lock
    "UI Design" -> Icons.Outlined.Brush
    "Web" -> Icons.Outlined.Language
    "Mobile" -> Icons.Outlined.PhoneAndroid
    "Cloud" -> Icons.Outlined.Cloud
    "AI" -> Icons.Outlined.Psychology
    "Programming", "Code" -> Icons.Outlined.Code
    "Hardware" -> Icons.Outlined.Memory
    "Git & Version Control" -> Icons.Outlined.AccountTree
    "Linux & Terminal" -> Icons.Outlined.Terminal
    "DevOps" -> Icons.Outlined.Build
    "Software Design" -> Icons.Outlined.Architecture
    "Data & Formats" -> Icons.Outlined.DataObject
    else -> Icons.Outlined.Bookmark
}

@Composable
fun CategoryWaveSection(
    stats: HomeStats,
    onBrowse: (BrowseRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val points = stats.categoryCounts
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val safeSelected = if (points.isEmpty()) 0 else selected.coerceIn(0, points.lastIndex)
    val current = points.getOrNull(safeSelected)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lessons",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    current?.let {
                        Text(
                            text = it.first,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                WaveGraph(
                    categories = points.map { it.first },
                    counts = points.map { it.second },
                    selected = safeSelected,
                    onSelect = { selected = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIcon(current?.first.orEmpty()),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current?.first ?: "No terms yet",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${current?.second ?: 0} terms",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = { current?.let { onBrowse(BrowseRequest(category = it.first)) } },
                    enabled = current != null
                ) {
                    Text("Browse")
                }
            }
        }
    }
}

@Composable
private fun WaveGraph(
    categories: List<String>,
    counts: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val ghostColor = MaterialTheme.colorScheme.secondary
    val dotRing = MaterialTheme.colorScheme.surface
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedContainer = MaterialTheme.colorScheme.primaryContainer
    val painters = categories.map { category ->
        key(category) { rememberVectorPainter(categoryIcon(category)) }
    }

    // Draw-in on first show; phase drives the idle wave. Both are read only in the draw phase,
    // so updates redraw the Canvas without recomposing anything else.
    val reveal = remember { Animatable(0f) }
    val phase = remember { Animatable(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(durationMillis = 1_100, easing = FastOutSlowInEasing))
    }
    // Pauses automatically when the screen is not RESUMED (backgrounded or covered).
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val remaining = ((TWO_PI - phase.value) / TWO_PI * WAVE_PERIOD_MS).roundToInt()
                phase.animateTo(TWO_PI, tween(durationMillis = remaining.coerceAtLeast(1), easing = LinearEasing))
                phase.snapTo(0f)
            }
        }
    }

    val currentCounts by rememberUpdatedState(counts)
    val currentOnSelect by rememberUpdatedState(onSelect)

    fun nearestIndex(x: Float, width: Float, padX: Float): Int {
        val n = currentCounts.size
        if (n <= 1) return 0
        val usable = (width - padX * 2).coerceAtLeast(1f)
        val step = usable / (n - 1)
        return ((x - padX) / step).roundToInt().coerceIn(0, n - 1)
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                val padX = 16.dp.toPx()
                detectTapGestures { currentOnSelect(nearestIndex(it.x, size.width.toFloat(), padX)) }
            }
            .pointerInput(Unit) {
                val padX = 16.dp.toPx()
                detectHorizontalDragGestures { change, _ ->
                    currentOnSelect(nearestIndex(change.position.x, size.width.toFloat(), padX))
                }
            }
    ) {
        if (counts.isEmpty()) return@Canvas
        val amp = 3.5.dp.toPx()
        val p = phase.value
        val revealRight = size.width * reveal.value

        // Faint background waves (different phase/speed, lower alpha).
        drawWave(counts, amp * 1.6f, p * 0.6f + 1.3f, ghostColor.copy(alpha = 0.12f), 1.5.dp.toPx(), null)
        drawWave(counts, amp * 2.2f, -p * 0.8f + 2.6f, ghostColor.copy(alpha = 0.08f), 1.5.dp.toPx(), null)

        // Baseline with category ticks.
        val pts = wavePoints(counts, amp, p)
        val baseY = size.height - 6.dp.toPx()
        drawLine(axisColor, Offset(0f, baseY), Offset(size.width, baseY), 1.dp.toPx())
        pts.forEach { drawCircle(axisColor, 2.dp.toPx(), Offset(it.x, baseY)) }

        clipRect(right = revealRight) {
            drawWave(
                counts, amp, p, lineColor, 2.5.dp.toPx(),
                Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0f)),
                    startY = 0f,
                    endY = baseY
                )
            )
        }

        // Unselected category icons moving with the wave.
        val iconPx = 20.dp.toPx()
        val iconBgRadius = 11.dp.toPx()
        pts.forEachIndexed { index, pt ->
            if (index != selected && pt.x <= revealRight + 1f) {
                drawCircle(dotRing.copy(alpha = 0.88f), iconBgRadius, pt)
                painters.getOrNull(index)?.let { painter ->
                    translate(left = pt.x - iconPx / 2f, top = pt.y - iconPx / 2f) {
                        with(painter) {
                            draw(
                                size = Size(iconPx, iconPx),
                                alpha = 0.82f,
                                colorFilter = ColorFilter.tint(iconTint)
                            )
                        }
                    }
                }
            }
        }

        // Selected point: vertical guide, circle highlight, and enlarged icon.
        pts.getOrNull(selected)?.let { dot ->
            if (dot.x <= revealRight + 1f) {
                val selectedIconPx = 24.dp.toPx()
                val highlightRadius = 15.dp.toPx()
                drawLine(lineColor.copy(alpha = 0.35f), Offset(dot.x, dot.y), Offset(dot.x, baseY), 1.dp.toPx())
                drawCircle(lineColor.copy(alpha = 0.16f), 19.dp.toPx(), dot)
                drawCircle(selectedContainer, highlightRadius, dot)
                drawCircle(lineColor, highlightRadius, dot, style = Stroke(1.5.dp.toPx()))
                painters.getOrNull(selected)?.let { painter ->
                    translate(left = dot.x - selectedIconPx / 2f, top = dot.y - selectedIconPx / 2f) {
                        with(painter) {
                            draw(
                                size = Size(selectedIconPx, selectedIconPx),
                                colorFilter = ColorFilter.tint(lineColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.wavePoints(counts: List<Int>, amp: Float, phase: Float): List<Offset> {
    val n = counts.size
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val padX = 16.dp.toPx()
    val top = 22.dp.toPx()
    val bottom = size.height - 24.dp.toPx()
    val usable = (size.width - padX * 2).coerceAtLeast(0f)
    val step = if (n > 1) usable / (n - 1) else 0f
    return counts.mapIndexed { i, c ->
        // Subtracting i shifts the wave so it flows left to right.
        val wave = sin(phase - i * 0.7f) * amp
        Offset(
            x = if (n > 1) padX + step * i else size.width / 2f,
            y = top + (1f - c.toFloat() / max) * (bottom - top) + wave
        )
    }
}

private fun DrawScope.drawWave(
    counts: List<Int>,
    amp: Float,
    phase: Float,
    color: Color,
    strokeWidth: Float,
    fill: Brush?
) {
    val pts = wavePoints(counts, amp, phase)
    if (pts.isEmpty()) return
    val line = Path().apply {
        moveTo(pts.first().x, pts.first().y)
        for (i in 1 until pts.size) {
            val a = pts[i - 1]
            val b = pts[i]
            val midX = (a.x + b.x) / 2f
            cubicTo(midX, a.y, midX, b.y, b.x, b.y)
        }
    }
    if (fill != null) {
        val area = Path().apply {
            addPath(line)
            lineTo(pts.last().x, size.height)
            lineTo(pts.first().x, size.height)
            close()
        }
        drawPath(area, fill)
    }
    drawPath(line, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}
