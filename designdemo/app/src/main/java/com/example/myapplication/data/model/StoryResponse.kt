package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * API 응답 데이터 모델
 */
data class StoryResponse(
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("result")
    val result: StoryData
)

data class StoryData(
    @SerializedName("storyId")
    val storyId: Int,
    
    @SerializedName("theme")
    val theme: String,
    
    @SerializedName("color")
    val color: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("daysFromStart")
    val daysFromStart: Int,
    
    @SerializedName("daysToEnd")
    val daysToEnd: Int,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("retrospect")
    val retrospect: Retrospect?,
    
    @SerializedName("records")
    val records: List<Record>,
    
    @SerializedName("recordsCount")
    val recordsCount: Int
)

data class Record(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("time")
    val time: String,
    
    @SerializedName("summaryText")
    val summaryText: String,
    
    @SerializedName("transcriptText")
    val transcriptText: String? = null
)

data class Retrospect(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("content")
    val content: String
)
