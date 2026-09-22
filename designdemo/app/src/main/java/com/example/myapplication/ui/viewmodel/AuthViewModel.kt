package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.LoginResult
import com.example.myapplication.data.model.UserInfo
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authRepository = ServiceLocator.authRepository

    private val _loginState = MutableStateFlow<LoginResult?>(null)
    val loginState: StateFlow<LoginResult?> = _loginState.asStateFlow()

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // ✅ null=로딩/판단불가, true/false=결과
    private val _hasWallet = MutableStateFlow<Boolean?>(null)
    val hasWallet: StateFlow<Boolean?> = _hasWallet.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            _isLoggedIn.value = currentUser != null
            if (currentUser != null) {
                // 로그인 상태면 서버에서 최신 프로필/지갑 조회
                refreshProfileAndWallet()
            } else {
                _userInfo.value = null
                _hasWallet.value = null
            }
        }
    }

    /** 서버에서 이메일로 사용자 조회 → userInfo 갱신 + hasWallet 판정 */
    private suspend fun refreshProfileAndWallet() {
        _hasWallet.value = null // 로딩 표시
        // 프로필/지갑: 이메일 조회 API로 최신값 받음
        val (serverUser, code) = authRepository.fetchUserByEmail()
        if (code == 1000 && serverUser != null) {
            _userInfo.value = serverUser
            _hasWallet.value = !serverUser.walletAddress.isNullOrBlank()
        } else if (code == 1003) {
            // 회원 없음
            _hasWallet.value = false
        } else {
            // 판단 불가(네트워크/서버 오류) → null 유지
            _hasWallet.value = null
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = LoginResult.Loading

            val result = authRepository.signInWithGoogle(idToken)
            _loginState.value = result

            when (result) {
                is LoginResult.Success -> {
                    _userInfo.value = result.userInfo
                    _isLoggedIn.value = true
                    // 로그인 직후 지갑 상태 최신화
                    refreshProfileAndWallet()
                }
                is LoginResult.Error -> {
                    _isLoggedIn.value = false
                    _userInfo.value = null
                    _hasWallet.value = null
                }
                is LoginResult.Loading -> Unit
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _isLoggedIn.value = false
        _userInfo.value = null
        _loginState.value = null
        _hasWallet.value = null
    }

    fun clearLoginState() {
        _loginState.value = null
    }

    // ▶ 외부에서 재조회 버튼으로 호출할 공개 메서드
    fun refresh() = checkCurrentUser()
}
