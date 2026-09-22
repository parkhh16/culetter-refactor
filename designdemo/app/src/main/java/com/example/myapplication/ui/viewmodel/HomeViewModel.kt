package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.StoryData
import com.example.myapplication.data.repository.StoryRepository
import com.example.myapplication.data.repository.StoryResult
import com.example.myapplication.utils.FirebaseTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * 홈 화면 ViewModel
 */
class HomeViewModel : ViewModel() {
    
    private val storyRepository = StoryRepository()
    private val firebaseTokenManager = FirebaseTokenManager()
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadStories()
    }
    
    fun loadStories() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "loadStories 시작")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasNoStory = false)
            
            val token = firebaseTokenManager.getCurrentUserToken()
            if (token == null) {
                Log.e("HomeViewModel", "Firebase 토큰이 null입니다")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Firebase 토큰을 가져올 수 없습니다. 로그인이 필요합니다.",
                    hasNoStory = false
                )
                return@launch
            }
            
            Log.d("HomeViewModel", "GET 요청 시작")
            when (val result = storyRepository.getStories(token)) {
                is StoryResult.Success -> {
                    Log.d("HomeViewModel", "스토리 로드 성공: ${result.data}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        storyData = result.data,
                        error = null,
                        hasNoStory = false
                    )
                }
                is StoryResult.NoStory -> {
                    Log.d("HomeViewModel", "스토리가 없음: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        storyData = null,
                        error = null,
                        hasNoStory = true
                    )
                }
                is StoryResult.Error -> {
                    Log.e("HomeViewModel", "스토리 로드 실패: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        storyData = null,
                        error = result.message,
                        hasNoStory = false
                    )
                }
            }
        }
    }
    
    fun refresh() {
        loadStories()
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val storyData: StoryData? = null,
    val error: String? = null,
    val hasNoStory: Boolean = false
)
