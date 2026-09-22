package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.model.RecordData
import com.example.myapplication.data.model.RecordRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 기록 데이터 리포지토리
 */
class RecordRepository {
    
    private val apiService = ApiClient.storyApiService
    
    /**
     * 새로운 기록 생성
     * @param token Firebase 토큰
     * @param transcriptText 음성 인식 텍스트
     * @param summaryText 요약 텍스트
     * @return 생성된 기록 데이터
     */
    suspend fun createRecord(
        token: String,
        transcriptText: String,
        summaryText: String
    ): Result<RecordData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RecordRequest(
                    transcriptText = transcriptText,
                    summaryText = summaryText
                )
                
                val response = apiService.createRecord("Bearer $token", request)
                
                if (response.isSuccessful) {
                    val recordResponse = response.body()
                    if (recordResponse != null) {
                        Result.success(recordResponse.result)
                    } else {
                        Result.failure(Exception("응답 데이터가 null입니다."))
                    }
                } else {
                    Result.failure(Exception("기록 생성 실패: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
