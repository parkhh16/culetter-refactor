//package com.example.myapplication.ui.screens
//
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.icons.filled.Mic
//import androidx.compose.material.icons.filled.Stop
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.myapplication.data.model.Record
//import com.example.myapplication.data.service.WhisperWebSocketService
//import com.example.myapplication.ui.viewmodel.ReflectionViewModel
//import com.example.myapplication.ui.viewmodel.ReflectionStep
//import dev.chrisbanes.haze.HazeState
//import dev.chrisbanes.haze.haze
//
///**
// * 회고 화면
// */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ReflectionScreen(
//    records: List<Record>,
//    onBackClick: () -> Unit,
//    hazeState: HazeState,
//    viewModel: ReflectionViewModel = viewModel()
//) {
//    val uiState by viewModel.uiState.collectAsState()
//    val currentQuestion by viewModel.currentQuestion.collectAsState()
//    val questionAnswers by viewModel.questionAnswers.collectAsState()
//
//    val context = LocalContext.current
//    val whisperService = remember { WhisperWebSocketService() }
//    val whisperConnectionState by whisperService.connectionState.collectAsState()
//    val transcriptionResult by whisperService.transcriptionResult.collectAsState()
//    val whisperError by whisperService.error.collectAsState()
//
//    var isRecording by remember { mutableStateOf(false) }
//    var answerText by remember { mutableStateOf("") }
//
//    // 회고 시작
//    LaunchedEffect(Unit) {
//        if (records.isNotEmpty()) {
//            viewModel.startReflection(records)
//        }
//    }
//
//    // Whisper 결과 처리
//    LaunchedEffect(transcriptionResult) {
//        transcriptionResult?.let { result ->
//            answerText = result
//            whisperService.clearResult()
//        }
//    }
//
//    // 컴포넌트 정리
//    DisposableEffect(Unit) {
//        onDispose {
//            whisperService.disconnect()
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(horizontal = 16.dp, vertical = 20.dp)
//            .haze(hazeState)
//    ) {
//        // 헤더
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(onClick = onBackClick) {
//                Icon(
//                    Icons.Filled.ArrowBack,
//                    contentDescription = "뒤로가기",
//                    tint = Color.White
//                )
//            }
//
//            Text(
//                "회고하기",
//                color = Color.White,
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold
//            )
//
//            Spacer(modifier = Modifier.width(48.dp))
//        }
//
//        Spacer(modifier = Modifier.height(40.dp))
//
//        // 메인 콘텐츠
//        when (uiState.currentStep) {
//            ReflectionStep.IDLE -> {
//                LoadingContent("회고를 준비하고 있습니다...")
//            }
//
//            ReflectionStep.QUESTIONS -> {
//                QuestionContent(
//                    currentQuestion = currentQuestion,
//                    answerText = answerText,
//                    onAnswerTextChange = { answerText = it },
//                    isRecording = isRecording,
//                    onStartRecording = {
//                        isRecording = true
//                        whisperService.connect("your-api-key") // 실제 API 키로 교체 필요
//                    },
//                    onStopRecording = {
//                        isRecording = false
//                        whisperService.disconnect()
//                    },
//                    onAnswerSubmit = {
//                        if (answerText.isNotEmpty()) {
//                            viewModel.answerQuestion(answerText)
//                            answerText = ""
//                        }
//                    },
//                    questionNumber = questionAnswers.size + 1,
//                    totalQuestions = uiState.questionResponse?.questions?.sumOf { it.questions.size } ?: 0
//                )
//            }
//
//            ReflectionStep.SUBMITTING -> {
//                LoadingContent("회고를 분석하고 있습니다...")
//            }
//
//            ReflectionStep.SAVING -> {
//                LoadingContent("회고를 저장하고 있습니다...")
//            }
//
//            ReflectionStep.COMPLETED -> {
//                CompletedContent(
//                    reflectionResponse = uiState.reflectionResponse,
//                    onRestart = {
//                        viewModel.restartReflection()
//                        if (records.isNotEmpty()) {
//                            viewModel.startReflection(records)
//                        }
//                    }
//                )
//            }
//        }
//
//        // 오류 표시
//        uiState.error?.let { error ->
//            Spacer(modifier = Modifier.height(16.dp))
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                colors = CardDefaults.cardColors(
//                    containerColor = Color.Red.copy(alpha = 0.2f)
//                ),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Text(
//                    text = error,
//                    color = Color.White,
//                    modifier = Modifier.padding(16.dp),
//                    fontSize = 14.sp
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun LoadingContent(message: String) {
//    Box(
//        modifier = Modifier.fillMaxSize(),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            CircularProgressIndicator(
//                color = Color.White,
//                modifier = Modifier.size(48.dp)
//            )
//            Text(
//                text = message,
//                color = Color.White,
//                fontSize = 16.sp,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}
//
//@Composable
//private fun QuestionContent(
//    currentQuestion: String?,
//    answerText: String,
//    onAnswerTextChange: (String) -> Unit,
//    isRecording: Boolean,
//    onStartRecording: () -> Unit,
//    onStopRecording: () -> Unit,
//    onAnswerSubmit: () -> Unit,
//    questionNumber: Int,
//    totalQuestions: Int
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .verticalScroll(rememberScrollState()),
//        verticalArrangement = Arrangement.spacedBy(24.dp)
//    ) {
//        // 진행 상황
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors(
//                containerColor = Color.White.copy(alpha = 0.1f)
//            ),
//            shape = RoundedCornerShape(16.dp)
//        ) {
//            Column(
//                modifier = Modifier.padding(20.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Text(
//                    text = "질문 $questionNumber / $totalQuestions",
//                    color = Color.White.copy(alpha = 0.8f),
//                    fontSize = 14.sp
//                )
//                LinearProgressIndicator(
//                    progress = questionNumber.toFloat() / totalQuestions.toFloat(),
//                    color = Color.White,
//                    modifier = Modifier.fillMaxWidth()
//                )
//            }
//        }
//
//        // 질문
//        currentQuestion?.let { question ->
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                colors = CardDefaults.cardColors(
//                    containerColor = Color.White.copy(alpha = 0.1f)
//                ),
//                shape = RoundedCornerShape(16.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(20.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    Text(
//                        text = "질문",
//                        color = Color.White.copy(alpha = 0.8f),
//                        fontSize = 14.sp
//                    )
//                    Text(
//                        text = question,
//                        color = Color.White,
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Medium,
//                        lineHeight = 24.sp
//                    )
//                }
//            }
//        }
//
//        // 답변 입력
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors(
//                containerColor = Color.White.copy(alpha = 0.1f)
//            ),
//            shape = RoundedCornerShape(16.dp)
//        ) {
//            Column(
//                modifier = Modifier.padding(20.dp),
//                verticalArrangement = Arrangement.spacedBy(16.dp)
//            ) {
//                Text(
//                    text = "답변",
//                    color = Color.White.copy(alpha = 0.8f),
//                    fontSize = 14.sp
//                )
//
//                OutlinedTextField(
//                    value = answerText,
//                    onValueChange = onAnswerTextChange,
//                    placeholder = {
//                        Text(
//                            text = "답변을 입력하세요...",
//                            color = Color.White.copy(alpha = 0.6f)
//                        )
//                    },
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedTextColor = Color.White,
//                        unfocusedTextColor = Color.White,
//                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
//                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
//                    ),
//                    modifier = Modifier.fillMaxWidth(),
//                    minLines = 3
//                )
//
//                // 녹음 버튼과 제출 버튼
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    // 녹음 버튼
//                    Button(
//                        onClick = if (isRecording) onStopRecording else onStartRecording,
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = if (isRecording) Color.Red else Color.White.copy(alpha = 0.2f)
//                        ),
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Icon(
//                            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
//                            contentDescription = if (isRecording) "녹음 중지" else "녹음 시작",
//                            tint = Color.White
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text(
//                            text = if (isRecording) "녹음 중지" else "녹음",
//                            color = Color.White
//                        )
//                    }
//
//                    // 제출 버튼
//                    Button(
//                        onClick = onAnswerSubmit,
//                        enabled = answerText.isNotEmpty(),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = Color.White.copy(alpha = 0.2f)
//                        ),
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Icon(
//                            Icons.Filled.Check,
//                            contentDescription = "답변 제출",
//                            tint = Color.White
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text(
//                            text = "제출",
//                            color = Color.White
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
////@Composable
////fun CompletedContent(
////    reflectionResponse: com.example.myapplication.data.model.ReflectionResponse?,
////    onRestart: () -> Unit
////) {
////    Column(
////        modifier = Modifier
////            .fillMaxSize()
////            .verticalScroll(rememberScrollState()),
////        verticalArrangement = Arrangement.spacedBy(24.dp)
////    ) {
////        // 완료 메시지
////        Card(
////            modifier = Modifier.fillMaxWidth(),
////            colors = CardDefaults.cardColors(
////                containerColor = Color.White.copy(alpha = 0.1f)
////            ),
////            shape = RoundedCornerShape(16.dp)
////        ) {
////            Column(
////                modifier = Modifier.padding(20.dp),
////                horizontalAlignment = Alignment.CenterHorizontally,
////                verticalArrangement = Arrangement.spacedBy(16.dp)
////            ) {
////                Text(
////                    text = "회고 완료!",
////                    color = Color.White,
////                    fontSize = 24.sp,
////                    fontWeight = FontWeight.Bold
////                )
////                Text(
////                    text = "오늘의 회고가 성공적으로 완료되었습니다.",
////                    color = Color.White.copy(alpha = 0.8f),
////                    fontSize = 16.sp,
////                    textAlign = TextAlign.Center
////                )
////            }
////        }
////
////        // 일일 회고
////        reflectionResponse?.let { response ->
////            Card(
////                modifier = Modifier.fillMaxWidth(),
////                colors = CardDefaults.cardColors(
////                    containerColor = Color.White.copy(alpha = 0.1f)
////                ),
////                shape = RoundedCornerShape(16.dp)
////            ) {
////                Column(
////                    modifier = Modifier.padding(20.dp),
////                    verticalArrangement = Arrangement.spacedBy(12.dp)
////                ) {
////                    Text(
////                        text = "일일 회고",
////                        color = Color.White.copy(alpha = 0.8f),
////                        fontSize = 14.sp
////                    )
////                    Text(
////                        text = response.dailyReflection,
////                        color = Color.White,
////                        fontSize = 16.sp,
////                        lineHeight = 22.sp
////                    )
////                }
////            }
////
////            // 요약
////            Card(
////                modifier = Modifier.fillMaxWidth(),
////                colors = CardDefaults.cardColors(
////                    containerColor = Color.White.copy(alpha = 0.1f)
////                ),
////                shape = RoundedCornerShape(16.dp)
////            ) {
////                Column(
////                    modifier = Modifier.padding(20.dp),
////                    verticalArrangement = Arrangement.spacedBy(12.dp)
////                ) {
////                    Text(
////                        text = "요약",
////                        color = Color.White.copy(alpha = 0.8f),
////                        fontSize = 14.sp
////                    )
////                    Text(
////                        text = response.summary,
////                        color = Color.White,
////                        fontSize = 16.sp,
////                        lineHeight = 22.sp
////                    )
////                }
////            }
////        }
////
////        // 다시 시작 버튼
////        Button(
////            onClick = onRestart,
////            colors = ButtonDefaults.buttonColors(
////                containerColor = Color.White.copy(alpha = 0.2f)
////            ),
////            modifier = Modifier.fillMaxWidth()
////        ) {
////            Text(
////                text = "다시 회고하기",
////                color = Color.White,
////                fontSize = 16.sp
////            )
////        }
////    }
////}
