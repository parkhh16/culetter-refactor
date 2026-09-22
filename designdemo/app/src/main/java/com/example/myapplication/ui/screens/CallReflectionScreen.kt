package com.example.myapplication.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.model.Record
import com.example.myapplication.data.service.MicRecorderSegmenter
import com.example.myapplication.data.service.PcmPlayer
import com.example.myapplication.data.service.TtsSpeechStreamer
import com.example.myapplication.ui.components.FrostedCard
import com.example.myapplication.ui.viewmodel.ReflectionStep
import com.example.myapplication.ui.viewmodel.ReflectionViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

/* ============================= */
/* == Networking (Whisper-1)  == */
/* ============================= */

private val TRANSCRIBE_HTTP by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

// ⚠ 실제 키는 BuildConfig나 안전한 주입 방식을 사용하세요.
private const val OPENAI_API_KEY = "YOUR_API_KEY_HERE"
private const val TTS_VOICE = "alloy"

/* ============================= */
/* ======== Data Model  ======== */
/* ============================= */

data class QaItem(
    val question: String,
    var audioFile: File? = null,
    var transcript: String = ""
)

/* ============================= */
/* ========= Screen  =========== */
/* ============================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallReflectionScreen(
    records: List<Record>,
    onBackClick: () -> Unit,
    hazeState: HazeState,
    viewModel: ReflectionViewModel = viewModel(),
    // 네비가 필요하면 주입, 아니면 이 화면에서 CompletedContent 렌더링
    onNavigateToCompleted: () -> Unit = {},
    onHome: () -> Unit = {}
) {
    val context = LocalContext.current

    // ViewModel state
    val uiState by viewModel.uiState.collectAsState()
    val step = uiState.currentStep
    val reflectionResponse = uiState.reflectionResponse

    // 질문 목록
    val questions: List<String> = remember(uiState.questionResponse) {
        uiState.questionResponse?.questions?.flatMap { it.questions } ?: emptyList()
    }
    val qaList = remember(questions) { questions.map { QaItem(it) }.toMutableStateList() }

    // Audio utils
    val player = remember { PcmPlayer() }
    val tts = remember { TtsSpeechStreamer(apiKey = OPENAI_API_KEY, player = player, voice = TTS_VOICE) }
    val recorder = remember { MicRecorderSegmenter(sampleRate = 16_000) }

    // Permission
    var pendingStart by remember { mutableStateOf<(() -> Unit)?>(null) }
    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingStart?.let { r -> pendingStart = null; if (granted) r() }
    }
    fun ensureMicPermission(then: () -> Unit) {
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (ok) then() else { pendingStart = then; micPermLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    }

    // UI states
    var idx by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("준비 중...") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }

    // Voice level → scale
    var voiceLevelRaw by remember { mutableFloatStateOf(0f) }
    val targetScale =
        if (isRecording) (0.9f + 0.4f * voiceLevelRaw.coerceIn(0f, 1f))
        else if (isSpeaking) 1.05f
        else 1.0f
    val imageScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "vectorScale"
    )

    val scope = rememberCoroutineScope()

    /* ===== Effects ===== */

    // 최초: 질문 생성 트리거
    LaunchedEffect(Unit) {
        if (records.isNotEmpty()) viewModel.startReflection(records)
    }

    // 질문 갱신 시 상태 초기화
    LaunchedEffect(questions.size) {
        idx = 0
        error = null
        started = false
        submitted = false
        isTranscribing = false
        status = if (questions.isEmpty()) "질문을 불러오는 중..." else "첫 질문을 준비 중..."
    }

    // COMPLETED 시 네비게이션 시도(옵션). 렌더는 현재 화면에서 보장.
    var navigatedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(step) {
        Log.d("DEBUG", "Step: $step, navigatedOnce: $navigatedOnce")
        if (step == ReflectionStep.COMPLETED && !navigatedOnce) {
            navigatedOnce = true
            onNavigateToCompleted()
        }
    }

    // 질문 시작 트리거
    LaunchedEffect(qaList.size, step, started) {
        if (step == ReflectionStep.QUESTIONS && qaList.isNotEmpty() && !started) {
            started = true
            playThenRecordFor(
                index = 0,
                qaList = qaList,
                tts = tts,
                recorder = recorder,
                context = context,
                onSpeaking = { isSpeaking = it; status = if (it) "질문 재생 중..." else "녹음 시작..." },
                onRecording = { isRecording = it },
                onNext = { newIndex -> idx = newIndex },
                onError = { msg -> error = msg; status = "오류 발생" },
                ensureMicPermission = ::ensureMicPermission,
                scope = scope,
                onTranscribeStart = {
                    isTranscribing = true
                    status = "전사 중..."
                    isRecording = false
                },
                onTranscribeUpdate = { i, text -> qaList[i] = qaList[i].copy(transcript = text) },
                onTranscribeDone = {
                    isTranscribing = false
                    status = "전사 완료"
                    if (!submitted) {
                        submitted = true
                        submitAllTranscriptsToServer(qaList, viewModel)
                    }
                },
                onLevel = { lvl -> voiceLevelRaw = lvl }
            )
        }
    }

    /* ===== UI ===== */

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .haze(hazeState)
    ) {
        // 배경
        Image(
            painter = painterResource(id = R.drawable.bg_call_reflection),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // ✅ COMPLETED면 즉시 완료 화면 렌더
        if (step == ReflectionStep.COMPLETED) {
            CompletedContent(
                reflectionResponse = reflectionResponse,
                onRestart = {
                    // 필요 시 다시 시작 로직으로 변경하세요.
                    onBackClick()
                },
                onHome = onHome,               // ✅ 홈 이동 전달
                hazeState = hazeState          // ✅ FrostedCard용 hazeState 전달
            )
        } else if (isTranscribing || step == ReflectionStep.SUBMITTING || step == ReflectionStep.SAVING) {
            // 로딩 상태일 때 RecordScreen과 같은 스타일
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // 상단 바
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        runCatching { tts.stop() }
                        runCatching { recorder.stop() }
                        runCatching { player.stopAndRelease() }
                        onBackClick()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color(0x99592813))
                    }
                    Text("회고", color = Color(0x99592813), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(48.dp))
                }
                
                Spacer(Modifier.height(40.dp))
                
                // 로딩 콘텐츠
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingContent(
                        message = when {
                            isTranscribing -> "전사 중입니다…"
                            step == ReflectionStep.SUBMITTING -> "회고를 생성 중입니다…"
                            else -> "회고를 저장 중입니다…"
                        }
                    )
                }
            }
        } else {
            // 진행 중 UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // 상단 바
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        runCatching { tts.stop() }
                        runCatching { recorder.stop() }
                        runCatching { player.stopAndRelease() }
                        onBackClick()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color(0x99592813))
                    }
                    Text("회고", color = Color(0x99592813), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(48.dp))
                }

                Spacer(Modifier.height(16.dp))

                // 질문/상태 카드
                FrostedCard(
                    hazeState = hazeState,
                    corner = 24.dp,
                    contentPadding = PaddingValues(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val headerText = when {
                            isTranscribing -> "전사 중…"
                            step == ReflectionStep.SUBMITTING -> "회고 생성 중…"
                            step == ReflectionStep.SAVING -> "회고 저장 중…"
                            else -> "질문 ${idx + 1}/${qaList.size}"
                        }
                        Text(text = headerText, fontSize = 14.sp, color = Color(0x99592813))

                        val currentQuestion = qaList.getOrNull(idx)?.question ?: ""
                        val bodyText = when {
                            isTranscribing -> "음성 전사 중입니다. 잠시만 기다려주세요…"
                            step == ReflectionStep.SUBMITTING -> "답변을 바탕으로 회고를 생성하고 있습니다…"
                            step == ReflectionStep.SAVING -> "생성된 회고를 저장하고 있습니다…"
                            else -> currentQuestion
                        }
                        Text(
                            text = bodyText,
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0x99592813)
                        )

                        if (!isTranscribing && step !in listOf(
                                ReflectionStep.SUBMITTING, ReflectionStep.SAVING
                            )
                        ) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = error ?: when {
                                    isSpeaking -> "질문을 재생 중…"
                                    isRecording -> "말씀해주세요."
                                    else -> status
                                },
                                fontSize = 14.sp,
                                color = Color(0x99592813)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // 중앙 애니메이션
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.fx_speech_orb),
                        contentDescription = "voice reactive blob",
                        modifier = Modifier
                            .size(400.dp)
                            .scale(imageScale)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 하단 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            runCatching { tts.stop() }
                            runCatching { recorder.stop() }
                            runCatching { player.stopAndRelease() }
                            onBackClick()
                        },
                        modifier = Modifier.width(200.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB5545))
                    ) {
                        Text("종료", color = Color.White)
                    }
                }
            }
        }

    }
}

/* ============================= */
/* ======= UI Components  ====== */
/* ============================= */

@Composable
private fun LoadingContent(
    message: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 로딩 스피너
        CircularProgressIndicator(
            color = Color(0x99592813),
            modifier = Modifier.size(80.dp)
        )
        
        Text(
            text = message,
            color = Color(0x99592813),
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CompletedContent(
    reflectionResponse: com.example.myapplication.data.model.ReflectionResponse?,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    hazeState: HazeState
) {
    // 카드 자체를 화면 가득 채우고, 내부 컨텐츠를 중앙 정렬
    FrostedCard(
        hazeState = hazeState,
        corner = 24.dp,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val contentModifier = Modifier
                .widthIn(max = 680.dp) // 넓이 제한(선택)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())

            if (reflectionResponse == null) {
                Column(
                    modifier = contentModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("회고 결과 없음", color = Color(0x99592813), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "아직 회고를 완료하지 않았습니다.",
                                color = Color(0x99592813).copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    // 버튼: 다시 회고하기, 완료(홈)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onRestart,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x99592813))
                        ) { Text("다시 회고하기", color = Color.White, fontSize = 16.sp) }

                        Button(
                            onClick = onHome,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF99161))
                        ) { Text("완료", color = Color.White, fontSize = 16.sp) }
                    }
                }
            } else {
                Column(
                    modifier = contentModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("회고 완료!", color = Color(0x99592813), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text(
                                "회고가 성공적으로 작성성되었습니다.",
                                color = Color(0x99592813).copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    // 결과 본문
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                            Text("일일 회고", color = Color(0x99592813).copy(alpha = 0.7f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(reflectionResponse.dailyReflection, color = Color(0x99592813), fontSize = 16.sp, lineHeight = 22.sp)
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                            Text("요약", color = Color(0x99592813).copy(alpha = 0.7f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(reflectionResponse.summary, color = Color(0x99592813), fontSize = 16.sp, lineHeight = 22.sp)
                        }
                    }
                    // 버튼: 다시 회고하기, 완료(홈)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onRestart,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x99592813))
                        ) { Text("다시 회고하기", color = Color.White, fontSize = 16.sp) }

                        Button(
                            onClick = onHome,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF99161))
                        ) { Text("완료", color = Color.White, fontSize = 16.sp) }
                    }
                }
            }
        }
    }
}

/* ============================= */
/* ====== Logic Helpers  ======= */
/* ============================= */

/** 한 질문: TTS로 읽고 → 200ms 대기 → VAD 녹음 → 침묵 시 다음 질문 or 일괄 전사 */
private fun playThenRecordFor(
    index: Int,
    qaList: List<QaItem>,
    tts: TtsSpeechStreamer,
    recorder: MicRecorderSegmenter,
    context: Context,
    onSpeaking: (Boolean) -> Unit,
    onRecording: (Boolean) -> Unit,
    onNext: (Int) -> Unit,
    onError: (String) -> Unit,
    ensureMicPermission: (((() -> Unit)) -> Unit),
    scope: CoroutineScope,
    onTranscribeStart: (() -> Unit)? = null,
    onTranscribeUpdate: ((Int, String) -> Unit)? = null,
    onTranscribeDone: (() -> Unit)? = null,
    onLevel: (Float) -> Unit = {}
) {
    val question = qaList.getOrNull(index)?.question ?: return
    onSpeaking(true)
    tts.speak(
        text = question,
        onStart = {
            runCatching { recorder.stop() }
            onRecording(false)
        },
        onDone = {
            onSpeaking(false)
            Handler(Looper.getMainLooper()).postDelayed({
                ensureMicPermission {
                    try {
                        val file = File(context.cacheDir, "qa_${index}.wav")
                        recorder.start(
                            context = context,
                            outFile = file,
                            onStart = { onRecording(true) },
                            onSegmentDone = { f ->
                                onRecording(false)
                                qaList.getOrNull(index)?.audioFile = f
                                val next = index + 1
                                if (next < qaList.size) {
                                    onNext(next)
                                    playThenRecordFor(
                                        next, qaList, tts, recorder, context,
                                        onSpeaking, onRecording, onNext, onError,
                                        ensureMicPermission, scope, onTranscribeStart, onTranscribeUpdate, onTranscribeDone, onLevel
                                    )
                                } else {
                                    onTranscribeStart?.invoke()
                                    startTranscribeAll(
                                        scope = scope,
                                        context = context,
                                        qaList = qaList,
                                        onUpdate = { i, text -> onTranscribeUpdate?.invoke(i, text) },
                                        onDone = { onTranscribeDone?.invoke() },
                                        onError = onError
                                    )
                                }
                            },
                            onError = { msg ->
                                onRecording(false)
                                onError(msg)
                            },
                            onLevel = onLevel
                        )
                    } catch (e: Exception) {
                        onError("마이크 시작 실패: ${e.message}")
                    }
                }
            }, 200)
        },
        onError = { msg ->
            onSpeaking(false)
            onError(msg)
        }
    )
}

/** Whisper 전사 */
private fun startTranscribeAll(
    scope: CoroutineScope,
    context: Context,
    qaList: List<QaItem>,
    onUpdate: (Int, String) -> Unit,
    onDone: () -> Unit,
    onError: (String) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        try {
            for (i in qaList.indices) {
                val f = qaList[i].audioFile ?: continue

                val reqBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-1")
                    .addFormDataPart("language", "ko")
                    .addFormDataPart("file", f.name, f.asRequestBody("audio/wav".toMediaTypeOrNull()))
                    .build()

                val req = Request.Builder()
                    .url("https://api.openai.com/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
                    .addHeader("Accept", "application/json")
                    .post(reqBody)
                    .build()

                var lastErr: String? = null
                repeat(2) { attempt ->
                    try {
                        TRANSCRIBE_HTTP.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) {
                                lastErr = "HTTP ${resp.code} ${resp.message} - ${resp.body?.string().orEmpty()}"
                                throw IOException(lastErr)
                            }
                            val txt = resp.body?.string().orEmpty()
                            val text = try {
                                JSONObject(txt).optString("text", "")
                            } catch (_: Exception) {
                                ""
                            }
                            withContext(Dispatchers.Main) { onUpdate(i, text) }
                            lastErr = null
                            return@repeat
                        }
                    } catch (e: SocketTimeoutException) {
                        lastErr = "timeout(읽기/쓰기 지연)"
                    } catch (e: IOException) {
                        lastErr = e.message ?: "io error"
                    }
                    if (attempt == 0) delay(1200)
                }

                if (lastErr != null) {
                    withContext(Dispatchers.Main) { onError("전사 실패(${i + 1}): $lastErr") }
                    return@launch
                }
            }
            withContext(Dispatchers.Main) { onDone() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError("전사 중 오류: ${e.message ?: "unknown"}") }
        }
    }
}

/** 전사 완료 후 서버(뷰모델)로 답변 모두 제출 → VM이 SUBMITTING/SAVING/COMPLETED로 진행 */
private fun submitAllTranscriptsToServer(
    qaList: List<QaItem>,
    viewModel: ReflectionViewModel
) {
    qaList.map { it.transcript }
        .filter { it.isNotBlank() }
        .forEach { viewModel.answerQuestion(it) }
}
