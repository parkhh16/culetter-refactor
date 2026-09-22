package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.model.Letter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DrawerViewModel : ViewModel() {
    private val _letters = MutableStateFlow<List<Letter>>(emptyList())
    val letters: StateFlow<List<Letter>> = _letters.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadLetters()
    }
    
    fun loadLetters() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    user.getIdToken(true).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result?.token
                            if (token != null) {
                                viewModelScope.launch {
                                    try {
                                        val response = ApiClient.letterApiService.getLetters("Bearer $token")
                                        if (response.isSuccessful) {
                                            _letters.value = response.body()?.result ?: emptyList()
                                        } else {
                                            _error.value = "편지를 불러오는데 실패했습니다: ${response.code()}"
                                        }
                                    } catch (e: Exception) {
                                        _error.value = "편지를 불러오는데 실패했습니다: ${e.message}"
                                    } finally {
                                        _isLoading.value = false
                                    }
                                }
                            } else {
                                _error.value = "토큰을 가져올 수 없습니다"
                                _isLoading.value = false
                            }
                        } else {
                            _error.value = "인증에 실패했습니다"
                            _isLoading.value = false
                        }
                    }
                } else {
                    _error.value = "로그인이 필요합니다"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "편지를 불러오는데 실패했습니다: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun refresh() {
        loadLetters()
    }
}
