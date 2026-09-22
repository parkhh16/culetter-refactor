package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class Letter(
    val id: Int,
    @SerializedName("storyId")
    val storyId: Int,
    val title: String,
    val content: String,
    val status: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String
)

data class LetterResponse(
    val status: Int,
    val code: Int,
    val message: String,
    val result: List<Letter>
)
