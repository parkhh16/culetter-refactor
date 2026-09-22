// DrawerComponents.kt
package com.example.myapplication.ui.screens.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CELL_SIZE = 92.dp
private val CELL_GAP = 12.dp
private val PULL_DISTANCE = 84.dp
private const val PULL_MILLIS = 560

// 라벨(12sp 1줄) + 위 Spacer(6dp) 합친 여유 공간
private val LABEL_SPACE = 24.dp

@Composable
fun DrawerCabinet(
    modifier: Modifier = Modifier,
    rows: Int = 3,
    cols: Int = 3,
    cellSize: Dp = CELL_SIZE,
    cellGap: Dp = CELL_GAP,
    openIndex: Int?,
    onOpenChange: (Int?) -> Unit,
    labels: List<String> = emptyList(),
    onOpenRectChange: (Rect?) -> Unit = {},
    onPressStart: () -> Unit = {} // ✅ 추가: 터치 순간 콜백 (스크림 즉시 표시용)
) {
    val total = rows * cols
    val rects = remember { MutableList<Rect?>(total) { null } }

    // 열린 셀의 Rect를 부모로 알려주기
    LaunchedEffect(openIndex, rects) {
        onOpenRectChange(openIndex?.let { rects.getOrNull(it) })
    }

    val cabinetW = cellSize * cols + cellGap * (cols - 1)
    // 🔧 라벨 공간까지 포함해 그리드 전체 높이 계산
    val perItemH = cellSize + LABEL_SPACE
    val cabinetH = perItemH * rows + cellGap * (rows - 1)

    Box(
        modifier = modifier
            .size(cabinetW, cabinetH) // ← 이제 라벨 포함 높이로 고정
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.spacedBy(cellGap),
            verticalArrangement = Arrangement.spacedBy(cellGap),
            userScrollEnabled = false
        ) {
            items((0 until total).toList()) { i ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.wrapContentHeight()
                ) {
                    DrawerCell(
                        index = i,
                        size = cellSize,
                        isOpen = openIndex == i,
                        onClick = { onOpenChange(if (openIndex == i) null else i) },
                        onMeasured = { rect ->
                            rects[i] = rect
                            if (openIndex == i) onOpenRectChange(rect)
                        },
                        onPressStart = onPressStart // ✅ 전달
                    )
                    Spacer(Modifier.height(6.dp))
                    val label = labels.getOrNull(i)
                    if (label != null) {
                        androidx.compose.material3.Text(
                            text = label,
                            fontSize = 12.sp,
                            color = Color(0xFF6B5A4D),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(Modifier.height(18.dp)) // 라벨과 동일한 높이 유지 (12sp + 패딩)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerCell(
    index: Int,
    size: Dp,
    isOpen: Boolean,
    onClick: () -> Unit,
    onMeasured: (Rect) -> Unit,
    onPressStart: () -> Unit // ✅ 추가
) {
    val density = LocalDensity.current
    val faceShape = RoundedCornerShape(14.dp)

    val t by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(PULL_MILLIS, easing = FastOutSlowInEasing),
        label = "pull-$index"
    )

    val slideY = (-PULL_DISTANCE) * t
    val lift = (-6).dp * t
    val transYPx = with(density) { (slideY + lift).toPx() }

    val scale = lerp(1f, 1.06f, t)
    val tilt = lerp(0f, -2.2f, t)
    val elev = lerp(8f, 42f, t)

    Box(
        modifier = Modifier
            .size(size)
            .onGloballyPositioned { coords -> onMeasured(coords.boundsInRoot()) }
            .shadow(8.dp, faceShape)
            .clip(faceShape)
            .background(
                Brush.verticalGradient(listOf(Color(0xFFF1E6D6), Color(0xFFE5D7C4)))
            )
            // 🔁 clickable → pointerInput + detectTapGestures(onPress)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // 터치 즉시 스크림 켜기
                        onPressStart()
                        // 손을 "뗀" 경우에만 열기 수행 (취소/드래그는 무시)
                        val released = tryAwaitRelease()
                        if (released) onClick()
                    }
                )
            }
            .padding(8.dp)
    ) {
        val cavityShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(cavityShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFCEC1AC), Color(0xFFB4A48B), Color(0xFF8D7B61))
                    )
                )
                .padding(6.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(cavityShape)
                    .background(
                        Brush.verticalGradient(listOf(Color(0x33000000), Color(0x66000000)))
                    )
            )
        }

        val drawerShape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = transYPx
                    scaleX = scale
                    scaleY = scale
                    rotationX = tilt
                    shadowElevation = elev
                }
                .clip(drawerShape)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFF1E6D6), Color(0xFFE5D7C4)))
                )
                .padding(8.dp)
        ) {
            val trayH by animateDpAsState(
                targetValue = if (isOpen) 26.dp else 18.dp,
                animationSpec = tween(PULL_MILLIS),
                label = "tray-$index"
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(trayH)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFEDE3D2), Color(0xFFD8C9AE))
                        )
                    )
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x00FFFFFF)))
                    )
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(width = 28.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x99555555))
            )
        }
    }
}

private fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t
