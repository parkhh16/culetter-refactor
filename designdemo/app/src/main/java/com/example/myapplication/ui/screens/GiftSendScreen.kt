// app/src/main/java/com/example/myapplication/ui/screens/GiftSendScreen.kt
package com.example.myapplication.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.FrostedCard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable as foundationClickable
import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.model.NftMintingRequest
import com.example.myapplication.data.model.NftMintingResult
import com.example.myapplication.data.service.ZipUploadService
import com.example.myapplication.data.service.AudioRecorderService
import com.example.myapplication.utils.FirebaseTokenManager
import kotlinx.coroutines.launch

/* 검색 결과 상태 */
private enum class SearchResult { Idle, Found, NotFound }

@Composable
fun GiftSendScreen(
    letterId: Int,
    onClose: () -> Unit = {},
    onGiftDone: () -> Unit = {}
) {
    val haze = remember { HazeState() } // FrostedCard용
    val brown = Color(0xFF6B4F3B)
    val pillShape = RoundedCornerShape(14.dp)
    val grad = Brush.horizontalGradient(
        listOf(Color(0xFFF79A75), Color(0xFFF6C88E), Color(0xFFDA98F6))
    )

    // --- UI State ---
    var email by remember { mutableStateOf("") }
    var result by remember { mutableStateOf(SearchResult.Idle) }
    var agreed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var foundUser by remember { mutableStateOf<com.example.myapplication.data.model.UserInfo?>(null) }
    var isGiftSending by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val tokenManager = remember { FirebaseTokenManager() }
    val context = LocalContext.current
    val zipUploadService = remember { ZipUploadService(context) }
    val audioRecorderService = remember { AudioRecorderService(context) }

    // 발신일(날짜/시간) — 기본값
    var sendDate by remember { mutableStateOf(LocalDate.now()) }
    var sendTime by remember { mutableStateOf(LocalTime.of(21, 0)) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    fun launchDatePicker() {
        DatePickerDialog(
            context,
            { _, y, m, d -> sendDate = LocalDate.of(y, m + 1, d) },
            sendDate.year, sendDate.monthValue - 1, sendDate.dayOfMonth
        ).show()
    }

    fun launchTimePicker() {
        TimePickerDialog(
            context,
            { _, h, min -> sendTime = LocalTime.of(h, min) },
            sendTime.hour, sendTime.minute, true
        ).show()
    }

    // 🔎 실제 API 호출로 이메일 검색
    fun doSearch() {
        if (email.trim().isEmpty()) return
        
        coroutineScope.launch {
            isLoading = true
            try {
                // Firebase 토큰 가져오기
                val token = tokenManager.getCurrentUserToken()
                if (token == null) {
                    // 토큰이 없으면 검색 실패로 처리
                    result = SearchResult.NotFound
                    foundUser = null
                    isLoading = false
                    return@launch
                }
                
                // Authorization 헤더에 토큰 포함하여 API 호출
                val response = ApiClient.authApiService.searchUserByEmail(
                    authorization = "Bearer $token",
                    email = email.trim()
                )
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 1000) {
                        // 회원 조회 성공
                        foundUser = apiResponse.result
                        result = SearchResult.Found
                    } else {
                        // 일치하는 회원 없음
                        result = SearchResult.NotFound
                        foundUser = null
                    }
                } else {
                    // API 호출 실패
                    result = SearchResult.NotFound
                    foundUser = null
                }
            } catch (e: Exception) {
                // 네트워크 오류 등
                result = SearchResult.NotFound
                foundUser = null
            } finally {
                isLoading = false
                agreed = false // 상태 초기화
            }
        }
    }

    // 🎁 선물하기 (NFT 민팅 + ZIP 업로드)
    fun sendGift() {
        val user = foundUser ?: return
        
        coroutineScope.launch {
            isGiftSending = true
            try {
                // Firebase 토큰 가져오기
                val token = tokenManager.getCurrentUserToken()
                if (token == null) {
                    // 토큰이 없으면 선물 실패로 처리
                    isGiftSending = false
                    return@launch
                }
                
                // 날짜/시간을 ISO 형식으로 변환
                val reservationDate = "${sendDate}T${sendTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}"
                
                // NFT 민팅 요청 데이터 생성
                val request = NftMintingRequest(
                    receiverEmail = user.email,
                    reservationDate = reservationDate,
                    letterId = letterId
                )
                
                // NFT 민팅 API 호출 (JSON 파싱 오류 무시)
                android.util.Log.d("GiftSend", "NFT 민팅 API 호출 시작")
                try {
                    val response = ApiClient.nftApiService.mintNft(
                        authorization = "Bearer $token",
                        request = request
                    )
                    android.util.Log.d("GiftSend", "NFT 민팅 응답: ${response.code()}, 성공: ${response.isSuccessful}")
                    android.util.Log.d("GiftSend", "NFT 민팅 응답 본문: ${response.body()}")
                } catch (e: Exception) {
                    android.util.Log.w("GiftSend", "NFT 민팅 API 호출 중 JSON 파싱 오류 (무시하고 진행): ${e.message}")
                }
                
                // NFT 민팅 API 호출 후 바로 ZIP 업로드 수행 (성공 여부 관계없이)
                android.util.Log.d("GiftSend", "NFT 민팅 API 호출 완료 - ZIP 업로드 시작")
                        
                        try {
                            // 1. 사용자 ID 가져오기
                            android.util.Log.d("GiftSend", "1단계: 사용자 ID 가져오기 시작")
                            val userId = tokenManager.getCurrentUserId()
                            android.util.Log.d("GiftSend", "사용자 ID: $userId")
                            if (userId == null) {
                                android.util.Log.e("GiftSend", "사용자 ID가 null입니다")
                                // TODO: 에러 메시지 표시
                                return@launch
                            }
                            
                            // 2. 편지 상세 정보 가져오기
                            android.util.Log.d("GiftSend", "2단계: 편지 상세 정보 조회 시작")
                            val letterResponse = ApiClient.letterApiService.getLetterDetail(
                                token = "Bearer $token",
                                letterId = letterId
                            )
                            
                            android.util.Log.d("GiftSend", "편지 상세 정보 응답: ${letterResponse.isSuccessful}, code: ${letterResponse.body()?.code}")
                            if (!letterResponse.isSuccessful || letterResponse.body()?.code != 1000) {
                                android.util.Log.e("GiftSend", "편지 상세 정보 조회 실패")
                                // TODO: 에러 메시지 표시
                                return@launch
                            }
                            
                            val letter = letterResponse.body()!!.result
                            android.util.Log.d("GiftSend", "편지 정보: title=${letter.title}, content=${letter.content}")
                            
                            // 3. 앱데이터의 모든 녹음 파일 ID 가져오기
                            android.util.Log.d("GiftSend", "3단계: 녹음 파일 ID 가져오기 시작")
                            val recordIds = audioRecorderService.getAllRecordingIds()
                            android.util.Log.d("GiftSend", "녹음 파일 ID 목록: $recordIds")
                            
                            if (recordIds.isEmpty()) {
                                // 녹음 파일이 없는 경우에도 편지만으로 ZIP 생성
                                android.util.Log.d("GiftSend", "4단계: ZIP 업로드 시작 (편지만)")
                                val uploadResult = zipUploadService.uploadGiftBundle(
                                    userId = userId,
                                    letterId = letterId.toString(),
                                    recordIds = emptyList(),
                                    letterTitle = letter.title,
                                    letterContent = letter.content
                                )
                                
                                if (uploadResult.isSuccess) {
                                    // ZIP 업로드 성공
                                    android.util.Log.d("GiftSend", "ZIP 업로드 성공 (편지만)")
                                    onGiftDone()
                                } else {
                                    android.util.Log.e("GiftSend", "ZIP 업로드 실패")
                                    // TODO: 에러 메시지 표시
                                }
                            } else {
                                // 4. ZIP 파일 생성 및 업로드
                                android.util.Log.d("GiftSend", "4단계: ZIP 업로드 시작 (녹음파일 포함)")
                                val uploadResult = zipUploadService.uploadGiftBundle(
                                    userId = userId,
                                    letterId = letterId.toString(),
                                    recordIds = recordIds,
                                    letterTitle = letter.title,
                                    letterContent = letter.content
                                )
                                
                                if (uploadResult.isSuccess) {
                                    // ZIP 업로드 성공
                                    android.util.Log.d("GiftSend", "ZIP 업로드 성공 (녹음파일 포함)")
                                    onGiftDone()
                                } else {
                                    android.util.Log.e("GiftSend", "ZIP 업로드 실패")
                                    // TODO: 에러 메시지 표시
                                }
                            }
                            
                        } catch (e: Exception) {
                            android.util.Log.e("GiftSend", "ZIP 업로드 중 오류 발생", e)
                            // TODO: 에러 메시지 표시
                        }
            } catch (e: Exception) {
                // 네트워크 오류 등 - 에러 처리 필요
                android.util.Log.e("GiftSend", "선물하기 전체 과정에서 오류 발생", e)
                // TODO: 에러 메시지 표시
            } finally {
                android.util.Log.d("GiftSend", "선물하기 과정 완료")
                isGiftSending = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .hazeSource(haze)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // 닫기
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "닫기",
            tint = brown,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clickableNoIndication { onClose() }
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 50.dp)
        ) {
            // 제목
            Text(
                "누구에게 선물을 보내시겠어요?",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = brown
            )

            Spacer(Modifier.height(18.dp))

            // 이메일 입력 + 검색 아이콘 (카드 밖)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientOutlinedField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "이메일을 입력해주세요.",
                    modifier = Modifier.weight(1f),
                    shape = pillShape,
                    grad = grad,
                    textColor = brown
                )
                Spacer(Modifier.width(10.dp))
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = brown,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "검색",
                        tint = brown,
                        modifier = Modifier
                            .size(24.dp)
                            .clickableNoIndication { doSearch() }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 경계선
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFD4C4B8))
            )

            Spacer(Modifier.height(16.dp))

            // ===== 아래 전부 FrostedCard 안으로(수신인 ~ 발신일 ~ 동의 ~ 선물하기) =====
            FrostedCard(
                hazeState = haze,
                corner = 22.dp,
                borderAlpha = 0.35f,
                highlightAlpha = 0.05f,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                when (result) {
                    SearchResult.Idle -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "검색 후 수신자를 확인하세요.",
                                color = brown.copy(alpha = 0.65f),
                                fontSize = 14.sp
                            )
                        }
                    }

                    SearchResult.NotFound -> {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "일치하는 사용자가 없습니다.\n지갑생성 요청을 보내세요.",
                                color = brown, fontSize = 16.sp, lineHeight = 22.sp
                            )
                            Spacer(Modifier.height(18.dp))
                            GradientButton(
                                text = "요청하기", grad = grad, enabled = true,
                                onClick = { /* TODO */ }
                            )
                        }
                    }

                    SearchResult.Found -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            // 수신인
                            LabeledPillRow(label = "수신인", brown = brown) {
                                Pill(
                                    text = foundUser?.nickname ?: email,
                                    shape = pillShape,
                                    textColor = brown,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(Modifier.height(32.dp))

                            // 발신일 (두 칩이 겹쳐보이지 않도록 가로폭 반씩)
                            LabeledPillRow(label = "발신일", brown = brown) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Pill(
                                        text = sendDate.format(dateFmt),
                                        shape = pillShape,
                                        textColor = brown,
                                        clickable = true,
                                        onClick = { launchDatePicker() },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Pill(
                                        text = sendTime.format(timeFmt),
                                        shape = pillShape,
                                        textColor = brown,
                                        clickable = true,
                                        onClick = { launchTimePicker() },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(Modifier.height(40.dp))

                            // 안내 문구
                            Text(
                                "${sendDate.year}년 ${sendDate.monthValue}월 ${sendDate.dayOfMonth}일 ${foundUser?.nickname ?: "수신인"}에게\n마음을 전달하시겠습니까?",
                                color = brown, fontSize = 16.sp, lineHeight = 22.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(24.dp))

                            // 동의
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = agreed,
                                    onCheckedChange = { agreed = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF28B2A))
                                )
                                Text("동의", color = brown, fontSize = 16.sp)
                            }

                            Spacer(Modifier.height(40.dp))

                            // 선물하기 버튼 (같은 카드 내부)
                            GradientButton(
                                text = if (isGiftSending) "선물 중..." else "선물하기",
                                grad = grad,
                                enabled = agreed && !isGiftSending,
                                onClick = { sendGift() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ────── 작은 컴포넌트들 ────── */

@Composable
private fun LabeledPillRow(
    label: String,
    brown: Color,
    content: @Composable RowScope.() -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = brown, fontSize = 16.sp, modifier = Modifier.width(64.dp))
        Text(":", color = brown, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Row(modifier = Modifier.weight(1f), content = content)
    }
}

@Composable
private fun Pill(
    text: String,
    shape: RoundedCornerShape,
    textColor: Color,
    clickable: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = Color(0x99592813)
    val base = modifier
        .height(36.dp)
        .background(Color.White, shape)
        .border(1.dp, borderColor, shape)

    val mod = if (clickable) base.clickableNoIndication { onClick() } else base

    Box(mod, contentAlignment = Alignment.Center) {
        Text(
            text,
            color = textColor,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GradientOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier,
    shape: RoundedCornerShape,
    grad: Brush,
    textColor: Color
) {
    Box(modifier) {
        // 그라데이션 외곽선
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(2.dp, grad, shape)
                .padding(2.dp)
        ) {
            // 내부
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.9f), shape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text("이메일을 입력해주세요.", color = Color(0xFFB9ADA2))
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun GradientButton(
    text: String,
    grad: Brush,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = Color(0xFFBBAFA5)
        ),
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(
                if (enabled) Modifier.background(grad, shape)
                else Modifier.background(Color(0xFFEAE2DB), shape)
            )
    ) {
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

/* ripple 없는 클릭 */
@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.then(
        foundationClickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    )
