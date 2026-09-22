// app/src/main/java/com/example/myapplication/ui/screens/MetaMaskConnectScreen.kt
package com.example.myapplication.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.navigation.NavItem
import com.example.myapplication.ui.viewmodel.WalletVM
import com.example.myapplication.ui.viewmodel.WalletVMFactory
import com.example.myapplication.data.api.UserApiService
import com.example.myapplication.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Retrofit 기본 BASE_URL (마지막에 / 포함 권장)
private const val BASE_URL = "http://43.201.19.75:8080/"

@Composable
fun MetaMaskConnectScreen(
    nav: NavController
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application

    // --- Hilt 없이 직접 의존성 구성 ---
    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val userApi = remember(retrofit) { retrofit.create(UserApiService::class.java) }
    val userRepo = remember { UserRepository(userApi, FirebaseAuth.getInstance()) }

    // 커스텀 팩토리로 ViewModel 생성
    val walletVM: WalletVM = viewModel(
        factory = WalletVMFactory(app, userRepo)
    )

    LaunchedEffect(Unit) { walletVM.init() }

    val connecting by walletVM.connecting.collectAsState()
    val patching by walletVM.patching.collectAsState()
    val patchOk by walletVM.patchOk.collectAsState()
    val address by walletVM.address.collectAsState()

    // 주소가 생기면 서버 PATCH
    LaunchedEffect(address) {
        if (!address.isNullOrEmpty()) walletVM.patchMe()
    }

    // PATCH 성공 시 홈으로 이동
    LaunchedEffect(patchOk) {
        if (patchOk == true) {
            nav.navigate(NavItem.Home.route) {
                popUpTo(NavItem.MetaMaskConnect.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // ====== UI (요청하신 스타일 유지) ======
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🦊", fontSize = 80.sp, modifier = Modifier.padding(bottom = 24.dp))

        Text(
            text = "메타마스크 연동",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "지갑을 연동하여\n더 안전하고 편리한 서비스를 이용하세요",
            fontSize = 16.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Button(
            onClick = { walletVM.connect(context) },
            enabled = !(connecting || patching),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF6851B))
        ) {
            if (connecting || patching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (patching) "저장 중..." else "연동 중...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            } else {
                Text(
                    text = "메타마스크 연동하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = {
                nav.navigate(NavItem.Home.route) {
                    popUpTo(NavItem.MetaMaskConnect.route) { inclusive = true }
                    launchSingleTop = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "건너뛰기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
            )
        }
    }
}
