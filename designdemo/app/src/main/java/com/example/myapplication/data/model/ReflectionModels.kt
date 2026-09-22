package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * 회고 관련 데이터 모델
 */

// 질문 생성 요청
data class QuestionRequest(
    @SerializedName("records")
    val records: List<RecordForQuestion>
)

data class RecordForQuestion(
    @SerializedName("time")
    val time: String,
    
    @SerializedName("text")
    val text: String
)

// 질문 생성 응답
data class QuestionResponse(
    @SerializedName("emotions")
    val emotions: List<Emotion>,
    
    @SerializedName("keywords")
    val keywords: List<String>,
    
    @SerializedName("questions")
    val questions: List<QuestionGroup>,
    
    @SerializedName("overall_summary")
    val overallSummary: String
)

data class Emotion(
    @SerializedName("time")
    val time: String,
    
    @SerializedName("emotion")
    val emotion: String,
    
    @SerializedName("intensity")
    val intensity: Int,
    
    @SerializedName("rationale")
    val rationale: String
)

data class QuestionGroup(
    @SerializedName("record_time")
    val recordTime: String,
    
    @SerializedName("questions")
    val questions: List<String>
)

// 회고 답변 전송 요청
data class ReflectionRequest(
    @SerializedName("original_records")
    val originalRecords: List<RecordForQuestion>,
    
    @SerializedName("question_answers")
    val questionAnswers: List<QuestionAnswer>,
    
    @SerializedName("reflection_date")
    val reflectionDate: String
)

data class QuestionAnswer(
    @SerializedName("question")
    val question: String,
    
    @SerializedName("answer")
    val answer: String,
    
    @SerializedName("record_time")
    val recordTime: String
)

// 회고 답변 응답
data class ReflectionResponse(
    @SerializedName("daily_reflection")
    val dailyReflection: String,
    
    @SerializedName("summary")
    val summary: String
)

// 회고 저장 요청 (앱 DB)
data class RetrospectRequest(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("content")
    val content: String
)

// 회고 저장 응답 (앱 DB)
data class RetrospectResponse(
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("result")
    val result: RetrospectData?
)

data class RetrospectData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("createdAt")
    val createdAt: String
)
