package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.ThemeSetupRequest
import com.example.myapplication.data.repository.StoryRepository
import com.example.myapplication.utils.FirebaseTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.util.Log

/**
 * 테마 설정 화면 ViewModel
 */
class ThemeSetupViewModel : ViewModel() {
    
    private val storyRepository = StoryRepository()
    private val firebaseTokenManager = FirebaseTokenManager()
    
    private val _uiState = MutableStateFlow(ThemeSetupUiState())
    val uiState: StateFlow<ThemeSetupUiState> = _uiState.asStateFlow()
    
    fun updateTheme(theme: String) {
        _uiState.value = _uiState.value.copy(theme = theme)
    }
    
    fun updateColor(color: String) {
        _uiState.value = _uiState.value.copy(selectedColor = color)
    }
    
    fun updateEndDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(endDate = date)
    }
    
    fun createTheme() {
        val currentState = _uiState.value
        
        if (currentState.theme.isBlank()) {
            _uiState.value = currentState.copy(error = "테마명을 입력해주세요.")
            return
        }
        
        if (currentState.selectedColor.isBlank()) {
            _uiState.value = currentState.copy(error = "색상을 선택해주세요.")
            return
        }
        
        if (currentState.endDate == null) {
            _uiState.value = currentState.copy(error = "종료일을 선택해주세요.")
            return
        }
        
        val today = LocalDate.now()
        if (currentState.endDate!!.isBefore(today) || currentState.endDate!!.isEqual(today)) {
            _uiState.value = currentState.copy(error = "종료일은 오늘 이후로 선택해주세요.")
            return
        }
        
        viewModelScope.launch {
            Log.d("ThemeSetupViewModel", "테마 생성 시작")
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            val token = firebaseTokenManager.getCurrentUserToken()
            if (token == null) {
                Log.e("ThemeSetupViewModel", "Firebase 토큰이 null입니다")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Firebase 토큰을 가져올 수 없습니다. 로그인이 필요합니다."
                )
                return@launch
            }
            
            val request = ThemeSetupRequest(
                theme = currentState.theme,
                color = currentState.selectedColor,
                endedAt = currentState.endDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
            
            Log.d("ThemeSetupViewModel", "테마 생성 요청: $request")
            storyRepository.createTheme(token, request)
                .onSuccess { response ->
                    Log.d("ThemeSetupViewModel", "테마 생성 성공: $response")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                    Log.d("ThemeSetupViewModel", "isSuccess = true로 설정됨")
                }
                .onFailure { exception ->
                    Log.e("ThemeSetupViewModel", "테마 생성 실패: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "테마 생성 중 오류가 발생했습니다."
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ThemeSetupUiState(
    val theme: String = "",
    val selectedColor: String = "",
    val endDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
