package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.navigation.NavItem
import com.example.myapplication.utils.noRippleClickable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.runtime.remember

/**
 * 하단 네비게이션 바 (아이콘 + 라벨).
 * - 반투명 화이트 바탕
 * - 선택된 항목은 불투명, 비선택은 반투명
 */
@Composable
fun BottomNav(
    currentRoute: String,
    onSelect: (NavItem) -> Unit,
    hazeState: HazeState? = null,
    barAlpha: Float = 0.6f // ✅ 배경 투명도 조절용 (0.0 ~ 1.0)
) {
    val items = listOf(
        NavItem.Home to R.drawable.ic_home_outline,
        NavItem.Calendar to R.drawable.ic_calendar_outline,
        NavItem.Archive to R.drawable.ic_box_outline,
        NavItem.Mail to R.drawable.ic_envelope_outline,
        NavItem.Profile to R.drawable.ic_person_outline
    )

    val base = Color(0xFFC7788A)
    val selectedTint = base
    // 배경이 더 투명해지면 비선택도 조금 더 진하게
    val unselectedTint = base.copy(alpha = if (barAlpha < 0.8f) 0.3f else 0.6f)

    // 위아래 모두 둥근 모서리를 위한 커스텀 shape
    val roundedShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(roundedShape)
            .haze(state = hazeState ?: remember { HazeState() })
            .background(Color.White.copy(alpha = 0.4f))
            .padding(horizontal = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (item, iconRes) ->
                val selected = currentRoute == item.route
                val tint = if (selected) selectedTint else unselectedTint
                val label = when (item) {
                    NavItem.Home     -> "홈"
                    NavItem.Calendar -> "달력"
                    NavItem.Archive  -> "서랍"
                    NavItem.Mail     -> "선물함"
                    NavItem.Profile  -> "내 정보"
                    else             -> ""
                }

                Column(
                    modifier = Modifier
                        .width(56.dp)
                        .wrapContentHeight()
                        .noRippleClickable { onSelect(item) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = item.route,
                        tint = tint,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = tint
                    )
                }
            }
        }
    }
}

