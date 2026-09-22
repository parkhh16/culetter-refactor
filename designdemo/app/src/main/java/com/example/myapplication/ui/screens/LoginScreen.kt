// LoginScreen.kt
package com.example.myapplication.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.myapplication.R
import com.example.myapplication.navigation.NavItem
import com.example.myapplication.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.widget.Button

@Composable
fun LoginScreen(
    nav: NavController,
    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val loginState by authViewModel.loginState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    
    // Google Sign-In 클라이언트 설정
    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("73517048057-cuec39sbq2qf0ephdnfi6nbrha2d4737.apps.googleusercontent.com")
                .requestEmail()
                .build()
        )
    }
    
    // 구글 로그인 결과 처리
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                authViewModel.signInWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "구글 로그인 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val hasWallet by authViewModel.hasWallet.collectAsState(initial = null)

    LaunchedEffect(isLoggedIn, hasWallet) {
        if (isLoggedIn && hasWallet != null) {
            if (hasWallet == true) {
                nav.navigate(NavItem.Home.route) {
                    popUpTo(NavItem.Login.route) { inclusive = true }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                nav.navigate(NavItem.MetaMaskConnect.route) {
                    popUpTo(NavItem.Login.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    
    // 로그인 상태에 따른 토스트 메시지
    LaunchedEffect(loginState) {
        when (val currentState = loginState) {
            is com.example.myapplication.data.model.LoginResult.Success -> {
                val message = if (currentState.isNewUser) "회원가입이 완료되었습니다!" else "로그인되었습니다!"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            is com.example.myapplication.data.model.LoginResult.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                authViewModel.clearLoginState()
            }
            is com.example.myapplication.data.model.LoginResult.Loading -> {
                // 로딩 상태는 UI에서 처리
            }
            null -> {
                // 초기 상태, 아무것도 하지 않음
            }
        }
    }

    // MP4 비디오 배경을 사용하는 레이아웃
    AndroidView(
        factory = { context ->
            android.view.LayoutInflater.from(context).inflate(R.layout.activity_login_video, null)
        },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        // 비디오 설정 및 재생
        val videoView = view.findViewById<VideoView>(R.id.loginVideo)
        val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.login_screen}")
        
        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            mediaPlayer.start()
        }
        
        // Google 로그인 버튼 클릭 이벤트
        val googleButton = view.findViewById<Button>(R.id.googleLoginButton)
        googleButton.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
        
        // 로딩 상태에 따른 버튼 업데이트
        when (loginState) {
            is com.example.myapplication.data.model.LoginResult.Loading -> {
                googleButton.text = "로그인 중..."
                googleButton.isEnabled = false
            }
            else -> {
                googleButton.text = "Google로 로그인하기"
                googleButton.isEnabled = true
            }
        }
    }
}