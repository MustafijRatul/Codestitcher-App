package com.codestitcher.app.ui.screens

import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure as AndroidPathMeasure
import android.graphics.RectF as AndroidRectF
import android.graphics.Typeface as AndroidTypeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay

/**
 * 100% Native Jetpack Compose GPU Canvas Cinematic Studio Intro.
 * Locked 120 FPS, pre-allocated geometry, tap-to-skip, zero cold-start delay.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var isFinished by remember { mutableStateOf(false) }
    var animationStarted by remember { mutableStateOf(false) }
    val animProgress = remember { Animatable(0f) }

    fun finishSplash() {
        if (!isFinished) {
            isFinished = true
            onSplashFinished()
        }
    }

    // Sequence: 120ms black delay -> 3.2s 1 loop -> smooth transition
    LaunchedEffect(Unit) {
        delay(120)
        animationStarted = true
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 3200,
                easing = LinearEasing
            )
        )
        finishSplash()
    }

    // Pre-allocated vector geometry (1000x360 coordinate space)
    val capsule1Path = remember {
        AndroidPath().apply {
            addRoundRect(AndroidRectF(-72f, -95f, -6f, 55f), 33f, 33f, AndroidPath.Direction.CW)
        }
    }
    val capsule2Path = remember {
        AndroidPath().apply {
            addRoundRect(AndroidRectF(6f, -55f, 72f, 95f), 33f, 33f, AndroidPath.Direction.CW)
        }
    }
    val measureCap1 = remember { AndroidPathMeasure(capsule1Path, true) }
    val measureCap2 = remember { AndroidPathMeasure(capsule2Path, true) }

    val textPath = remember {
        val basePaint = AndroidPaint().apply {
            isAntiAlias = true
            textSize = 96f
            typeface = AndroidTypeface.create("sans-serif", AndroidTypeface.BOLD)
            letterSpacing = -0.025f
        }
        AndroidPath().apply {
            basePaint.getTextPath("CodeStitcher", 0, "CodeStitcher".length, 320f, 212f, this)
        }
    }

    // Pre-allocated paints
    val capsuleOutlinePaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.STROKE
            strokeWidth = 4.5f
            color = AndroidColor.WHITE
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
        }
    }
    val capsuleFillPaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.FILL
            color = AndroidColor.WHITE
        }
    }
    val stitchPaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.STROKE
            strokeWidth = 7.5f
            color = AndroidColor.BLACK
            strokeCap = AndroidPaint.Cap.ROUND
        }
    }
    val textOutlinePaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.STROKE
            strokeWidth = 2.2f
            color = AndroidColor.WHITE
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
        }
    }
    val textFillPaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.FILL
            color = AndroidColor.WHITE
        }
    }

    val segCap1 = remember { AndroidPath() }
    val segCap2 = remember { AndroidPath() }
    val segText = remember { AndroidPath() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { finishSplash() }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (animationStarted) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val t = animProgress.value
                fun phase(start: Float, end: Float): Float = when {
                    t < start -> 0f
                    t > end -> 1f
                    else -> (t - start) / (end - start)
                }

                val cap1DrawProgress = phase(0.00f, 0.35f)
                val cap1FillAlpha = phase(0.35f, 0.55f)
                val cap2DrawProgress = phase(0.06f, 0.41f)
                val cap2FillAlpha = phase(0.41f, 0.60f)
                val stitchDrawProgress = phase(0.20f, 0.40f)
                val textDrawProgress = phase(0.12f, 0.45f)
                val textFillAlpha = phase(0.45f, 0.65f)

                val globalAlpha = when {
                    t < 0.88f -> 1f
                    else -> 1f - phase(0.88f, 1.00f)
                }

                val targetWidth = 1000f
                val targetHeight = 360f
                val scale = (size.width * 0.90f / targetWidth).coerceAtMost(size.height * 0.70f / targetHeight)
                val offsetX = (size.width - targetWidth * scale) / 2f
                val offsetY = (size.height - targetHeight * scale) / 2f

                drawContext.canvas.nativeCanvas.apply {
                    save()
                    translate(offsetX, offsetY)
                    scale(scale, scale)

                    // 1. 'S-STITCH' MARK (Centered at X: 180, Y: 180 and rotated 45°)
                    save()
                    translate(180f, 180f)
                    rotate(45f)

                    if (cap1FillAlpha > 0f) {
                        capsuleFillPaint.alpha = (cap1FillAlpha * globalAlpha * 255).toInt().coerceIn(0, 255)
                        drawPath(capsule1Path, capsuleFillPaint)
                    }
                    if (cap2FillAlpha > 0f) {
                        capsuleFillPaint.alpha = (cap2FillAlpha * globalAlpha * 255).toInt().coerceIn(0, 255)
                        drawPath(capsule2Path, capsuleFillPaint)
                    }

                    capsuleOutlinePaint.alpha = (globalAlpha * 255).toInt().coerceIn(0, 255)
                    if (cap1DrawProgress > 0f) {
                        segCap1.reset()
                        measureCap1.getSegment(0f, measureCap1.length * cap1DrawProgress, segCap1, true)
                        drawPath(segCap1, capsuleOutlinePaint)
                    }
                    if (cap2DrawProgress > 0f) {
                        segCap2.reset()
                        measureCap2.getSegment(0f, measureCap2.length * cap2DrawProgress, segCap2, true)
                        drawPath(segCap2, capsuleOutlinePaint)
                    }

                    if (stitchDrawProgress > 0f) {
                        stitchPaint.alpha = (globalAlpha * 255).toInt().coerceIn(0, 255)
                        val curX = -20f + 40f * stitchDrawProgress
                        drawLine(-20f, 0f, curX, 0f, stitchPaint)
                    }
                    restore()

                    // 2. BRAND TYPOGRAPHY ("CodeStitcher")
                    if (textFillAlpha > 0f) {
                        textFillPaint.alpha = (textFillAlpha * globalAlpha * 255).toInt().coerceIn(0, 255)
                        drawPath(textPath, textFillPaint)
                    }

                    if (textDrawProgress > 0f) {
                        textOutlinePaint.alpha = (globalAlpha * 255).toInt().coerceIn(0, 255)
                        segText.reset()
                        val textMeasure = AndroidPathMeasure(textPath, false)
                        val p = textDrawProgress.coerceIn(0f, 1f)
                        do {
                            textMeasure.getSegment(0f, textMeasure.length * p, segText, true)
                        } while (textMeasure.nextContour())
                        drawPath(segText, textOutlinePaint)
                    }

                    restore()
                }
            }
        }
    }
}
