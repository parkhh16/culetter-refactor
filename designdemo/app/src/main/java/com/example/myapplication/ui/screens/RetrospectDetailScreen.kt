package com.example.myapplication.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.VideoBackground
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun RetrospectDetailScreen(
    title: String,
    content: String,
    onBackClick: () -> Unit,
    hazeState: HazeState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 비디오
        VideoBackground(
            videoUri = Uri.parse("android.resource://com.example.myapplication/raw/reflection_detail_screen"),
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
                .systemBarsPadding()
        ) {
            // 상단 네비게이션 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color(0x99592813),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(49.dp))

            // 제목 "데일리 회고"
            Text(
                text = "데일리 회고",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0x99592813),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    lineHeight = 48.sp
                )
            )

            Spacer(Modifier.height(66.dp))

            // 제목 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 37.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(57.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xCC592813),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 25.sp
                    )
                }
            }

            Spacer(Modifier.height(43.dp))

            // 내용 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 35.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(347.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(24.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = content,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6A515E),
                        lineHeight = 28.sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    )
                }
            }
        }
    }
}
