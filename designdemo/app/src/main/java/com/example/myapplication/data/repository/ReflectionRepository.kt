package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * 회고 데이터 리포지토리
 */
class ReflectionRepository {
    
    private val reflectionApiService = ApiClient.reflectionApiService
    private val storyApiService = ApiClient.storyApiService
    
    /**
     * 질문 생성
     */
    suspend fun generateQuestions(
        token: String,
        records: List<RecordForQuestion>
    ): Result<QuestionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = QuestionRequest(records = records)
                val response = reflectionApiService.generateQuestions("Bearer $token", request)
                
                if (response.isSuccessful) {
                    val questionResponse = response.body()
                    if (questionResponse != null) {
                        Result.success(questionResponse)
                    } else {
                        Result.failure(Exception("응답 데이터가 null입니다."))
                    }
                } else {
                    Result.failure(Exception("질문 생성 실패: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 회고 답변 전송
     */
    suspend fun submitReflection(
        token: String,
        originalRecords: List<RecordForQuestion>,
        questionAnswers: List<QuestionAnswer>
    ): Result<ReflectionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = dateFormat.format(Date())
                
                val request = ReflectionRequest(
                    originalRecords = originalRecords,
                    questionAnswers = questionAnswers,
                    reflectionDate = currentDate
                )
                
                val response = reflectionApiService.submitReflection("Bearer $token", request)
                
                if (response.isSuccessful) {
                    val reflectionResponse = response.body()
                    if (reflectionResponse != null) {
                        Result.success(reflectionResponse)
                    } else {
                        Result.failure(Exception("응답 데이터가 null입니다."))
                    }
                } else {
                    Result.failure(Exception("회고 전송 실패: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 회고를 앱 DB에 저장
     */
    suspend fun saveRetrospect(
        token: String,
        title: String,
        content: String
    ): Result<RetrospectData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RetrospectRequest(
                    title = title,
                    content = content
                )
                
                val response = storyApiService.saveRetrospect("Bearer $token", request)
                
                if (response.isSuccessful) {
                    val retrospectResponse = response.body()
                    if (retrospectResponse != null && retrospectResponse.result != null) {
                        Result.success(retrospectResponse.result)
                    } else {
                        Result.failure(Exception("응답 데이터가 null입니다."))
                    }
                } else {
                    Result.failure(Exception("회고 저장 실패: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
