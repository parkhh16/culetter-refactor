// BubbleEntryRow.kt
package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.service.AudioPlayerService
import com.example.myapplication.data.service.PlaybackState

@Composable
fun BubbleEntryRow(
    time: String,
    text: String,
    modifier: Modifier = Modifier,
    bubbleWidth: Dp = 280.dp,
    bubbleHeight: Dp = 64.dp,
    bubbleCorner: Dp = 30.dp,
    bubbleAlpha: Float = 0.40f,
    onClick: (() -> Unit)? = null,          // ⬅️ 추가
    recordId: Int? = null,                  // 녹음 파일 ID
    audioPlayerService: AudioPlayerService? = null, // 오디오 플레이어 서비스
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { m -> if (onClick != null) m.clickable { onClick() } else m }, // ⬅️ 탭 처리
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xCC592813),
            modifier = Modifier.width(64.dp)
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(bubbleWidth)
                .height(bubbleHeight)
                .clip(RoundedCornerShape(bubbleCorner))
                .background(Color.White.copy(alpha = bubbleAlpha))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF6A515E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center

            )
            
            // 재생 버튼 (녹음 파일이 있는 경우에만 표시)
            if (recordId != null && audioPlayerService != null) {
                PlayButton(
                    recordId = recordId,
                    audioPlayerService = audioPlayerService
                )
            }
        }
    }
}

@Composable
private fun PlayButton(
    recordId: Int,
    audioPlayerService: AudioPlayerService
) {
    val playbackState by audioPlayerService.playbackState.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }
    
    // 재생 상태 업데이트
    LaunchedEffect(playbackState) {
        isPlaying = playbackState == PlaybackState.PLAYING
    }
    
    IconButton(
        onClick = {
            if (isPlaying) {
                audioPlayerService.stopAudio()
            } else {
                audioPlayerService.playAudio(recordId)
            }
        },
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "재생 중지" else "재생",
            tint = Color(0xFF6A515E),
            modifier = Modifier.size(20.dp)
        )
    }
}
