package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * 기록 생성 요청 데이터 모델
 */
data class RecordRequest(
    @SerializedName("transcriptText")
    val transcriptText: String,
    
    @SerializedName("summaryText")
    val summaryText: String
)

/**
 * 기록 생성 응답 데이터 모델
 */
data class RecordResponse(
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("result")
    val result: RecordData
)

data class RecordData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("storyId")
    val storyId: Int,
    
    @SerializedName("transcriptText")
    val transcriptText: String,
    
    @SerializedName("summaryText")
    val summaryText: String,
    
    @SerializedName("createdAt")
    val createdAt: String
)
