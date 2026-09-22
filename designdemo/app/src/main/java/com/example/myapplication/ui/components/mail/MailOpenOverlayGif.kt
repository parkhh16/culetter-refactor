package com.example.myapplication.ui.components.mail

import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.myapplication.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable as foundationClickable
import dev.chrisbanes.haze.HazeState // 시그니처 유지용(내부 미사용)

@Composable
@Suppress("UNUSED_PARAMETER")
fun MailOpenOverlayGif(
    hazeState: HazeState,                 // ← 호출부 호환 위해 유지(내부 미사용)
    onDismiss: () -> Unit,
    title: String,
    content: String,
    partnerLabel: String,
    dateTime: LocalDateTime,
    gifResId: Int = R.drawable.ic_envelope_open,
    gifDurationMs: Int = 2000,            // 디코더가 길이 못 줄 때 fallback
    scrimAlpha: Float = 0.2f,            // 배경 딤 투명도
    scrimColor: Color = Color.Black,      // 배경 딤 색
    fillWidthFraction: Float = 0.7f,      // GIF 가로폭 비율
    useDialog: Boolean = true,            // ✅ 기본값: 전체화면 보장
    onAnimationComplete: (() -> Unit)? = null // 애니메이션 완료 콜백
) {
    val fmt = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm") }
    val context = androidx.compose.ui.platform.LocalContext.current

    // GIF 로더
    val imageLoader = remember(context) {
        ImageLoader.Builder(context).components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
            else add(GifDecoder.Factory())
        }.build()
    }

    // GIF 종료 → 본문 표시
    var showContent by remember { mutableStateOf(false) }
    var endScheduled by remember { mutableStateOf(false) }

    val request = remember {
        ImageRequest.Builder(context)
            .data(gifResId)
            .setParameter("coil#repeat_count", 1) // 가능한 디코더에서 1회 재생
            .build()
    }
    val painter = rememberAsyncImagePainter(model = request, imageLoader = imageLoader)
    val state = painter.state

    LaunchedEffect(state) {
        if (state is AsyncImagePainter.State.Success && !endScheduled) {
            endScheduled = true
            val d = state.result.drawable
            if (Build.VERSION.SDK_INT >= 28 && d is AnimatedImageDrawable) {
                try { d.repeatCount = 1 } catch (_: Throwable) {}
                d.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        showContent = true
                        onAnimationComplete?.invoke()
                        d.unregisterAnimationCallback(this)
                    }
                })
                d.start()
            } else {
                val totalMs = resolveGifTotalDurationMs(d) ?: gifDurationMs
                kotlinx.coroutines.delay(totalMs.toLong())
                showContent = true
                onAnimationComplete?.invoke()
            }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = scrimAlpha,
        animationSpec = tween(220),
        label = "gif-scrim"
    )

    // 공통 콘텐츠(배경 딤 + GIF + 본문)
    @Composable
    fun OverlayBody() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor.copy(alpha = alpha))
                .foundationClickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    // 본문이 뜬 뒤에만 바깥 탭으로 닫기
                    if (showContent) onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            // 1) GIF
            AnimatedVisibility(
                visible = !showContent,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200))
            ) {
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth(fillWidthFraction)
                        .wrapContentHeight()
                )
            }

            // 2) 본문 카드 — 중앙에서 살짝 커지며 등장
            AnimatedVisibility(
                visible = showContent,
                enter =
                    fadeIn(animationSpec = tween(durationMillis = 420, delayMillis = 100)) +
                            scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(durationMillis = 560, delayMillis = 160, easing = FastOutSlowInEasing)
                            ),
                exit =
                    fadeOut(animationSpec = tween(durationMillis = 220)) +
                            scaleOut(
                                targetScale = 0.98f,
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                            ),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    // 카드 내부 탭은 흡수(닫히지 않게)
                    .foundationClickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
            ) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    color = Color(0xFFFDFDFD)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0x99592813))
                        Spacer(Modifier.height(6.dp))
                        Text("${dateTime.format(fmt)}  ·  $partnerLabel", fontSize = 12.sp, color = Color(0x99592813))
                        Spacer(Modifier.height(14.dp))
                        Text(content, fontSize = 15.sp, color = Color(0x99592813), lineHeight = 20.sp)
                        Spacer(Modifier.height(16.dp))
                        Box(
                            Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                                .foundationClickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                    onDismiss()
                                }
                        ) {
                            Text("닫기", color = Color(0x99592813), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    // ✅ 기본: Dialog 로 전체화면 보장 (Scaffold 바텀바까지 덮음)
    if (useDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) { OverlayBody() }
    } else {
        // 필요 시 화면 내부 오버레이로도 사용 가능(컨텐트 영역만 덮음)
        OverlayBody()
    }
}

/** GIF 총 길이 최대한 정확히 구하기 (여러 디코더 대응) */
private fun resolveGifTotalDurationMs(drawable: Drawable): Int? {
    val c = drawable.javaClass
    try { c.getMethod("setLoopCount", Int::class.javaPrimitiveType).invoke(drawable, 1) } catch (_: Throwable) {}
    try { val m = c.getMethod("getDuration"); val v = m.invoke(drawable); if (v is Int && v > 0) return v } catch (_: Throwable) {}
    try { val m = c.getMethod("getTotalDuration"); val v = m.invoke(drawable); if (v is Int && v > 0) return v } catch (_: Throwable) {}
    try {
        val framesM = c.getMethod("getNumberOfFrames")
        val count = (framesM.invoke(drawable) as? Int) ?: return null
        val frameDurM = c.getMethod("getFrameDuration", Int::class.javaPrimitiveType)
        var sum = 0
        for (i in 0 until count) sum += (frameDurM.invoke(drawable, i) as? Int ?: 0)
        if (sum > 0) return sum
    } catch (_: Throwable) {}
    return null
}
