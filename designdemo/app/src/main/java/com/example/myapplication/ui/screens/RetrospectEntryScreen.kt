// file: ui/screens/RetrospectEntryScreen.kt
package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.FrostedCard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetrospectEntryScreen(
    onTextClick: () -> Unit,
    onCallClick: () -> Unit,
    onBackClick: () -> Unit,
    hazeState: HazeState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .haze(hazeState)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.Black
                )
            }
            Text(
                text = "회고 방법 선택",
                color = Color.Black,
                fontSize = 20.sp
            )
            Spacer(Modifier.width(48.dp)) // 오른쪽 균형용
        }

        // 안내
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "회고를 진행할 방법을 선택하세요.",
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = "텍스트로 입력하거나, 전화 회고 전용 페이지로 이동할 수 있어요.",
                    color = Color.Black.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }
        }

        // ① 텍스트로 입력
        FrostedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clickable { onTextClick() },
            hazeState = hazeState,
            corner = 16.dp,
            contentPadding = PaddingValues(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "텍스트로 입력",
                    tint = Color.White
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "텍스트로 입력",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "키보드로 작성하는 기존 회고 화면으로 이동합니다.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ② 전화하기 (새 페이지로 이동)
        FrostedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clickable { onCallClick() },
            hazeState = hazeState,
            corner = 16.dp,
            contentPadding = PaddingValues(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "전화하기",
                    tint = Color.White
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "전화하기",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "전화 회고 전용 페이지로 이동합니다.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
