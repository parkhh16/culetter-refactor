package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.model.CalendarDetailData
import com.example.myapplication.data.model.CalendarEntry
import com.example.myapplication.data.model.CalendarResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 달력 데이터 리포지토리
 */
class CalendarRepository {
    
    private val apiService = ApiClient.calendarApiService
    
    suspend fun getCalendarEntries(token: String, year: Int? = null, month: Int? = null): Result<CalendarResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCalendars("Bearer $token", year, month)
                if (response.isSuccessful) {
                    val calendarResponse = response.body()
                    if (calendarResponse != null) {
                        Result.success(calendarResponse)
                    } else {
                        Result.failure(Exception("응답 데이터가 null입니다."))
                    }
                } else {
                    // 404 에러의 경우 빈 리스트로 처리 (스토리가 없는 경우)
                    if (response.code() == 404) {
                        Result.success(CalendarResponse(
                            status = 200,
                            code = 1000,
                            message = "데이터 없음",
                            progress = 0, // 기본 진행률
                            result = emptyList()
                        ))
                    } else {
                        Result.failure(Exception("API 호출 실패: ${response.code()} - ${response.message()}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getCalendarDetail(token: String, date: String): Result<CalendarDetailData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCalendarDetail("Bearer $token", date)
                if (response.isSuccessful) {
                    val detailResponse = response.body()
                    if (detailResponse != null) {
                        Result.success(detailResponse.result)
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
