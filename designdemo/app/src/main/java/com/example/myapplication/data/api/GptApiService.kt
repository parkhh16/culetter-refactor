package com.example.myapplication.data.api

import retrofit2.Response
import retrofit2.http.*

/**
 * GPT API 연동을 위한 서비스 인터페이스
 * GMS API를 통해 텍스트를 한줄 요약으로 변환
 */
interface GptApiService {
    
    @POST("chat/completions")
    suspend fun createCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: CompletionRequest
    ): Response<CompletionResponse>
}

/**
 * GPT API 요청 데이터 클래스
 */
data class CompletionRequest(
    val model: String = "gpt-5-nano",
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

/**
 * GPT API 응답 데이터 클래스
 */
data class CompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

/**
 * GPT API 서비스 구현체
 */
class GptApiServiceImpl(
    private val apiService: GptApiService
) {
    companion object {
        private const val API_KEY = "S13P22A209-75339ea7-5d72-4869-8370-0a00b7853e3f"
    }
    
    /**
     * 텍스트를 한줄 요약으로 변환
     * @param text 요약할 텍스트
     * @return 요약된 텍스트
     */
    suspend fun summarizeText(text: String): Result<String> {
        val maxRetries = 3
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val request = CompletionRequest(
                    model = "gpt-5-nano",
                    messages = listOf(
                        Message(
                            role = "developer",
                            content = "Answer in Korean"
                        ),
                        Message(
                            role = "user",
                            content = "다음 텍스트를 한 줄로 간단하게 요약해주세요:\n\n$text"
                        )
                    )
                )
                
                val response = apiService.createCompletion(
                    authorization = "Bearer $API_KEY",
                    request = request
                )
                
                if (response.isSuccessful) {
                    val completionResponse = response.body()
                    if (completionResponse != null && 
                        completionResponse.choices.isNotEmpty() && 
                        completionResponse.choices[0].message.content.isNotEmpty()) {
                        return Result.success(completionResponse.choices[0].message.content.trim())
                    } else {
                        return Result.failure(Exception("요약 생성 결과가 비어있습니다."))
                    }
                } else {
                    val errorMessage = "요약 생성 실패: ${response.code()} ${response.message()}"
                    lastException = Exception(errorMessage)
                    
                    // 503 에러인 경우 재시도
                    if (response.code() == 503 && attempt < maxRetries - 1) {
                        kotlinx.coroutines.delay((attempt + 1) * 2000L) // 2초, 4초 대기
                        return@repeat
                    } else {
                        return Result.failure(lastException!!)
                    }
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay((attempt + 1) * 2000L) // 2초, 4초 대기
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("요약 생성에 실패했습니다."))
    }
}
