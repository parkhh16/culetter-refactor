package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.runtime.remember

/**
 * Glassmorphism 스타일 카드
 *
 * - 배경 블러: hazeChild + HazeMaterials.ultraThin()
 * - 유리 틴트: 반투명 화이트
 * - 보더: 얇은 그라데이션
 * - 하이라이트 + 비네트로 유리 두께감
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FrostedCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,          // ← 선택 항목
    corner: Dp = 20.dp,     // ← 호출부와 동일한 이름 유지
    borderWidth: Dp = 1.dp,
    borderAlpha: Float = 0.0f,
    vignetteAlpha: Float = 0.00f,
    highlightAlpha: Float = 0.00f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.90f),
            Color.White.copy(alpha = 0.40f),
            Color.White.copy(alpha = 0.90f)
        )
    ),
    content: @Composable () -> Unit,
) {
    val effectiveHaze = hazeState ?: remember { HazeState() } // 안전 폴백
    val shape = RoundedCornerShape(corner)

    Box(
        modifier = modifier
            .clip(shape) // ← 모양은 clip으로
            .hazeEffect(
                state = effectiveHaze,
                style = HazeMaterials.ultraThin()
            ){
                // 🔥 블러 강도 업
                blurRadius = 90.dp          // 기본 20.dp → 체감 확 올리기 (34~44.dp 권장) :contentReference[oaicite:2]{index=2}

                // 🌫️ 질감(당신 버전에 맞는 이름)
                noiseFactor = 0.10f         // 0.0~0.15 권장 :contentReference[oaicite:3]{index=3}

                // ✨ 가독성 강화: 배경색/알파
                // (틴트 객체 직접 생성 없이도 충분히 효과 낼 수 있음)
                alpha = 1f
                backgroundColor = Color.White.copy(alpha = 0.18f)
                // 필요하면 mask/progressive도 여기에 설정 가능
            }
            .background(Color.White.copy(alpha = 0.6f)) // 필요 없으면 줄이거나 제거
            .border(               // ✅ 하얀색 보더 추가
                width = 1.dp,
                color = Color.White.copy(alpha = 0.8f),
                shape = shape
            )
            .padding(contentPadding)

    ) {
        // 데코레이션 레이어(보더/하이라이트/비네트)
        Canvas(Modifier.matchParentSize()) {
            val r = corner.toPx()
            val w = size.width
            val h = size.height

            // 하이라이트
            if (highlightAlpha > 0f) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = highlightAlpha),
                            Color.Transparent
                        ),
                        start = Offset(w * 0.0f, h * 0.0f),
                        end   = Offset(w * 0.8f, h * 0.8f)
                    ),
                    cornerRadius = CornerRadius(r, r)
                )
            }

            // 비네트
            if (vignetteAlpha > 0f) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = vignetteAlpha)
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = size.minDimension * 0.95f
                    ),
                    cornerRadius = CornerRadius(r, r)
                )
            }

            // 그라데이션 보더
            if (borderAlpha > 0f && borderWidth.value > 0f) {
                drawRoundRect(
                    brush = borderBrush,
                    style = Stroke(width = borderWidth.toPx()),
                    cornerRadius = CornerRadius(r, r),
                    alpha = borderAlpha
                )
            }
        }

        content()
    }
}