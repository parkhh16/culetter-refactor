// app/src/main/java/com/example/myapplication/ui/screens/MailScreen.kt
package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.mail.MailGrid
import com.example.myapplication.ui.components.mail.MailOpenOverlayGif
import com.example.myapplication.ui.components.mail.MailSummaryUi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.time.LocalDateTime
import androidx.compose.foundation.clickable as foundationClickable
import com.example.myapplication.utils.FirebaseTokenManager
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.data.repository.GiftBoxRepository
import com.example.myapplication.data.model.MailItem
import com.example.myapplication.data.model.NftRenderingResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import com.example.myapplication.di.ServiceLocator

@Composable
fun MailScreen(
    hazeState: HazeState? = null
) {
    val viewModel: MailScreenViewModel = viewModel { MailScreenViewModel(ServiceLocator.giftBoxRepository) }
    val haze = hazeState ?: remember { HazeState() }
    var tab by remember { mutableStateOf(MailTab.INBOX) }

    // 상태 관찰
    val inboxItems by viewModel.inboxItems.collectAsState()
    val outboxItems by viewModel.outboxItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // UID 로깅 및 데이터 로드
    val context = LocalContext.current
    val tokenManager = remember { FirebaseTokenManager() }

    LaunchedEffect(Unit) {
        val idToken = tokenManager.getCurrentUserToken()
        val firebaseUid = tokenManager.getCurrentUserId()
        android.util.Log.d("MailScreen", "선물함 진입 - Firebase UID: $firebaseUid")
        android.util.Log.d("MailScreen", "선물함 진입 - 사용자 ID Token: $idToken")
        if (idToken != null) {
            viewModel.loadGiftBoxData(idToken)
        }
    }

    // 현재 탭에 따른 데이터 선택
    val list = if (tab == MailTab.INBOX) inboxItems else outboxItems

    // UI 모델 + 역조회
    val summaries = remember(list) { list.map { it.toUi() } }
    val byId = remember(list) { list.associateBy { it.id } }

    // 클릭된 메일(오버레이 표시용)
    var opening by remember { mutableStateOf<MailItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {

            // 탭 (수신함 | 발신함)
            MailTabs(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
            )

            // 편지 그리드
            MailGrid(
                items = summaries,
                onMailClick = { summary: MailSummaryUi ->
                    byId[summary.id]?.let { opening = it }  // opening: MailItem?
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(18.dp)
            )

        }

        // 편지 열기 애니메이션 + 상세 모달
        opening?.let { mail ->
            var showDetailModal by remember { mutableStateOf(false) }
            val nftDetails by viewModel.nftDetails.collectAsState()

            // 상세 정보 로드
            LaunchedEffect(mail.id) {
                val idToken = tokenManager.getCurrentUserToken()
                if (idToken != null) {
                    viewModel.loadNftDetails(idToken, mail.letterId)
                }
            }

            if (!showDetailModal) {
                // 1단계: GIF 애니메이션
                MailOpenOverlayGif(
                    hazeState = haze,
                    onDismiss = { opening = null },
                    title = nftDetails?.title ?: mail.title,
                    content = nftDetails?.content ?: "내용을 불러오는 중...",
                    partnerLabel = mail.partnerLabel,
                    dateTime = mail.dateTime,
                    onAnimationComplete = { showDetailModal = true }
                )
            } else {
                // 2단계: 상세 모달
                MailDetailModal(
                    visible = true,
                    title = nftDetails?.title ?: mail.title,
                    content = nftDetails?.content ?: "내용을 불러오는 중...",
                    audioUrl = nftDetails?.audioData, // Base64 오디오 데이터
                    onDismiss = {
                        showDetailModal = false
                        opening = null
                        viewModel.clearNftDetails() // 상세 정보 클리어
                    }
                )
            }
        }
    }
}

/* ───────── 탭 UI ───────── */

@Composable
private fun MailTabs(
    selected: MailTab,
    onSelect: (MailTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
    ) {
        @Composable
        fun TabText(tab: MailTab, label: String) {
            val isSel = selected == tab
            Text(
                text = label,
                fontSize = 25.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                color = Color(0x99592813),
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clickableNoIndication { onSelect(tab) }
            )
        }
        TabText(MailTab.INBOX, "수신함")
        Text("|", color = Color(0x99592813), fontSize = 25.sp, modifier = Modifier.padding(horizontal = 4.dp))
        TabText(MailTab.OUTBOX, "발신함")
    }
}

/* ───────── ViewModel ───────── */

class MailScreenViewModel(
    private val giftBoxRepository: GiftBoxRepository
) : ViewModel() {

    private val _inboxItems = MutableStateFlow<List<MailItem>>(emptyList())
    val inboxItems: StateFlow<List<MailItem>> = _inboxItems.asStateFlow()

    private val _outboxItems = MutableStateFlow<List<MailItem>>(emptyList())
    val outboxItems: StateFlow<List<MailItem>> = _outboxItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _nftDetails = MutableStateFlow<NftRenderingResponse?>(null)
    val nftDetails: StateFlow<NftRenderingResponse?> = _nftDetails.asStateFlow()

    fun loadGiftBoxData(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // 발신함과 수신함 동시 로드
            val inboxResult = giftBoxRepository.getReceivedTokens(token)
            val outboxResult = giftBoxRepository.getSentTokens(token)

            inboxResult.fold(
                onSuccess = { _inboxItems.value = it },
                onFailure = { _errorMessage.value = "수신함 로드 실패: ${it.message}" }
            )

            outboxResult.fold(
                onSuccess = { _outboxItems.value = it },
                onFailure = { _errorMessage.value = "발신함 로드 실패: ${it.message}" }
            )

            _isLoading.value = false
        }
    }

    fun loadNftDetails(token: String, letterId: Long) {
        viewModelScope.launch {
            val result = giftBoxRepository.getNftDetails(token, letterId)
            result.fold(
                onSuccess = { _nftDetails.value = it },
                onFailure = { _errorMessage.value = "상세 정보 로드 실패: ${it.message}" }
            )
        }
    }

    fun clearNftDetails() {
        _nftDetails.value = null
    }
}

/* ───────── 모델 ───────── */

private enum class MailTab { INBOX, OUTBOX }

private fun MailItem.toUi() = MailSummaryUi(
    id = id, title = title, dateTime = dateTime, partnerLabel = partnerLabel
)


/* ───────── 헬퍼 ───────── */

@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.then(
        foundationClickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    )
