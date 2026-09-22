package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * 달력 상세 API 응답 데이터 모델
 */
data class CalendarDetailResponse(
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("result")
    val result: CalendarDetailData
)

data class CalendarDetailData(
    @SerializedName("retrospect")
    val retrospect: CalendarEntry,
    
    @SerializedName("records")
    val records: List<RecordDetail>
)

data class RecordDetail(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("time")
    val time: String, // "09:30" 형식
    
    @SerializedName("summaryText")
    val summaryText: String
)
