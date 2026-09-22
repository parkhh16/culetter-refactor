package com.example.myapplication.data.api

import com.example.myapplication.data.model.CalendarDetailResponse
import com.example.myapplication.data.model.CalendarResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * 달력 관련 API 서비스 인터페이스
 */
interface CalendarApiService {
    
    @GET("api/calendars")
    suspend fun getCalendars(
        @Header("Authorization") token: String,
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null
    ): Response<CalendarResponse>
    
    @GET("api/calendars/detail")
    suspend fun getCalendarDetail(
        @Header("Authorization") token: String,
        @Query("date") date: String
    ): Response<CalendarDetailResponse>
}
