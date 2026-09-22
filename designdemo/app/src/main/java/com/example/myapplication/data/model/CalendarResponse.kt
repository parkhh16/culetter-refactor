package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * 달력 API 응답 데이터 모델
 */
data class CalendarResponse(
    @SerializedName("status")
    val status: Int,

    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("progress")
    val progress: Int, // 진행률 (0 ~ 100)

    @SerializedName("result")
    val result: List<CalendarEntry>
)

data class CalendarEntry(
    @SerializedName("id")
    val id: Int,

    @SerializedName("storyId")
    val storyId: Int,

    @SerializedName("entryDate")
    val entryDate: String, // "2025-09-19" 형식

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("color")
    val color: String, // 해당 스토리의 색상 (hex 형식: #4CAF50)

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)
