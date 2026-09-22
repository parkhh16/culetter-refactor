package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class DailyReflection(
    val date: String,
    @SerializedName("daily_reflection")
    val dailyReflection: String
)

data class LetterCreateRequest(
    val receiver: String,
    val sender: String,
    val theme: String,
    val reflections: List<DailyReflection>,
    val mood: String,
    val tone: String
)

data class LetterCreateResponse(
    @SerializedName("letter_content")
    val letterContent: String,
    @SerializedName("letter_title")
    val letterTitle: String
)

data class LetterSaveRequest(
    val title: String,
    val content: String
)

data class LetterSaveResponse(
    val status: Int,
    val code: Int,
    val message: String,
    val result: Letter
)

// 편지 생성용 회고 조회 응답
data class LetterRetrospectResponse(
    val status: Int,
    val code: Int,
    val message: String,
    val result: LetterRetrospectResult
)

data class LetterRetrospectResult(
    @SerializedName("storyId")
    val storyId: Int,
    val theme: String,
    val color: String,
    val retrospects: List<LetterRetrospect>,
    @SerializedName("totalCount")
    val totalCount: Int
)

data class LetterRetrospect(
    @SerializedName("entryDate")
    val entryDate: String,
    val title: String,
    val content: String
)
