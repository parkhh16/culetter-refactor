package com.example.myapplication.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 임시 화면(플레이스홀더)로 쓰는 중앙 정렬 텍스트.
 * 실제 화면 준비되면 교체하세요.
 */
@Composable
fun SimpleCenter(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = label, color = Color(0x99592813), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}
