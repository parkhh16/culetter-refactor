package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * 레코드 상세 조회 응답 데이터 모델
 */
data class RecordDetailResponse(
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("result")
    val result: RecordDetailData
)

data class RecordDetailData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("storyId")
    val storyId: Int,
    
    @SerializedName("storyTheme")
    val storyTheme: String,
    
    @SerializedName("storyColor")
    val storyColor: String,
    
    @SerializedName("transcriptText")
    val transcriptText: String,
    
    @SerializedName("summaryText")
    val summaryText: String,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("time")
    val time: String
)

