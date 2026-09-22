// app/src/main/java/com/example/myapplication/ui/screens/DrawerScreen.kt
package com.example.myapplication.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.R
import com.example.myapplication.ui.components.FrostedCard
import com.example.myapplication.ui.screens.components.DrawerCabinet
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.clickable as foundationClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.foundation.border
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.model.Letter
import com.example.myapplication.ui.viewmodel.DrawerViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DrawerScreen(
    onCreateLetterClick: () -> Unit = {},
    onNavigateToGiftSend: (Int) -> Unit = {}     // 편지 ID를 받는 콜백으로 변경
) {
    val haze = remember { HazeState() }
    val viewModel: DrawerViewModel = viewModel()
    val letters by viewModel.letters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // 화면 진입 시마다 데이터 새로고침
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // 서랍 상태
    var openIndex by remember { mutableStateOf<Int?>(null) }
    var originRect by remember { mutableStateOf<Rect?>(null) }

    // 편지 본문 모달 상태(한 번만 표출)
    var showSend by remember { mutableStateOf(false) }
    var sendTitle by remember { mutableStateOf("") }
    var sendBody by remember { mutableStateOf("") }
    var sendLetterId by remember { mutableStateOf(0) }

    // 편지 제목들을 서랍 라벨로 사용 (최대 9개)
    val drawerLabels = letters.take(9).map { it.title }

    // 셀 열리면 바로 모달 띄울 준비
    LaunchedEffect(openIndex, originRect) {
        if (openIndex != null && originRect != null && letters.isNotEmpty()) {
            val selectedLetter = letters.getOrNull(openIndex!!)
            if (selectedLetter != null) {
                sendTitle = selectedLetter.title
                sendBody = selectedLetter.content
                sendLetterId = selectedLetter.id
                showSend = true
            }
        }
    }

    val closeAll: () -> Unit = {
        openIndex = null
        originRect = null
        showSend = false
    }

    Box(Modifier.fillMaxSize()) {
        // 서랍 화면 전용 배경 이미지
        Image(
            painter = painterResource(id = R.raw.drawer_screen),
            contentDescription = "서랍 배경",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 컨텐츠 (상단 버튼 + 서랍 카드)
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(haze)
                .systemBarsPadding()
        ) {
            // 상단 Frosted 버튼
            FrostedCard(
                hazeState = haze,
                corner = 22.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(horizontal = 2.dp)
                        .clickableNoIndication { onCreateLetterClick() }
                ) {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = Color(0xFF6B4F3B), modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("편지 생성", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B4F3B))
                }
            }

            // 서랍 카드
            FrostedCard(
                hazeState = haze,
                corner = 24.dp,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = 80.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(372.dp), // DrawerCabinet과 동일한 높이 (116dp * 3 + 12dp * 2)
                            contentAlignment = Alignment.Center
                        ) {
                            Text("편지를 불러오는 중...", color = Color(0xFF6B4F3B))
                        }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(372.dp), // DrawerCabinet과 동일한 높이
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("오류가 발생했습니다", color = Color(0xFF6B4F3B))
                                Spacer(Modifier.height(8.dp))
                                Text(error!!, color = Color(0xFF6B4F3B).copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }
                    else -> {
                        // 편지 유무와 관계없이 항상 빈 서랍 9개 표시
                        DrawerCabinet(
                            openIndex = openIndex,
                            onOpenChange = { openIndex = it },
                            labels = drawerLabels,
                            onOpenRectChange = { originRect = it }
                        )
                    }
                }
            }
        }

        // ✅ Dialog 없이 동일 트리에 오버레이로 띄우기
        if (showSend && originRect != null) {
            LetterSendModalFromRect(
                hazeState = haze,
                originRect = originRect!!,
                title = sendTitle,
                body = sendBody,
                onClose = { closeAll() },
                onSend = {
                    // 기존 모달 닫고
                    openIndex = null
                    originRect = null
                    // 새 페이지로 이동 (편지 ID 전달)
                    onNavigateToGiftSend(sendLetterId)
                }
            )
        }
        
    }
}

/** ripple 없는 클릭 */
@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.then(
        foundationClickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    )

@Composable
private fun LetterSendModalFromRect(
    hazeState: HazeState,           // ← 받되 내부에서는 사용하지 않습니다(호출부 수정 불필요)
    originRect: Rect,
    title: String,
    body: String,
    onClose: () -> Unit,
    onSend: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 전 화면 스크림(항상 동일)
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickableNoIndication { onClose() }
        ) {
            // 애니 값
            var started by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { started = true }

            val t by animateFloatAsState(
                targetValue = if (started) 1f else 0f,
                animationSpec = tween(520, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "modal-fly"
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                val centerX = with(density) { maxWidth.toPx() / 2f }
                val centerY = with(density) { maxHeight.toPx() / 2f }
                val startCx = originRect.left + originRect.width / 2f
                val startCy = originRect.top + originRect.height / 2f
                val dx = startCx - centerX
                val dy = startCy - centerY
                val tx = dx * (1f - t)
                val ty = dy * (1f - t)

                val scale = 0.36f + (1f - 0.36f) * t
                val alpha = t

                // 🔒 Offscreen 합성으로 클리핑/반투명 안전
                Box(
                    modifier = Modifier
                        .size(320.dp, 620.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .graphicsLayer {
                            translationX = tx
                            translationY = ty
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .clickableNoIndication { /* consume */ }
                ) {
                    // 배경 이미지
                    Image(
                        painter = painterResource(id = R.raw.letter_send_modal),
                        contentDescription = "편지 발송 모달 배경",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // 컨텐츠 오버레이
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 닫기 버튼 (우상단)
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "닫기",
                            tint = Color(0x99592813),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 12.dp, end = 20.dp)
                                .size(40.dp)
                                .clickableNoIndication { onClose() }
                        )

                        // 메인 컨텐츠 (Column으로 순차 배치)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 60.dp, start = 10.dp, end = 10.dp, bottom = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 제목 - 스크롤 가능
                            Box(
                                modifier = Modifier
                                    .height(76.dp) // 2줄 높이 (38sp * 2)
                            ) {
                                val titleScrollState = rememberScrollState()
                                Text(
                                    text = title,
                                    color = Color(0x99592813),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 38.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(titleScrollState)
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            // 본문 카드
                            Box(
                                modifier = Modifier
                                    .size(260.dp, 350.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .padding(24.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                Text(
                                    text = body,
                                    color = Color(0xFF6A515E),
                                    fontSize = 18.sp,
                                    lineHeight = 21.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            // 전송하기 버튼
                            Box(
                                modifier = Modifier
                                    .size(180.dp, 55.dp)
                                    .clip(RoundedCornerShape(40.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(40.dp))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "전송하기",
                                        color = Color(0x99592813),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickableNoIndication { onSend() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}