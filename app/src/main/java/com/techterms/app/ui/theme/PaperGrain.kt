package com.techterms.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/** Faint, seeded paper grain drawn with Canvas. Does not intercept touches. */
@Composable
fun PaperGrain(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val random = Random(seed = 42)
        val dots = ((size.width * size.height) / 600f).toInt().coerceIn(500, 4000)
        repeat(dots) {
            drawCircle(
                color = Color(0xFF5A4636).copy(alpha = 0.02f + random.nextFloat() * 0.05f),
                radius = 0.5f + random.nextFloat() * 0.9f,
                center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
            )
        }
    }
}
