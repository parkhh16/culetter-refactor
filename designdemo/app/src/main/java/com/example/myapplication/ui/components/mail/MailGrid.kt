// app/src/main/java/com/example/myapplication/ui/components/mail/MailGrid.kt
package com.example.myapplication.ui.components.mail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp

/**
 * 편지 카드 그리드 (기본 2열)
 * - onMailClick: 간단 클릭 콜백
 * - onMailClickWithRect: 위치(Rect)까지 필요한 경우(옵션)
 */
@Composable
fun MailGrid(
    items: List<MailSummaryUi>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    horizontalSpacingDp: Int = 16,
    verticalSpacingDp: Int = 16,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onMailClick: (MailSummaryUi) -> Unit = {},
    onMailClickWithRect: ((MailSummaryUi, Rect) -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacingDp.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacingDp.dp),
        userScrollEnabled = true,
        contentPadding = contentPadding
    ) {
        items(items, key = { it.id }) { mail ->
            // ⬇️ MailCard가 (summary, rect) 형태로 콜백을 주는 경우에 맞춰 연결
            MailCard(summary = mail) { summaryFromCard, rect ->
                // Rect 기반 콜백이 있으면 우선 사용, 없으면 간단 콜백 호출
                onMailClickWithRect?.invoke(summaryFromCard, rect) ?: onMailClick(summaryFromCard)
            }
        }
    }
}
