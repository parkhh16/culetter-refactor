package com.example.myapplication.ui.components.mail

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.myapplication.R
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable as foundationClickable
import androidx.compose.ui.draw.clip

private val K_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm")

@Composable
fun MailCard(
    summary: MailSummaryUi,
    modifier: Modifier = Modifier,
    onClickWithRect: (MailSummaryUi, Rect) -> Unit
) {
    var iconRect: Rect? = null

    Column(modifier = modifier) {
        // 제목
        Text(
            text = summary.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0x99592813),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(8.dp))

        // 🔵 썸네일은 PNG 사용
        val painter = painterResource(R.drawable.ic_envelope_glass)
        val ar = remember(painter) {
            val s = painter.intrinsicSize
            val w = s.width.takeIf { it.isFinite() && it > 0f } ?: 1f
            val h = s.height.takeIf { it.isFinite() && it > 0f } ?: 1f
            w / h
        }

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,     // 전체가 보이도록
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ar)                 // ← PNG 비율 그대로
                .clip(RoundedCornerShape(12.dp))
                .onGloballyPositioned { iconRect = it.boundsInRoot() }
                .clickableNoIndication { iconRect?.let { r -> onClickWithRect(summary, r) } }
        )

        Spacer(Modifier.height(8.dp))

        // 날짜
        Text(
            text = K_FMT.format(summary.dateTime),
            fontSize = 12.sp,
            color = Color(0x99592813),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))

        // 발신/수신
        Text(
            text = summary.partnerLabel,
            fontSize = 12.sp,
            color = Color(0x99592813),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ripple 없는 클릭 헬퍼 */
@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.then(
        foundationClickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    )
