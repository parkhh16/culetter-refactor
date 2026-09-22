package com.example.myapplication.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoBackground(
    videoUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // ExoPlayer 인스턴스 생성
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL // 무한 반복
            playWhenReady = true
            volume = 0f // 오디오 비활성화
            prepare()
        }
    }
    
    // 컴포넌트가 해제될 때 ExoPlayer 해제
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player = exoPlayer
                useController = false // 컨트롤러 숨김
                setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER) // 버퍼링 표시 안함
                
                // 비디오 스케일링 설정 - 핸드폰 세로 화면에 맞게 조정
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM // 화면을 채우도록 확대
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
