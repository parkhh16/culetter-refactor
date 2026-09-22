package com.example.myapplication.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.ui.components.FrostedCard
import com.example.myapplication.ui.components.buttons.GlowyCircleButton
import com.example.myapplication.ui.viewmodel.RecordViewModel
import com.example.myapplication.utils.PermissionManager
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * 기록하기 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onBackClick: () -> Unit,
    hazeState: HazeState
) {
    val context = LocalContext.current
    val viewModel: RecordViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    // 권한 상태 관리
    var hasPermissions by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // ViewModel 초기화
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        hasPermissions = PermissionManager.areAllPermissionsGranted(context)
    }
    
    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        hasPermissions = allGranted
        
        if (!allGranted) {
            showPermissionDialog = true
        }
    }
    
    // 권한 요청 함수
    fun requestPermissions() {
        permissionLauncher.launch(PermissionManager.getRequiredPermissions())
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .haze(hazeState)
    ) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.raw.letter_send_modal),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
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
                "기록하기",
                color = Color(0x99592813),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp)) // 균형 맞추기
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 메인 콘텐츠
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                !hasPermissions -> {
                    PermissionRequiredContent(
                        onRequestPermissions = { requestPermissions() }
                    )
                }
                
                uiState.isLoading -> {
                    LoadingContent(
                        processingStep = uiState.processingStep,
                        isRecording = uiState.isRecording
                    )
                }
                
                uiState.isCompleted -> {
                    CompletedContent(
                        transcriptText = uiState.transcriptText,
                        summaryText = uiState.summaryText,
                        onNewRecord = { viewModel.resetState() }
                    )
                }
                
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error ?: "알 수 없는 오류",
                        onRetry = { viewModel.resetState() }
                    )
                }
                
                else -> {
                    RecordingContent(
                        isRecording = uiState.isRecording,
                        onStartRecording = { viewModel.startRecording() },
                        onStopRecording = { viewModel.stopRecording() },
                        onCancelRecording = { viewModel.cancelRecording() }
                    )
                }
            }
        }
        
        // 권한 거부 다이얼로그
        if (showPermissionDialog) {
            PermissionDeniedDialog(
                onDismiss = { showPermissionDialog = false },
                onRequestAgain = { 
                    showPermissionDialog = false
                    requestPermissions() 
                }
            )
        }
        }
    }
}

@Composable
private fun RecordingContent(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // 녹음 버튼
        if (isRecording) {
            RecordingButton(
                onStop = onStopRecording,
                onCancel = onCancelRecording
            )
        } else {
            StartRecordingButton(onStart = onStartRecording)
        }
        
        // 안내 텍스트
        Text(
            text = if (isRecording) "녹음 중... 마이크에 대고 이야기해주세요" else "마이크 버튼을 눌러 녹음을 시작하세요",
            color = Color(0xCC592813),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StartRecordingButton(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.3f))
            .clickable { onStart() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "녹음 시작",
            tint = Color(0x99592813),
            modifier = Modifier.size(80.dp)
        )
    }
}

@Composable
private fun RecordingButton(
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 중지 버튼
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
                .clickable { onStop() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = "녹음 중지",
                tint = Color(0x99592813),
                modifier = Modifier.size(80.dp)
            )
        }
        
        // 취소 버튼
        TextButton(onClick = onCancel) {
            Text(
                "취소",
                color = Color(0xCC592813),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun LoadingContent(
    processingStep: String,
    isRecording: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (isRecording) {
            // 녹음 중 애니메이션
            val infiniteTransition = rememberInfiniteTransition(label = "recording")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = "녹음 중",
                    tint = Color(0x99592813),
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            // 로딩 스피너
            CircularProgressIndicator(
                color = Color(0x99592813),
                modifier = Modifier.size(80.dp)
            )
        }
        
        Text(
            text = processingStep,
            color = Color(0x99592813),
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompletedContent(
    transcriptText: String,
    summaryText: String,
    onNewRecord: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        
        Text(
            text = "기록이 완료되었습니다!",
            color = Color(0x99592813),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        // 결과 표시
        FrostedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            hazeState = null,
            corner = 16.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "요약\n$summaryText",
                    color = Color(0x99592813),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "전문: $transcriptText",
                    color = Color(0xCC592813),
                    fontSize = 14.sp
                )
            }
        }
        
        // 새 기록 버튼
        FrostedCard(
            modifier = Modifier
                .wrapContentWidth()
                .clickable { onNewRecord() },
            hazeState = null,
            corner = 24.dp,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                "새 기록하기",
                color = Color(0x99592813),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermissions: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 마이크 아이콘
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0x99592813).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "마이크",
                tint = Color(0x99592813),
                modifier = Modifier.size(48.dp)
            )
        }
        
        Text(
            text = "녹음 권한이 필요합니다",
            color = Color(0x99592813),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "음성 기록을 위해 마이크 접근 권한이 필요합니다.\n설정에서 권한을 허용해주세요.",
            color = Color(0xCC592813),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onRequestPermissions,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x99592813).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                "권한 허용하기",
                color = Color(0x99592813),
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onRequestAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "권한이 필요합니다",
                color = Color(0xFF333333)
            )
        },
        text = {
            Text(
                text = "녹음 기능을 사용하려면 마이크 권한이 필요합니다.\n설정에서 권한을 허용해주세요.",
                color = Color(0xFF666666)
            )
        },
        confirmButton = {
            TextButton(onClick = onRequestAgain) {
                Text("다시 요청", color = Color(0xFF2196F3))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "오류가 발생했습니다",
            color = Color(0x99592813),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = error,
            color = Color(0xCC592813),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x99592813).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                "다시 시도",
                color = Color(0x99592813),
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
    }
}