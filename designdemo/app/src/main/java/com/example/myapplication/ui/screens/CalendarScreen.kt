package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.myapplication.data.model.CalendarEntry
import com.example.myapplication.ui.components.FrostedCard
import com.example.myapplication.ui.components.calendar.CalendarCard
import com.example.myapplication.ui.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(),
    onRetrospectDetailClick: (String, String) -> Unit = { _, _ -> }
) {
    // 상태
    var currentYm by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    var selected by remember { mutableStateOf(today) }

    // ViewModel 상태 관찰
    val uiState by viewModel.uiState.collectAsState()

    // 월 변경 시 API 호출
    LaunchedEffect(currentYm) {
        viewModel.loadCalendarEntries(currentYm)
    }
    
    // 선택된 날짜 변경 시 상세 데이터 로드
    LaunchedEffect(selected) {
        viewModel.loadCalendarDetail(selected)
    }

    // 선택된 날짜의 회고 데이터 가져오기
    val selectedEntry = viewModel.getEntryForDate(selected)
    

    val progress = (uiState.progress / 100f).coerceIn(0f, 1f)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 캘린더 화면 전용 배경 이미지
        Image(
            painter = painterResource(id = R.raw.calander_screen),
            contentDescription = "캘린더 배경",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
        ) {
            val scroll = rememberScrollState()

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll) // ✅ 전체 화면 스크롤
            ) {
            // ── 상단 진행 바 + 퍼센트 칩
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(284.dp)
                        .height(21.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(284.dp * progress.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF9A6B))
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(45.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xCC592813)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 달력 카드
            CalendarCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                yearMonth = currentYm,
                selected = selected,
                badges = uiState.badgeDates,
                badgeColors = uiState.badgeColors,
                onMonthChanged = { ym -> currentYm = ym },
                onSelectDate = { picked -> selected = picked }
            )

            Spacer(Modifier.height(16.dp))

            // 선택한 날짜
            Text(
                text = selected.format(java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월 d일")),
                fontSize = 20.sp,
                color = Color(0xCC592813),
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            // "데일리 회고" 칩
            FrostedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable {
                        if (selectedEntry != null) {
                            onRetrospectDetailClick(selectedEntry.title, selectedEntry.content)
                        }
                    },
                corner = 15.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.EditNote,
                            contentDescription = "회고 아이콘",
                            tint = Color(0xCC592813),
                            modifier = Modifier.size(40.dp)
                        )
                    }
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
                        if (selectedEntry != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = selectedEntry.title,
                                fontSize = 14.sp,
                                color = Color(0xCC592813),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 선택된 날짜의 기록
            FrostedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                corner = 20.dp,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
            ) {
                if (uiState.isLoadingDetail) {
                    // 로딩 상태
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "기록을 불러오는 중...",
                            fontSize = 16.sp,
                            color = Color(0xCC6A515E)
                        )
                    }
                } else if (uiState.detailError != null) {
                    // 에러 상태
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "기록을 불러올 수 없습니다.",
                            fontSize = 16.sp,
                            color = Color(0xCC6A515E)
                        )
                    }
                } else if (uiState.calendarDetail?.records?.isNotEmpty() == true) {
                    // 실제 기록 데이터 표시
                    val detailData = uiState.calendarDetail
                    if (detailData != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            detailData.records.forEachIndexed { index, record ->
                                TimelineRow(record.time, record.summaryText)
                                if (index < detailData.records.size - 1) {
                                    DividerThin()
                                }
                            }
                        }
                    }
                } else {
                    // 기록이 없는 경우
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "이 날은 기록이 없습니다.",
                            fontSize = 16.sp,
                            color = Color(0xCC6A515E)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            }
        }
    }
}

@Composable
private fun TimelineRow(time: String, text: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFAE88))
        )
        Spacer(Modifier.width(12.dp))
        Text(time, fontSize = 18.sp, color = Color(0xCC592813))
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontSize = 18.sp, color = Color(0xCC6A515E), maxLines = 1)
        }
    }
}

@Composable
private fun DividerThin() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.Black.copy(alpha = 0.08f))
    )
}


