package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.model.DailyReflection
import com.example.myapplication.data.model.LetterCreateRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LetterParamsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val generatedTitle: String = "",
    val generatedContent: String = ""
)

class LetterParamsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LetterParamsUiState())
    val uiState: StateFlow<LetterParamsUiState> = _uiState.asStateFlow()
    
    fun createLetter(
        receiver: String,
        sender: String,
        tone: String,
        mood: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    user.getIdToken(true).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result?.token
                            if (token != null) {
                                viewModelScope.launch {
                                    try {
                                        // 1. 회고 데이터 조회 (theme 포함)
                                        val retrospectResponse = ApiClient.letterRetrospectApiService.getRetrospects("Bearer $token")
                                        if (retrospectResponse.isSuccessful) {
                                            val retrospectData = retrospectResponse.body()?.result
                                            if (retrospectData != null) {
                                                // 회고 데이터를 DailyReflection 형태로 변환
                                                val reflections = retrospectData.retrospects.map { retrospect ->
                                                    DailyReflection(
                                                        date = retrospect.entryDate,
                                                        dailyReflection = retrospect.content
                                                    )
                                                }
                                                
                                                // 2. 편지 생성 요청 (theme은 API에서 받은 값 사용)
                                                val letterRequest = LetterCreateRequest(
                                                    receiver = receiver,
                                                    sender = sender,
                                                    theme = retrospectData.theme, // API에서 받은 theme 사용
                                                    reflections = reflections,
                                                    mood = mood,
                                                    tone = tone
                                                )
                                                
                                                val letterResponse = ApiClient.letterGenerationApiService.createLetter(letterRequest)
                                                if (letterResponse.isSuccessful) {
                                                    val letterData = letterResponse.body()
                                                    if (letterData != null) {
                                                        _uiState.value = _uiState.value.copy(
                                                            isLoading = false,
                                                            generatedTitle = letterData.letterTitle,
                                                            generatedContent = letterData.letterContent
                                                        )
                                                    } else {
                                                        _uiState.value = _uiState.value.copy(
                                                            isLoading = false,
                                                            error = "편지 생성에 실패했습니다"
                                                        )
                                                    }
                                                } else {
                                                    _uiState.value = _uiState.value.copy(
                                                        isLoading = false,
                                                        error = "편지 생성에 실패했습니다: ${letterResponse.code()}"
                                                    )
                                                }
                                            } else {
                                                _uiState.value = _uiState.value.copy(
                                                    isLoading = false,
                                                    error = "회고 데이터를 불러올 수 없습니다"
                                                )
                                            }
                                        } else {
                                            _uiState.value = _uiState.value.copy(
                                                isLoading = false,
                                                error = "회고 데이터 조회에 실패했습니다: ${retrospectResponse.code()}"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            error = "편지 생성 중 오류가 발생했습니다: ${e.message}"
                                        )
                                    }
                                }
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "토큰을 가져올 수 없습니다"
                                )
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "인증에 실패했습니다"
                            )
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "로그인이 필요합니다"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "편지 생성 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }
}
