package com.lchat.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.lchat.app.ui.neumorphism.NeumorphicStyle
import com.lchat.app.ui.neumorphism.neumorphic

@Composable
fun NeumorphicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    style: NeumorphicStyle = NeumorphicStyle.Raised,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .neumorphic(shape = shape, style = style)
            .padding(16.dp)
    ) {
        content()
    }
}