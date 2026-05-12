package com.lchat.app.ui.neumorphism

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lchat.app.ui.theme.NeuShadowDark
import com.lchat.app.ui.theme.NeuShadowLight
import com.lchat.app.ui.theme.NeuSurface
import androidx.compose.ui.graphics.nativeCanvas

enum class NeumorphicStyle {
    Raised,
    Pressed
}

fun Modifier.neumorphic(
    shape: Shape = RoundedCornerShape(20.dp),
    style: NeumorphicStyle = NeumorphicStyle.Raised,
    color: Color = NeuSurface,
    lightShadowColor: Color = NeuShadowLight,
    darkShadowColor: Color = NeuShadowDark,
    elevation: Dp = 3.dp
): Modifier = composed {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val elevationPx = with(density) { elevation.toPx() }
    val blurPx = elevationPx * 2f

    drawBehind {
        drawIntoCanvas { canvas ->
            val outline = shape.createOutline(size, layoutDirection, this)

            when (style) {
                NeumorphicStyle.Raised -> {
                    drawShadowLayer(
                        canvas = canvas.nativeCanvas,
                        size = size,
                        offset = Offset(elevationPx, elevationPx),
                        blur = blurPx,
                        color = darkShadowColor,
                        outline = outline
                    )
                    drawShadowLayer(
                        canvas = canvas.nativeCanvas,
                        size = size,
                        offset = Offset(-elevationPx, -elevationPx),
                        blur = blurPx,
                        color = lightShadowColor,
                        outline = outline
                    )
                }
                NeumorphicStyle.Pressed -> {
                    drawShadowLayer(
                        canvas = canvas.nativeCanvas,
                        size = size,
                        offset = Offset(-elevationPx / 2, -elevationPx / 2),
                        blur = blurPx,
                        color = darkShadowColor,
                        outline = outline
                    )
                    drawShadowLayer(
                        canvas = canvas.nativeCanvas,
                        size = size,
                        offset = Offset(elevationPx / 2, elevationPx / 2),
                        blur = blurPx,
                        color = lightShadowColor,
                        outline = outline
                    )
                }
            }
        }
    }
        .drawBehind {
            drawIntoCanvas { canvas ->
                val outline = shape.createOutline(size, layoutDirection, this)
                when (outline) {
                    is androidx.compose.ui.graphics.Outline.Rounded ->
                        canvas.nativeCanvas.drawRoundRect(
                            0f, 0f, size.width, size.height,
                            outline.roundRect.topLeftCornerRadius.x,
                            outline.roundRect.topLeftCornerRadius.y,
                            android.graphics.Paint().apply {
                                this.color = color.toArgb()
                                isAntiAlias = true
                            }
                        )
                    is androidx.compose.ui.graphics.Outline.Rectangle ->
                        canvas.nativeCanvas.drawRect(
                            0f, 0f, size.width, size.height,
                            android.graphics.Paint().apply {
                                this.color = color.toArgb()
                                isAntiAlias = true
                            }
                        )
                    else -> drawRect(color)
                }
            }
        }
}

private fun drawShadowLayer(
    canvas: android.graphics.Canvas,
    size: Size,
    offset: Offset,
    blur: Float,
    color: Color,
    outline: androidx.compose.ui.graphics.Outline
) {
    val paint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        isAntiAlias = true
        maskFilter = android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    when (outline) {
        is androidx.compose.ui.graphics.Outline.Rounded -> {
            val rx = outline.roundRect.topLeftCornerRadius.x
            val ry = outline.roundRect.topLeftCornerRadius.y
            canvas.drawRoundRect(
                offset.x,
                offset.y,
                size.width + offset.x,
                size.height + offset.y,
                rx, ry, paint
            )
        }
        is androidx.compose.ui.graphics.Outline.Rectangle -> {
            canvas.drawRect(
                offset.x,
                offset.y,
                size.width + offset.x,
                size.height + offset.y,
                paint
            )
        }
        else -> {
            canvas.drawRect(
                offset.x,
                offset.y,
                size.width + offset.x,
                size.height + offset.y,
                paint
            )
        }
    }
}