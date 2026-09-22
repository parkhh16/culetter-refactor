package com.example.myapplication.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.Image
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import com.example.myapplication.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.wear.compose.material.*
import com.example.myapplication.data.Recorder
import com.example.myapplication.data.WearMessageRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

private enum class RecState { Idle, Recording }

@Composable
fun LiveRecordingScreen(
    recorder: Recorder,
    repo: WearMessageRepository,
    onFinished: (Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(RecState.Idle) }

    // ---- 전송 결과 HUD
    var showSendResult by remember { mutableStateOf(false) }
    var lastSendOk by remember { mutableStateOf<Boolean?>(null) }

    // ---- 타이머(경과 시간)
    var elapsedMs by remember { mutableStateOf(0L) }
    var startedAt by remember { mutableStateOf<Long?>(null) }
    var accMs by remember { mutableStateOf(0L) }

    // ---- 마이크 레벨(0..1)
    var level by remember { mutableStateOf(0f) }
    val providedLevelFlow: StateFlow<Float>? = try {
        @Suppress("UNCHECKED_CAST")
        recorder.javaClass.getDeclaredField("levelFlow")
            .apply { isAccessible = true }
            .get(recorder) as? StateFlow<Float>
    } catch (_: Exception) { null }

    // 녹음 중일 때만 레벨 수집/폴링
    LaunchedEffect(state, providedLevelFlow) {
        if (providedLevelFlow != null && state == RecState.Recording) {
            providedLevelFlow.collectLatest { lvl -> level = lvl.coerceIn(0f, 1f) }
        }
    }
    LaunchedEffect(state) {
        if (providedLevelFlow == null) {
            while (true) {
                level = if (state == RecState.Recording) safeGetAmplitudeSmart(recorder) else 0f
                delay(33L)
            }
        }
    }

    // 경과 시간 (Recording 중에만 증가)
    LaunchedEffect(state, startedAt, accMs) {
        while (state == RecState.Recording && startedAt != null) {
            elapsedMs = accMs + (System.currentTimeMillis() - startedAt!!)
            delay(100L)
        }
    }

    // 화면 떠날 때 안전 정리
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(Unit) {
        val obs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_STOP && recorder.isRecording()) recorder.stop()
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    // ---- 애니메이션 준비
    val infinite = rememberInfiniteTransition(label = "loop")
    // 회전/숨결은 "녹음 중일 때만" 실제로 적용
    val sweepAngle by infinite.animateFloat(
        0f, 360f, animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "sweep"
    )
    val pulse by infinite.animateFloat(
        0.97f, 1.03f, animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "pulse"
    )
    val smoothLevel = remember { Animatable(0f) }
    LaunchedEffect(level, state) {
            // 녹음 중일 때만 레벨 반영, 아니면 0으로
            val raw = if (state == RecState.Recording) level.coerceIn(0f, 1f) else 0f
            // 아주 작은 값은 바로 0으로 스냅 (잔상 제거)
            val target = if (raw < 0.03f) 0f else raw
            val curr = smoothLevel.value

            if (target < curr) {
                    // 🔻 하강: 즉시 스냅 → 말 멈추면 바로 줄어듦
                    smoothLevel.snapTo(target)
                } else {
                    // 🔺 상승: 아주 짧게만 보간 (확확 올라가도록)
                    smoothLevel.animateTo(
                            target,
                            animationSpec = tween(
                                    durationMillis = if (target - curr > 0.15f) 60 else 40,
                                    easing = LinearEasing
                                        )
                                )
                }
        }
    val t by rememberTicker()
    val micro = 0.03f * sin(t / 260.0).toFloat()

    // ✅ Idle/Paused: 고정값(정지된 링), Recording: 레벨 반응(+ 약간의 리듬)
    val boosted = sqrt(smoothLevel.value.coerceIn(0f, 1f)) // 작은 소리도 보이게
    val ringReact = when (state) {
        RecState.Recording -> ((boosted + micro + 0.02f) * pulse).coerceIn(0f, 1f)
        RecState.Idle -> 0.15f  // 완전 고정 (움직임/회전 없음)
    }

    // 전송 결과 HUD 자동 숨김
    LaunchedEffect(showSendResult) {
        if (showSendResult) {
            delay(1200L)
            showSendResult = false
        }
    }

    MaterialTheme {
        Scaffold {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // 중앙 네온 링
                NeonRingVisual(
                    showSweep = (state == RecState.Recording), // 녹음 중에만 회전/스윕
                    sweepRotation = sweepAngle,
                    ringLevel = ringReact,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(0.9f)
                )

                // 취소(X): 녹음 상태에서만 노출
                if (state == RecState.Recording) {
                    Button(
                        onClick = {
                            if (recorder.isRecording()) recorder.stop()
                            startedAt = null
                            accMs = 0L
                            elapsedMs = 0L
                            state = RecState.Idle
                            onCancel()
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(10.dp)
                            .size(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0x33FFFFFF),
                            contentColor = Color.White
                        )
                    ) { Icon(Icons.Filled.Close, contentDescription = "취소") }
                }

                // 중앙 하트 버튼 (항상 중앙 고정)
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BigCircleButton(
                        imageRes = R.drawable.recording_active,
                        contentDesc = if (state == RecState.Recording) "녹음 완료" else "녹음 시작",
                        bg = Color.Transparent,
                        diameter = 100.dp,
                        iconSize = 80.dp,
                        onClick = {
                            if (state == RecState.Idle) {
                                // 녹음 시작
                                accMs = 0L
                                elapsedMs = 0L
                                startedAt = System.currentTimeMillis()
                                recorder.start()
                                state = RecState.Recording
                            } else {
                                // 녹음 완료
                                startedAt = null
                                val f = recorder.stop()
                                // 백그라운드로 전송, UI는 즉시 Idle
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) { f?.let { repo.sendVoice(it) } ?: false }
                                    lastSendOk = ok
                                    showSendResult = true
                                    onFinished(ok)
                                }
                                accMs = 0L
                                elapsedMs = 0L
                                state = RecState.Idle
                            }
                        }
                    )

                    Text(
                        text = if (state == RecState.Recording) "터치하여 종료!" else "터치하여 녹음!",
                        color = Color.White,
                        modifier = Modifier.offset(y = (-15).dp)
                    )
                }

                // 녹음 중일 때만 시간 표시 (하트 아래)
                if (state == RecState.Recording) {
                    Text(
                        text = formatElapsed(elapsedMs), 
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 40.dp)
                    )
                }

                // ✅ 전송 결과 HUD (중앙 오버레이)
                if (showSendResult) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(
                                color = Color(0xFF212121),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = if (lastSendOk == true) Color(0xFF00E676) else Color(0xFFFF5252),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (lastSendOk == true) "전송 완료" else "전송 실패",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/* ---------- 네온 스윕 링 ---------- */
@Composable
private fun NeonRingVisual(
    showSweep: Boolean,
    sweepRotation: Float,
    ringLevel: Float,
    modifier: Modifier = Modifier
) {
    // 녹음 중에만 회전
    Canvas(modifier.rotate(if (showSweep) sweepRotation else 0f)) {
        val s = min(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f

        val minR = s * 0.40f
        val maxR = s * 0.60f
        val r = minR + (maxR - minR) * ringLevel

        val thickness = s * (0.035f + 0.065f * ringLevel)

        // 베이스 링(항상 보이도록 희미한 흰색)
        drawCircle(
            color = Color(0x22FFFFFF),
            center = Offset(cx, cy),
            radius = r,
            style = Stroke(width = thickness * 0.5f, cap = StrokeCap.Round)
        )

        if (showSweep) {
            val sweep = Brush.sweepGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.08f to Color(0xFFFF6B9D), // 핑크색
                    0.16f to Color.Transparent,

                    0.48f to Color.Transparent,
                    0.56f to Color(0xFFFF8A80), // 코랄색
                    0.64f to Color.Transparent,

                    1.00f to Color.Transparent
                ),
                center = Offset(cx, cy)
            )
            drawCircle(
                brush = sweep,
                center = Offset(cx, cy),
                radius = r,
                style = Stroke(width = thickness, cap = StrokeCap.Round)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x33FF6B9D), Color.Transparent), // 핑크 글로우
                    center = Offset(cx, cy),
                    radius = r * 1.35f
                ),
                center = Offset(cx, cy),
                radius = r * 1.02f,
                style = Stroke(width = thickness * 1.6f, cap = StrokeCap.Round)
            )
        }
    }
}

/* ---------- 공용 버튼 ---------- */
@Composable
private fun BigCircleButton(
    icon: ImageVector? = null,
    imageRes: Int? = null,
    contentDesc: String,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 56.dp,
    iconSize: Dp = 24.dp
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(diameter),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = bg,
            contentColor = Color.White
        )
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = contentDesc,
                modifier = Modifier.size(iconSize)
            )
        } else if (icon != null) {
            Icon(icon, contentDescription = contentDesc, modifier = Modifier.size(iconSize))
        }
    }
}

/* ---------- 유틸 ---------- */
private fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

/**
 * 0..1 정규화된 레벨.
 * - Recorder.getAmplitude()가 있으면 그것을 사용(0..32767)
 * - 없으면 Recorder 내부의 MediaRecorder(mr)에서 getMaxAmplitude()를 reflection으로 호출
 */
private fun safeGetAmplitudeSmart(recorder: Any): Float {
    fun clamp01(x: Float) = x.coerceIn(0f, 1f)
    fun fromLinear(x: Int): Float = clamp01(x / 32767f)

    return try {
        // 1) Recorder.getAmplitude()
        recorder::class.java.methods
            .firstOrNull { it.name == "getAmplitude" && it.parameterCount == 0 }
            ?.invoke(recorder)
            ?.let { (it as? Number)?.toInt() }
            ?.let { return fromLinear(it) }

        // 2) Recorder 내부 필드 mr (MediaRecorder)
        val mrField = recorder::class.java.declaredFields.firstOrNull { it.name == "mr" }
        val mr = mrField?.apply { isAccessible = true }?.get(recorder)
        val maxAmp = mr?.javaClass?.methods
            ?.firstOrNull { it.name == "getMaxAmplitude" && it.parameterCount == 0 }
            ?.invoke(mr) as? Number

        fromLinear(maxAmp?.toInt() ?: 0)
    } catch (_: Exception) {
        0f
    }
}

@Composable
private fun rememberTicker(periodMs: Long = 16L): State<Long> {
    val st = remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        var t = 0L
        while (true) { t += periodMs; st.value = t; delay(periodMs) }
    }
    return st
}
