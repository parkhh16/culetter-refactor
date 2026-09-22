package com.example.myapplication.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.myapplication.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun MailDetailModal(
    visible: Boolean,
    title: String,
    content: String,
    audioUrl: String? = null,
    onDismiss: () -> Unit
) {
    if (visible) BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val player = remember(audioUrl) {
        if (audioUrl.isNullOrBlank()) null
        else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(audioUrl)))
            prepare()
        }
    }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    val togglePlay: () -> Unit = {
        player?.let {
            if (playing) {
                it.pause()
                playing = false
            } else {
                it.playWhenReady = true
                it.play()
                playing = true
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)) // 어두운 배경 스크림
                    .clickable { onDismiss() }
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { /* 모달 카드 내부 클릭 차단 */ }
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f) // 모달 높이
                        .padding(vertical = 10.dp) // 모달 내부 여백
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    // 배경 이미지
                    Image(
                        painter = painterResource(id = R.raw.mail_detail_modal),
                        contentDescription = "배경 이미지",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        /* ───── 상단 여백 ───── */
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        /* ───── 상단 아이콘 줄 ───── */
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                            ,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (player != null) togglePlay() },
                                enabled = player != null,
                                modifier = Modifier.padding(start = 12.dp)

                            ) {
                                Icon(
                                    imageVector = if (playing) Icons.Outlined.Pause else Icons.Outlined.VolumeUp,
                                    contentDescription = "재생/정지",
                                    tint = Color(0xFFB08D84),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "닫기",
                                    tint = Color(0xFFB08D84),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(30.dp))

                        /* ───── 제목 줄 ───── */
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp) // 3줄 정도 높이
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            val titleScroll = rememberScrollState()
                            Text(
                                text = title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0x99592813),
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(titleScroll)
                            )
                        }

                        /* ───── 본문 카드 ───── */
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 30.dp, vertical = 40.dp) // 카드와 모달 사이 여백
                                .clip(RoundedCornerShape(24.dp))
                                .border( //
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .background(Color.White.copy(alpha = 0.85f))
                                .padding(horizontal = 28.dp, vertical = 40.dp) // 카드 안쪽 여백
                        ) {
                            val scroll = rememberScrollState()
                            Text(
                                text = content,
                                fontSize = 16.sp,
                                color = Color(0x99592813),
                                lineHeight = 22.sp,
                                modifier = Modifier.verticalScroll(scroll)
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

/* ───────── Preview ───────── */

@Preview(showBackground = true)
@Composable
fun MailDetailModalPreview() {
    MailDetailModal(
        visible = true,
        title = "보통을 채우는 커피",
        content = List(20) { "블라블라블라...." }.joinToString("\n"),
        audioUrl = null,
        onDismiss = {}
    )
}
