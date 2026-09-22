@file:Suppress("unused")

package com.example.myapplication.ui.components.buttons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GlowyCircleButton (customizable colors)
 * - 내부 그라디언트, 얇은 링, 상단 하이라이트, 아이콘 틴트까지 커스터마이즈 가능
 */
@Composable
fun GlowyCircleButton(
    diameter: Dp,
    icon: ImageVector,

    // ✅ 색/브러시 커스터마이즈 포인트들
    fillBrush: Brush = Brush.verticalGradient(
        listOf(Color(0xFFFFE3BE), Color(0xFFFF8D64)) // 살구 → 코랄
    ),
    ringColor: Color = Color.White.copy(alpha = 0.55f),     // 얇은 유리 링
    ringWidth: Dp = 1.5.dp,
    highlightColor: Color = Color.White.copy(alpha = 0.45f),// 상단 하이라이트
    highlightRadiusFactor: Float = 0.9f,                    // r * factor
    highlightYOffsetFactor: Float = 0.45f,                  // r * factor(위로)

    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsState().value
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.85f),
        label = "pressScale"
    ).value

    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension / 2f
            val ringInset = 8.dp.toPx()

            // 1) 내부 채움 (사용자가 준 브러시)
            drawCircle(
                brush = fillBrush,
                radius = r - ringInset
            )

            // 2) 얇은 링
            drawCircle(
                color = ringColor,
                radius = r - ringInset,
                style = Stroke(width = ringWidth.toPx())
            )

            // 3) 상단 하이라이트 (떠 보이는 느낌)
            val hl = Offset(center.x, center.y - r * highlightYOffsetFactor)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(highlightColor, Color.Transparent),
                    center = hl,
                    radius = r * highlightRadiusFactor
                ),
                center = hl,
                radius = r * highlightRadiusFactor
            )
        }

        // 4) 아이콘
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(diameter * 0.4f)
        )
    }
}
