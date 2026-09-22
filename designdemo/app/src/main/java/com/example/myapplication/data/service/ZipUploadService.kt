package com.example.myapplication.data.service

import android.content.Context
import android.util.Log
import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.model.RecordDetailData
import com.example.myapplication.utils.FirebaseTokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ZIP 파일 생성 및 업로드 서비스
 */
class ZipUploadService(private val context: Context) {
    
    companion object {
        private const val TAG = "ZipUploadService"
        private const val TEMP_DIR = "temp_zip"
    }
    
    private val audioRecorderService = AudioRecorderService(context)
    private val tokenManager = FirebaseTokenManager()
    
    /**
     * 선물하기를 위한 ZIP 파일 생성 및 업로드
     * @param userId 사용자 ID
     * @param letterId 편지 ID
     * @param recordIds 녹음 ID 목록
     * @param letterTitle 편지 제목
     * @param letterContent 편지 내용
     * @return 업로드 성공 여부
     */
    suspend fun uploadGiftBundle(
        userId: String,
        letterId: String,
        recordIds: List<Int>,
        letterTitle: String,
        letterContent: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== ZIP 파일 생성 시작 ===")
            Log.d(TAG, "userId: $userId, letterId: $letterId, recordIds: $recordIds")
            Log.d(TAG, "letterTitle: $letterTitle, letterContent: $letterContent")
            
            // 1. 임시 디렉토리 생성
            Log.d(TAG, "1단계: 임시 디렉토리 생성")
            val tempDir = File(context.filesDir, TEMP_DIR)
            if (!tempDir.exists()) {
                tempDir.mkdirs()
                Log.d(TAG, "임시 디렉토리 생성됨: ${tempDir.absolutePath}")
            } else {
                Log.d(TAG, "임시 디렉토리 이미 존재: ${tempDir.absolutePath}")
            }
            
            // 2. ZIP 파일 생성
            Log.d(TAG, "2단계: ZIP 파일 생성")
            val zipFile = File(tempDir, "gift_bundle_${System.currentTimeMillis()}.zip")
            Log.d(TAG, "ZIP 파일 경로: ${zipFile.absolutePath}")
            createZipFile(zipFile, recordIds, letterTitle, letterContent)
            
            // 3. ZIP 파일 업로드
            Log.d(TAG, "3단계: ZIP 파일 업로드")
            val uploadResult = uploadZipFile(zipFile, userId, letterId)
            
            // 4. 임시 파일 정리
            Log.d(TAG, "4단계: 임시 파일 정리")
            cleanupTempFiles(tempDir)
            
            Log.d(TAG, "=== ZIP 파일 생성 및 업로드 완료 ===")
            uploadResult
            
        } catch (e: Exception) {
            Log.e(TAG, "ZIP 파일 생성 및 업로드 실패", e)
            Result.failure(e)
        }
    }
    
    /**
     * ZIP 파일 생성
     */
    private suspend fun createZipFile(
        zipFile: File,
        recordIds: List<Int>,
        letterTitle: String,
        letterContent: String
    ) {
        try {
            Log.d(TAG, "ZIP 파일 생성 시작: ${zipFile.absolutePath}")
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                
                // 1. 녹음 파일들 추가
                Log.d(TAG, "녹음 파일 추가 시작, 개수: ${recordIds.size}")
                for (recordId in recordIds) {
                    Log.d(TAG, "녹음 파일 추가 중: recordId=$recordId")
                    addRecordingFile(zipOut, recordId)
                    addTranscriptFile(zipOut, recordId)
                }
                
                // 2. 편지 파일 추가
                Log.d(TAG, "편지 파일 추가 시작")
                addLetterFile(zipOut, letterTitle, letterContent)
                
            }
            Log.d(TAG, "ZIP 파일 생성 완료: ${zipFile.absolutePath}")
            
        } catch (e: IOException) {
            Log.e(TAG, "ZIP 파일 생성 실패", e)
            throw e
        }
    }
    
    /**
     * 녹음 파일 추가
     */
    private fun addRecordingFile(zipOut: ZipOutputStream, recordId: Int) {
        try {
            val audioFile = audioRecorderService.getRecordingFile(recordId)
            if (audioFile != null && audioFile.exists()) {
                // 파일명을 rec_{record_id}.wav로 변경하여 ZIP에 추가
                val zipEntry = ZipEntry("rec_${recordId}.wav")
                zipOut.putNextEntry(zipEntry)
                
                audioFile.inputStream().use { inputStream ->
                    inputStream.copyTo(zipOut)
                }
                
                zipOut.closeEntry()
                Log.d(TAG, "녹음 파일 추가 완료: rec_${recordId}.wav")
            } else {
                Log.w(TAG, "녹음 파일을 찾을 수 없습니다: record_$recordId.m4a")
            }
        } catch (e: Exception) {
            Log.e(TAG, "녹음 파일 추가 실패: recordId=$recordId", e)
        }
    }
    
    /**
     * 전사 텍스트 파일 추가
     */
    private suspend fun addTranscriptFile(zipOut: ZipOutputStream, recordId: Int) {
        try {
            val token = tokenManager.getCurrentUserToken()
            if (token == null) {
                Log.w(TAG, "토큰이 없어 전사 텍스트를 가져올 수 없습니다: recordId=$recordId")
                return
            }
            
            // API 호출로 레코드 상세 정보 가져오기
            val response = ApiClient.storyApiService.getRecordDetail(
                token = "Bearer $token",
                recordId = recordId
            )
            
            if (response.isSuccessful && response.body()?.code == 1000) {
                val recordDetail = response.body()!!.result
                val transcriptText = recordDetail.transcriptText
                
                // 텍스트 파일로 ZIP에 추가
                val zipEntry = ZipEntry("rec_${recordId}.txt")
                zipOut.putNextEntry(zipEntry)
                
                transcriptText.toByteArray().let { bytes ->
                    zipOut.write(bytes)
                }
                
                zipOut.closeEntry()
                Log.d(TAG, "전사 텍스트 파일 추가 완료: rec_${recordId}.txt")
            } else {
                Log.w(TAG, "레코드 상세 정보를 가져올 수 없습니다: recordId=$recordId")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "전사 텍스트 파일 추가 실패: recordId=$recordId", e)
        }
    }
    
    /**
     * 편지 파일 추가
     */
    private fun addLetterFile(zipOut: ZipOutputStream, title: String, content: String) {
        try {
            val letterJson = """
                {
                    "title": "$title",
                    "content": "$content"
                }
            """.trimIndent()
            
            val zipEntry = ZipEntry("letter.json")
            zipOut.putNextEntry(zipEntry)
            
            letterJson.toByteArray().let { bytes ->
                zipOut.write(bytes)
            }
            
            zipOut.closeEntry()
            Log.d(TAG, "편지 파일 추가 완료: letter.json")
            
        } catch (e: Exception) {
            Log.e(TAG, "편지 파일 추가 실패", e)
        }
    }
    
    /**
     * ZIP 파일 업로드
     */
    private suspend fun uploadZipFile(
        zipFile: File,
        userId: String,
        letterId: String
    ): Result<Boolean> {
        try {
            Log.d(TAG, "ZIP 파일 업로드 시작")
            Log.d(TAG, "zipFile: ${zipFile.absolutePath}, size: ${zipFile.length()} bytes")
            Log.d(TAG, "userId: $userId, letterId: $letterId")
            
            val token = tokenManager.getCurrentUserToken()
            if (token == null) {
                Log.e(TAG, "토큰이 없습니다.")
                return Result.failure(Exception("토큰이 없습니다."))
            }
            
            Log.d(TAG, "토큰 확인 완료")
            val requestFile = zipFile.asRequestBody("application/zip".toMediaType())
            val bundle = MultipartBody.Part.createFormData("bundle", zipFile.name, requestFile)
            
            Log.d(TAG, "API 호출 시작")
            val response = ApiClient.voiceApiService.uploadZip(
                authorization = "Bearer $token",
                userId = userId,
                letterId = letterId,
                bundle = bundle
            )
            
            Log.d(TAG, "API 응답: ${response.code()}, 성공: ${response.isSuccessful}")
            if (response.isSuccessful) {
                Log.d(TAG, "ZIP 파일 업로드 성공")
                return Result.success(true)
            } else {
                Log.e(TAG, "ZIP 파일 업로드 실패: ${response.message()}")
                return Result.failure(Exception("업로드 실패: ${response.message()}"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "ZIP 파일 업로드 중 오류 발생", e)
            return Result.failure(e)
        }
    }
    
    /**
     * 임시 파일 정리
     */
    private fun cleanupTempFiles(tempDir: File) {
        try {
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "임시 파일 삭제 완료: ${file.name}")
                    }
                }
                if (tempDir.delete()) {
                    Log.d(TAG, "임시 디렉토리 삭제 완료")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "임시 파일 정리 실패", e)
        }
    }
}
