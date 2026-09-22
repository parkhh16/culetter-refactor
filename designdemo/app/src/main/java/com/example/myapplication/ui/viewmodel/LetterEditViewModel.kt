package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.model.LetterSaveRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LetterEditUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

class LetterEditViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LetterEditUiState())
    val uiState: StateFlow<LetterEditUiState> = _uiState.asStateFlow()
    
    fun saveLetter(
        title: String,
        content: String,
        onSuccess: () -> Unit
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
                                        val request = LetterSaveRequest(
                                            title = title,
                                            content = content
                                        )
                                        
                                        val response = ApiClient.letterApiService.saveLetter("Bearer $token", request)
                                        if (response.isSuccessful) {
                                            _uiState.value = _uiState.value.copy(
                                                isLoading = false,
                                                isSaved = true
                                            )
                                            onSuccess()
                                        } else {
                                            _uiState.value = _uiState.value.copy(
                                                isLoading = false,
                                                error = "편지 저장에 실패했습니다: ${response.code()}"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            error = "편지 저장 중 오류가 발생했습니다: ${e.message}"
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
                    error = "편지 저장 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }
}
