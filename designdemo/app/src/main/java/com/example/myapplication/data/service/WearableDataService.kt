package com.example.myapplication.data.service

import android.content.Context
import android.util.Log
import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.api.GptApiServiceImpl
import com.example.myapplication.data.api.WhisperApiService
import com.example.myapplication.data.repository.RecordRepository
import com.example.myapplication.utils.FirebaseTokenManager
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class WearableDataService(private val context: Context) : DataClient.OnDataChangedListener, MessageClient.OnMessageReceivedListener {
    
    companion object {
        private const val TAG = "WearableDataService"
        private const val VOICE_MESSAGE_PATH = "/voice"
        private const val PING_MESSAGE_PATH = "/ping"
    }
    
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val channelClient = Wearable.getChannelClient(context)
    private val recordRepository = RecordRepository()
    private val whisperApiService = WhisperApiService()
    private val gptApiService = GptApiServiceImpl(ApiClient.gptApiService)
    private val firebaseTokenManager = FirebaseTokenManager()
    
    init {
        // 리스너 등록
        dataClient.addListener(this)
        messageClient.addListener(this)
    }
    
    fun cleanup() {
        dataClient.removeListener(this)
        messageClient.removeListener(this)
    }
    
    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: ${dataEventBuffer.count} events")
        
        for (event in dataEventBuffer) {
            when (event.type) {
                DataEvent.TYPE_CHANGED -> {
                    val dataItem = event.dataItem
                    Log.d(TAG, "Data changed: ${dataItem.uri}")
                    // 필요시 데이터 처리 로직 추가
                }
                DataEvent.TYPE_DELETED -> {
                    Log.d(TAG, "Data deleted: ${event.dataItem.uri}")
                }
            }
        }
    }
    
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived: path=${messageEvent.path}, from=${messageEvent.sourceNodeId}")
        
        when (messageEvent.path) {
            VOICE_MESSAGE_PATH -> {
                Log.d(TAG, "Received voice data: ${messageEvent.data.size} bytes")
                handleVoiceData(messageEvent.data)
            }
            PING_MESSAGE_PATH -> {
                Log.d(TAG, "Received ping from wear device")
                // 핑 응답 처리
            }
        }
    }
    
    private fun handleVoiceData(data: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 파일로 저장
                val fileName = "wear_voice_${System.currentTimeMillis()}.m4a"
                val file = File(context.filesDir, fileName)
                
                FileOutputStream(file).use { fos ->
                    fos.write(data)
                }
                
                Log.d(TAG, "Voice data saved to: ${file.absolutePath}")
                
                // 웨어앱에서 받은 음성 데이터 처리
                handleWearVoiceData(file)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling voice data", e)
            }
        }
    }
    
    // Channel을 통한 대용량 파일 수신
    fun handleChannelData(nodeId: String, channelId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Handling channel data from node: $nodeId, channel: $channelId")
                
                // Channel API는 실제로는 다른 방식으로 사용됩니다
                // 여기서는 간단히 로그만 남기고, 실제 구현은 웨어앱에서 Message API를 사용하도록 합니다
                Log.d(TAG, "Channel data received from node: $nodeId, channel: $channelId")
                
                // TODO: 실제 Channel API 구현이 필요한 경우 여기에 추가
                // 현재는 웨어앱에서 Message API를 사용하므로 이 메서드는 사용되지 않습니다
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling channel data", e)
            }
        }
    }
    
    // 웨어앱에서 받은 음성 데이터 처리 (핸드폰 앱과 동일한 로직)
    private fun handleWearVoiceData(voiceFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Processing wear voice data: ${voiceFile.absolutePath}")
                Log.d(TAG, "웨어앱에서 녹음된 음성 파일을 받았습니다: ${voiceFile.name}")
                
                // 핸드폰 앱과 동일한 처리 로직 적용
                processWearRecording(voiceFile)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing wear voice data", e)
            }
        }
    }
    
    /**
     * 웨어앱 녹음 파일 처리 (핸드폰 앱과 동일한 로직)
     * 1. Whisper API로 음성 인식
     * 2. GPT API로 요약 생성
     * 3. 백엔드에 저장
     */
    private suspend fun processWearRecording(audioFile: File) {
        try {
            Log.d(TAG, "Starting wear recording processing...")
            
            // 1. Whisper API로 음성 인식
            Log.d(TAG, "Step 1: 음성 인식 중...")
            whisperApiService.transcribeAudio(audioFile)
                .onSuccess { transcriptText ->
                    Log.d(TAG, "음성 인식 완료: $transcriptText")
                    
                    // 2. GPT API로 요약 생성
                    Log.d(TAG, "Step 2: 요약 생성 중...")
                    gptApiService.summarizeText(transcriptText)
                        .onSuccess { summaryText ->
                            Log.d(TAG, "요약 생성 완료: $summaryText")
                            
                            // 3. 백엔드에 저장
                            Log.d(TAG, "Step 3: 저장 중...")
                            saveWearRecord(transcriptText, summaryText)
                        }
                        .onFailure { exception ->
                            Log.w(TAG, "GPT API 실패, 대체 요약 생성: ${exception.message}")
                            
                            // GPT API 실패 시 대체 요약 생성
                            val fallbackSummary = createFallbackSummary(transcriptText)
                            Log.d(TAG, "대체 요약 생성 완료: $fallbackSummary")
                            
                            // 백엔드에 저장 (대체 요약으로)
                            saveWearRecord(transcriptText, fallbackSummary)
                        }
                }
                .onFailure { exception ->
                    Log.e(TAG, "음성 인식 실패: ${exception.message}")
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "웨어앱 녹음 처리 중 오류", e)
        }
    }
    
    /**
     * 웨어앱 녹음 기록 저장
     */
    private suspend fun saveWearRecord(transcriptText: String, summaryText: String) {
        try {
            val token = firebaseTokenManager.getCurrentUserToken()
            if (token == null) {
                Log.e(TAG, "Firebase 토큰을 가져올 수 없습니다.")
                return
            }
            
            recordRepository.createRecord(token, transcriptText, summaryText)
                .onSuccess { recordData ->
                    Log.d(TAG, "웨어앱 녹음 기록 저장 완료: ${recordData.id}")
                    // TODO: 필요시 UI 알림 또는 다른 후처리
                }
                .onFailure { exception ->
                    Log.e(TAG, "웨어앱 녹음 기록 저장 실패: ${exception.message}")
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "웨어앱 녹음 저장 중 오류", e)
        }
    }
    
    /**
     * GPT API 실패 시 대체 요약 생성 (RecordViewModel과 동일)
     */
    private fun createFallbackSummary(transcriptText: String): String {
        return when {
            transcriptText.length <= 20 -> transcriptText
            transcriptText.length <= 50 -> transcriptText.take(20) + "..."
            else -> transcriptText.take(30) + "..."
        }
    }
    
    // 연결된 웨어 디바이스 확인
    suspend fun getConnectedWearDevices(): List<Node> {
        return try {
            Wearable.getNodeClient(context).connectedNodes.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected devices", e)
            emptyList()
        }
    }
}
