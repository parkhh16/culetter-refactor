package com.example.myapplication.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Record
import com.example.myapplication.data.service.AudioPlayerService
import com.example.myapplication.data.service.PlaybackState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * 기록 상세 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    record: Record,
    onBackClick: () -> Unit,
    hazeState: HazeState
) {
    val context = LocalContext.current
    val audioPlayerService = remember { AudioPlayerService(context) }
    val playbackState by audioPlayerService.playbackState.collectAsState()
    
    // 컴포넌트 정리
    DisposableEffect(Unit) {
        onDispose {
            audioPlayerService.cleanup()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .haze(hazeState)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color(0x99592813)
                )
            }
            
            Text(
                "기록 상세",
                color = Color(0x99592813),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp)) // 균형 맞추기
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 메인 콘텐츠
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 시간 정보
            TimeInfoCard(time = record.time)
            
            // 요약 정보
            SummaryCard(summaryText = record.summaryText)
            
            // 전문 정보
            TranscriptCard(transcriptText = record.transcriptText)
            
            // 녹음 재생
            AudioPlayerCard(
                recordId = record.id,
                audioPlayerService = audioPlayerService,
                playbackState = playbackState
            )
        }
    }
}

@Composable
private fun TimeInfoCard(time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "기록 시간",
                color = Color(0xCC592813),
                fontSize = 14.sp
            )
            Text(
                text = time,
                color = Color(0x99592813),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryCard(summaryText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "요약",
                color = Color(0xCC592813),
                fontSize = 14.sp
            )
            Text(
                text = summaryText,
                color = Color(0x99592813),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun TranscriptCard(transcriptText: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "전문",
                color = Color(0xCC592813),
                fontSize = 14.sp
            )
            Text(
                text = transcriptText ?: "전문이 없습니다.",
                color = Color(0xCC592813),
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun AudioPlayerCard(
    recordId: Int,
    audioPlayerService: AudioPlayerService,
    playbackState: PlaybackState
) {
    var isPlaying by remember { mutableStateOf(false) }
    
    // 재생 상태 업데이트
    LaunchedEffect(playbackState) {
        isPlaying = playbackState == PlaybackState.PLAYING
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "녹음 재생",
                color = Color(0xCC592813),
                fontSize = 14.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 재생 버튼
                PlayButton(
                    isPlaying = isPlaying,
                    onPlayPause = {
                        if (isPlaying) {
                            audioPlayerService.stopAudio()
                        } else {
                            audioPlayerService.playAudio(recordId)
                        }
                    }
                )
                
                // 상태 표시
                Text(
                    text = if (isPlaying) "재생 중..." else "재생 대기",
                    color = Color(0xCC592813),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PlayButton(
    isPlaying: Boolean,
    onPlayPause: () -> Unit
) {
    // 재생 중 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "playback")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x80FF6B6B),
                        1.0f to Color(0x80FF4757)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(80.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "재생 중지" else "재생",
                tint = Color(0x99592813),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
