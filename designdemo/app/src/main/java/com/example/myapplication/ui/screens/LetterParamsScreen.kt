// app/src/main/java/com/example/myapplication/ui/screens/LetterParamsScreen.kt
package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.components.FrostedCard
import com.example.myapplication.ui.viewmodel.LetterParamsViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable as foundationClickable

@Composable
fun LetterParamsScreen(
    onBack: () -> Unit = {},
    onLetterCreated: (String, String) -> Unit = { _, _ -> } // title, content
) {
    val viewModel: LetterParamsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var to by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("") }

    // 편지 생성 완료 시 LetterEditScreen으로 이동
    LaunchedEffect(uiState.generatedTitle, uiState.generatedContent) {
        if (uiState.generatedTitle.isNotEmpty() && uiState.generatedContent.isNotEmpty()) {
            onLetterCreated(uiState.generatedTitle, uiState.generatedContent)
        }
    }

    val brown = Color(0xFF6B4F3B)
    val hint = Color(0xFFB9ADA2)
    val divider = Color(0xFFE7DDD4)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // 하얀색 배경
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
        
        when {
            uiState.isLoading -> {
                // 로딩 상태 - RecordScreen과 동일한 방식
                LoadingContent()
            }
            else -> {
                // 상단 Back 버튼
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 4.dp, top = 4.dp)
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "뒤로가기", tint = brown)
                }

                // 본문 카드
                FrostedCard(
                    hazeState = null,
                    corner = 22.dp,
                    borderAlpha = 0.35f,
                    highlightAlpha = 0.05f,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 52.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "더욱 진심이 담긴 편지를 만들 수 있도록\n아래 질문에 답해주세요.",
                            color = brown,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )

                        Spacer(Modifier.height(12.dp))
                        Divider(color = divider, thickness = 1.dp)
                        Spacer(Modifier.height(8.dp))

                        NumberedField(
                            number = 1,
                            korLabel = "편지를 받는 사람을 부르는 애칭",
                            enLabel = "To",
                            value = to,
                            onValueChange = { to = it },
                            placeholder = "예: 엄마, 자기야, 내 단짝",
                            brown = brown,
                            hint = hint
                        )

                        Spacer(Modifier.height(8.dp))

                        NumberedField(
                            number = 2,
                            korLabel = "편지를 받는 분이 나를 부르는 애칭",
                            enLabel = "From",
                            value = from,
                            onValueChange = { from = it },
                            placeholder = "예: 딸, 곰돌이, 영원한 친구",
                            brown = brown,
                            hint = hint
                        )

                        Spacer(Modifier.height(8.dp))

                        NumberedField(
                            number = 3,
                            korLabel = "편지에 담고 싶은 말투",
                            enLabel = "Tone",
                            value = tone,
                            onValueChange = { tone = it },
                            placeholder = "예: 다정한, 무뚝뚝한",
                            brown = brown,
                            hint = hint
                        )

                        Spacer(Modifier.height(8.dp))

                        NumberedField(
                            number = 4,
                            korLabel = "원하시는 편지의 분위기",
                            enLabel = "Mood",
                            value = mood,
                            onValueChange = { mood = it },
                            placeholder = "예: 사랑이 넘치는, 애정을 담은",
                            brown = brown,
                            hint = hint
                        )

                        Spacer(Modifier.height(40.dp))

                        // 확인 버튼
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.createLetter(to.trim(), from.trim(), tone.trim(), mood.trim())
                                },
                                enabled = to.trim().isNotEmpty() && from.trim().isNotEmpty() && tone.trim().isNotEmpty() && mood.trim().isNotEmpty(),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.7f),
                                    contentColor = Color(0xFFE67E22),
                                    disabledContainerColor = Color.White.copy(alpha = 0.3f),
                                    disabledContentColor = Color(0xFFE67E22).copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(52.dp)
                                    .border(
                                        width = 1.dp,
                                        color = Color.Gray.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                            ) {
                                Text("확인", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // 에러 메시지
                        if (uiState.error != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = uiState.error!!,
                                color = Color.Red.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------ 로딩 컴포넌트 ------------ */

@Composable
private fun LoadingContent() {
    val loadingMessages = listOf(
        "기록들을 레티가 정리중입니다.",
        "편지가 잠시후 완성됩니다."
    )
    
    var currentMessageIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000) // 3초 대기
            currentMessageIndex = (currentMessageIndex + 1) % loadingMessages.size
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color(0x99592813),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = loadingMessages[currentMessageIndex],
            color = Color(0x99592813),
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

/* ------------ 작은 조각들 ------------ */

@Composable
private fun NumberedField(
    number: Int,
    korLabel: String,
    enLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    brown: Color,
    hint: Color
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("$number.", color = brown, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(6.dp))
        Text(korLabel, color = brown, fontSize = 15.sp)
    }
    Spacer(Modifier.height(6.dp))
    Text(enLabel, color = brown.copy(alpha = 0.8f), fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = {
            Text(
                placeholder,
                color = hint,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = hint.copy(alpha = 0.6f),
            unfocusedBorderColor = hint.copy(alpha = 0.45f),
            cursorColor = brown,
            focusedContainerColor = Color.White.copy(alpha = 0.65f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.55f),
            focusedTextColor = brown,
            unfocusedTextColor = brown
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    )
}

/* ------------ 모델 ------------ */
data class LetterParams(
    val to: String,
    val from: String,
    val tone: String,
    val mood: String
)

/* ripple 없는 클릭 확장(필요 시 사용) */
@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.then(
        foundationClickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    )