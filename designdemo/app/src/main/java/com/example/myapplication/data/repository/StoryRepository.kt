    package com.example.myapplication.data.repository

    import com.example.myapplication.data.api.ApiClient
    import com.example.myapplication.data.model.StoryData
    import com.example.myapplication.data.model.ThemeSetupRequest
    import com.example.myapplication.data.model.ThemeSetupResponse
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.withContext

    /**
     * 스토리 데이터 리포지토리
     */
    class StoryRepository {

        private val apiService = ApiClient.storyApiService

        suspend fun getStories(token: String): StoryResult {
            return withContext(Dispatchers.IO) {
                try {
                    val response = apiService.getStories("Bearer $token")
                    if (response.isSuccessful) {
                        val storyResponse = response.body()
                        if (storyResponse != null) {
                            StoryResult.Success(storyResponse.result)
                        } else {
                            StoryResult.Error("응답 데이터가 null입니다.")
                        }
                    } else {
                        when (response.code()) {
                            404 -> StoryResult.NoStory("진행 중인 스토리가 없습니다.")
                            else -> StoryResult.Error("API 호출 실패: ${response.code()} - ${response.message()}")
                        }
                    }
                } catch (e: Exception) {
                    StoryResult.Error(e.message ?: "알 수 없는 오류가 발생했습니다.")
                }
            }
        }

        suspend fun createTheme(token: String, request: ThemeSetupRequest): Result<ThemeSetupResponse> {
            return withContext(Dispatchers.IO) {
                try {
                    val response = apiService.createTheme("Bearer $token", request)
                    if (response.isSuccessful) {
                        val themeResponse = response.body()
                        if (themeResponse != null) {
                            Result.success(themeResponse)
                        } else {
                            Result.failure(Exception("응답 데이터가 null입니다."))
                        }
                    } else {
                        Result.failure(Exception("API 호출 실패: ${response.code()} - ${response.message()}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    sealed class StoryResult {
        data class Success(val data: StoryData) : StoryResult()
        data class NoStory(val message: String) : StoryResult()
        data class Error(val message: String) : StoryResult()
    }
