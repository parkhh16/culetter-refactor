package com.example.myapplication.ui.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.myapplication.ui.components.FrostedCard
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarCard(
    modifier: Modifier = Modifier,
    yearMonth: YearMonth = YearMonth.now(),
    selected: LocalDate? = LocalDate.now(),
    badges: Set<LocalDate> = emptySet(),
    badgeColors: Map<LocalDate, String> = emptyMap(), // 날짜별 색상 매핑
    onMonthChanged: (YearMonth) -> Unit = {},
    onSelectDate: (LocalDate) -> Unit = {},
) {
    var ym by remember { mutableStateOf(yearMonth) }
    var sel by remember { mutableStateOf(selected) }
    val today = LocalDate.now()

    FrostedCard(
        modifier = modifier,
        corner = 20.dp,
        borderAlpha = 0.00f,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // 🔧 여기부터 Column으로 감싸기!
        Column(modifier = Modifier.fillMaxWidth()) {

            // 헤더
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${ym.year} ${ym.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF794C37)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ChevronLeft, contentDescription = "prev",
                        tint = Color(0xFF794C37),
                        modifier = Modifier
                            .size(28.dp)
                            .clickableNoRipple {
                                ym = ym.minusMonths(1)
                                onMonthChanged(ym)
                            }
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.ChevronRight, contentDescription = "next",
                        tint = Color(0xFF794C37),
                        modifier = Modifier
                            .size(28.dp)
                            .clickableNoRipple {
                                ym = ym.plusMonths(1)
                                onMonthChanged(ym)
                            }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 요일
            val days = listOf("일","월","화","수","목","금","토")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach {
                    Box(
                        modifier = Modifier.width(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = it,
                            fontSize = 16.sp,
                            color = Color(0xFF794C37)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 날짜 그리드
            DayGrid(
                ym = ym,
                today = today,
                selected = sel,
                badges = badges,
                badgeColors = badgeColors,
                onSelect = {
                    sel = it
                    onSelectDate(it)
                }
            )
        }
    }
}

@Composable
private fun DayGrid(
    ym: YearMonth,
    today: LocalDate,
    selected: LocalDate?,
    badges: Set<LocalDate>,
    badgeColors: Map<LocalDate, String>,
    onSelect: (LocalDate) -> Unit
) {
    val first = ym.atDay(1)
    val shift = first.dayOfWeek.value % 7   // Sun=0
    val daysInMonth = ym.lengthOfMonth()

    // 6x7 구성
    val dates = buildList {
        val prevYm = ym.minusMonths(1)
        val prevLen = prevYm.lengthOfMonth()
        repeat(shift) { i -> add(prevYm.atDay(prevLen - shift + 1 + i)) }
        repeat(daysInMonth) { i -> add(ym.atDay(i + 1)) }
        val remain = 42 - size
        val nextYm = ym.plusMonths(1)
        repeat(remain) { i -> add(nextYm.atDay(i + 1)) }
    }

    Column(
        Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        dates.chunked(7).forEach { week ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                week.forEach { date ->
                    val inMonth = date.month == ym.month
                    val isToday = date == today
                    val isSelected = date == selected
                    val hasBadge = badges.contains(date)

                    DayCell(
                        date = date,
                        enabled = inMonth,
                        today = isToday,
                        selected = isSelected,
                        badge = hasBadge,
                        badgeColor = badgeColors[date] ?: "#FFC2F7", // 해당 날짜의 색상 또는 기본값
                        onClick = { if (inMonth) onSelect(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    enabled: Boolean,
    today: Boolean,
    selected: Boolean,
    badge: Boolean,
    badgeColor: String,
    onClick: () -> Unit
) {
    val baseColor = if (enabled) Color(0xFF794C37) else Color(0xFFB8B8B8)

    // ✅ drawWithContent를 꼭 쓰지 않아도 Box 조합으로 동일 효과 가능
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .then(
                if (selected)
                    Modifier
                        .background(Color.White.copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, Color(0x99BEB8B8), CircleShape)
                else Modifier
            )
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        // 오늘 테두리 (선택 안 된 경우만)
        if (today && !selected) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .border(1.dp, Color(0x66BEB8B8), CircleShape)
            )
        }

        // 동적 색상 배지
        if (badge) {
            val color = try {
                Color(android.graphics.Color.parseColor(badgeColor))
            } catch (e: Exception) {
                Color(0xFFFFC2F7) // 기본 핑크색
            }

            Box(
                Modifier
                    .matchParentSize()
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                color,
                                color.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 16.sp,
            color = baseColor,
            fontWeight = if (badge) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/* ----- helpers ----- */

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    clickable(
        indication = null,
        interactionSource = interaction,
        onClick = onClick
    )
}
