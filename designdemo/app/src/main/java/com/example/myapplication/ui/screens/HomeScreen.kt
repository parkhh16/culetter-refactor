package com.example.myapplication.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.ui.components.TodaySection
import com.example.myapplication.ui.components.FrostedCard
import com.example.myapplication.ui.components.VideoBackground
import com.example.myapplication.ui.components.buttons.GlowyCircleButton
import com.example.myapplication.ui.viewmodel.HomeViewModel
import com.example.myapplication.data.service.AudioPlayerService
import com.example.myapplication.data.state.RecordState
import com.example.myapplication.data.model.Retrospect
import androidx.compose.foundation.layout.BoxWithConstraints
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import android.util.Log
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRecordClick: () -> Unit,
    onRetrospectClick: () -> Unit,
    onRecordDetailClick: (com.example.myapplication.data.model.Record) -> Unit,
    onRetrospectDetailClick: (String, String) -> Unit,
    onThemeSetupClick: () -> Unit,
    hazeState: HazeState,
    shouldRefresh: Boolean = false
) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    // 화면 진입 시마다 데이터 새로고침
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "초기 데이터 로드")
        viewModel.refresh()
    }
    
    // 테마 설정 후 돌아올 때 새로고침
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            Log.d("HomeScreen", "테마 설정 후 새로고침")
            viewModel.refresh()
        }
    }
    
    // 스토리가 없을 때 테마 설정 화면으로 이동 (약간의 지연을 주어 GET 요청 완료 대기)
    LaunchedEffect(uiState.hasNoStory) {
        Log.d("HomeScreen", "hasNoStory 상태 변경: ${uiState.hasNoStory}")
        if (uiState.hasNoStory) {
            Log.d("HomeScreen", "스토리가 없음 - 500ms 후 테마 설정 화면으로 이동")
            delay(500) // GET 요청이 완료될 시간을 줌
            // 다시 한번 상태 확인 (GET 요청이 완료되었을 수 있음)
            if (uiState.hasNoStory) {
                Log.d("HomeScreen", "여전히 스토리가 없음 - 테마 설정 화면으로 이동")
                onThemeSetupClick()
            } else {
                Log.d("HomeScreen", "GET 요청 완료로 스토리 발견 - 홈 화면 유지")
            }
        }
    }
    
    // 오디오 플레이어 서비스 (실제로는 Context에서 초기화해야 함)
    val audioPlayerService = remember { 
        // Context는 실제로는 Activity에서 가져와야 함
        // 여기서는 임시로 null 처리
        null
    }
    
    // 선택한 항목(시간, 내용)
    var selected by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val circleSize = w * 0.388f // 필요시 사용

        Image(
            painter = painterResource(id = R.raw.home_screen_1),
            contentDescription = "캘린더 배경",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            
            // 테마 제목과 D+ 칩을 같은 행에 배치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        uiState.storyData?.theme?.split(" ")?.getOrNull(0) ?: "우리 아이의",
                        color = Color(0x99592813),
                        fontSize = (w * 0.097f).value.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        uiState.storyData?.theme?.split(" ")?.drop(1)?.joinToString(" ") ?: "첫 백일",
                        color = Color(0x99592813),
                        fontSize = (w * 0.097f).value.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                FrostedCard(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(start = 8.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(45.dp),
                            ambientColor = Color.Black.copy(alpha = 0.3f),
                            spotColor = Color.Black.copy(alpha = 0.3f)
                        ),
                    hazeState = hazeState,
                    corner = 45.dp,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        "D+${(uiState.storyData?.daysFromStart ?: 0) + 1}",
                        color = Color(0xCC592813),
                        fontSize = 25.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 중앙 버튼 2개
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 기록 버튼 (record_icon.png)
                Image(
                    painter = painterResource(id = R.drawable.record_icon),
                    contentDescription = "기록 버튼",
                    modifier = Modifier
                        .size(160.dp)
                        .clickable { onRecordClick() },
                    contentScale = ContentScale.Fit
                )

                // 회고 버튼 (reflect_icon.png)
                Image(
                    painter = painterResource(id = R.drawable.reflect_icon),
                    contentDescription = "회고 버튼",
                    modifier = Modifier
                        .size(160.dp)
                        .clickable { onRetrospectClick() },
                    contentScale = ContentScale.Fit
                )
            }

 

            Spacer(Modifier.height(16.dp))

            // 오늘의 기록 카드
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                uiState.error != null && !uiState.hasNoStory -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "오류가 발생했습니다",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = uiState.error ?: "알 수 없는 오류",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f)
                                )
                            ) {
                                Text("다시 시도", color = Color.White)
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 회고 섹션 (회고가 있을 때만 표시)
                        uiState.storyData?.retrospect?.let { retrospect ->
                            FrostedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clickable { 
                                        onRetrospectDetailClick(retrospect.title, retrospect.content)
                                    },
                                hazeState = hazeState,
                                corner = 15.dp,
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.4f))
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "데일리 회고",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0x99592813)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = retrospect.title,
                                            fontSize = 14.sp,
                                            color = Color(0xCC592813),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 오늘의 기록 섹션
                        val records = uiState.storyData?.records?.map { record ->
                            record.time to record.summaryText
                        } ?: emptyList()
                        
                        val recordIds = uiState.storyData?.records?.map { record ->
                            record.id
                        } ?: emptyList()
                        
                        TodaySection(
                            entries = records,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            onEntryClick = { time, text ->
                                // 해당 기록 찾기
                                val record = uiState.storyData?.records?.find { record ->
                                    record.time == time && record.summaryText == text
                                }
                                
                                if (record != null) {
                                    RecordState.setSelectedRecord(record)
                                    onRecordDetailClick(record)
                                } else {
                                    selected = time to text
                                    sheetOpen = true
                                }
                            },
                            hazeState = hazeState,
                            recordIds = recordIds,
                            audioPlayerService = audioPlayerService
                        )
                    }
                }
            }
        }

        // 모달 바텀시트
        if (sheetOpen && selected != null) {
            ModalBottomSheet(
                onDismissRequest = { sheetOpen = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                val (time, text) = selected!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("상세 보기", fontSize = 18.sp)
                    Text("시간: $time", fontSize = 16.sp)
                    Text("내용: $text", fontSize = 16.sp)

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { sheetOpen = false }) {
                            Text("닫기")
                        }
                    }
                }
            }
        }
    }
}

