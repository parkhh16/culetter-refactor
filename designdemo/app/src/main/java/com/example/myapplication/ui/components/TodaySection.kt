// TodaySection.kt
package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.service.AudioPlayerService
import dev.chrisbanes.haze.HazeState

@Composable
fun TodaySection(
    entries: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    // Bubble
    bubbleWidth: Dp = 280.dp,
    bubbleHeight: Dp = 64.dp,
    bubbleCorner: Dp = 30.dp,
    bubbleAlpha: Float = 0.40f,
    onEntryClick: ((time: String, text: String) -> Unit)? = null, // ⬅️ 추가,
    hazeState: HazeState? = null,
    // Audio playback
    recordIds: List<Int>? = null, // 각 기록의 ID 리스트
    audioPlayerService: AudioPlayerService? = null // 오디오 플레이어 서비스
) {
    FrostedCard(
        modifier = modifier,
        contentPadding = PaddingValues(start = 22.dp, top = 18.dp, end = 22.dp, bottom = 18.dp),
        hazeState = hazeState,
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = "오늘의 기록",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color(0xCC592813),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(entries.size) { index ->
                    val (time, text) = entries[index]
                    val recordId = recordIds?.getOrNull(index)
                    
                    BubbleEntryRow(
                        time = time,
                        text = text,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        bubbleWidth = bubbleWidth,
                        bubbleHeight = bubbleHeight,
                        bubbleCorner = bubbleCorner,
                        bubbleAlpha = bubbleAlpha,
                        onClick = onEntryClick?.let { { it(time, text) } }, // ⬅️ 전달
                        recordId = recordId,
                        audioPlayerService = audioPlayerService
                    )
                }
            }
        }
    }
}
