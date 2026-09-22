// app/src/main/java/com/example/myapplication/ui/screens/LetterEditScreen.kt
package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.ui.components.FrostedCard
import com.example.myapplication.ui.viewmodel.LetterEditViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun LetterEditScreen(
    hazeState: HazeState? = null,
    title: String = "보통을 채우는 커피",
    initialContent: String = "블라블라블라블라블 ....\n블라블라블라블라블 ....",
    onBack: () -> Unit = {},
    onSaveAndGoDrawer: () -> Unit = {}   // ← 저장 후 Drawer로 이동
) {
    val haze = hazeState ?: remember { HazeState() }
    val viewModel: LetterEditViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var body by rememberSaveable {
        mutableStateOf(initialContent)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.raw.letter_edit_screen),
            contentDescription = "편지 편집 배경",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .haze(haze)
                .systemBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 10.dp)
        ) {
            // 뒤로가기 버튼
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.Start)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color(0x99592813),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // 제목 (내용 카드와 같은 너비)
            Text(
                text = title,
                fontSize = 35.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0x99592813),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp)
            )

            Spacer(Modifier.height(20.dp))

            // 본문 카드
            FrostedCard(
                hazeState = haze,
                corner = 20.dp,
                borderAlpha = 0.35f,
                highlightAlpha = 0.06f,
                contentPadding = PaddingValues(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp)
            ) {
                TextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = {
                        Text(
                            "블라블라블라블라블 ...\n블라블라블라블라블 ....",
                            color = Color(0xFF6A515E).copy(alpha = 0.6f),
                            fontSize = 18.sp,
                            lineHeight = 21.sp
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        lineHeight = 21.sp,
                        color = Color(0xFF6A515E)
                    ),
                    modifier = Modifier.fillMaxSize(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFFF99161),
                        focusedTextColor = Color(0xFF6A515E),
                        unfocusedTextColor = Color(0xFF6A515E)
                    ),
                    singleLine = false,
                    minLines = 8
                )
            }

            Spacer(Modifier.height(40.dp))

            // 저장 버튼
            FrostedCard(
                hazeState = haze,
                corner = 40.dp,
                borderAlpha = 0.35f,
                highlightAlpha = 0.06f,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(226.dp, 64.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = { 
                        viewModel.saveLetter(title, body) {
                            onSaveAndGoDrawer()
                        }
                    },
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFFF99161),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color(0xFFF99161).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFF99161),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "저장하기", 
                            fontSize = 26.sp, 
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(86.dp))

            // 에러 메시지
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = Color.Red.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
