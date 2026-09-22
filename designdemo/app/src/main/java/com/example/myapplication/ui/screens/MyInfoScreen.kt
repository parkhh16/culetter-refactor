package com.example.myapplication.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.UserInfo
import com.example.myapplication.data.model.LoginResult
import com.example.myapplication.ui.components.FrostedCard
import com.example.myapplication.ui.components.VideoBackground
import com.example.myapplication.ui.viewmodel.AuthViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.foundation.text.selection.SelectionContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyInfoScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val vm: AuthViewModel = viewModel()

    val userInfo by vm.userInfo.collectAsState()
    val isLoggedIn by vm.isLoggedIn.collectAsState()
    val hasWallet by vm.hasWallet.collectAsState()
    val loginState by vm.loginState.collectAsState()

    val haze = remember { HazeState() }

    // ✅ 비디오 배경 + 상단 UI 오버레이
    Box(
        modifier = Modifier
            .fillMaxSize()
            .haze(haze) // FrostedCard의 hazeEffect와 동일 state 공유
    ) {
        // 바닥 레이어: 비디오 배경 (리소스 이름은 필요에 맞게 교체 가능)
        VideoBackground(
            videoUri = Uri.parse("android.resource://com.example.myapplication/raw/home_screen"),
            modifier = Modifier.fillMaxSize()
        )

        // 상단 레이어: 실제 화면 UI
        Scaffold(
            containerColor = Color.Transparent, // 배경 투명 (영상 보이게)
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "내 정보",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.refresh() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "새로고침",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            val isLoadingUi = (loginState is LoginResult.Loading) && userInfo == null

            when {
                isLoadingUi -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }

                        item { ProfileCard(haze = haze, userInfo = userInfo) }

                        item {
                            WalletStatusCard(
                                haze = haze,
                                hasWallet = hasWallet,
                                userInfo = userInfo,
                                onSetWallet = { onNavigate("walletaddress") }
                            )
                        }

                        item {
                            BackendStatusCard(
                                haze = haze,
                                isLoggedIn = isLoggedIn,
                                loginState = loginState,
                                userInfo = userInfo,
                                onRetry = { vm.refresh() },
                                onSignOut = { vm.signOut() }
                            )
                        }

                        item { SettingsCard(haze = haze) }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

/* --------------------------------- Cards --------------------------------- */

@Composable
private fun ProfileCard(
    haze: HazeState,
    userInfo: UserInfo?
) {
    FrostedCard(
        hazeState = haze,
        corner = 20.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(12.dp))

            val name = userInfo?.name ?: "이름 없음"
            val email = userInfo?.email ?: "이메일 없음"
            val nickname = userInfo?.nickname

            Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (!nickname.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("@$nickname", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(email, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WalletStatusCard(
    haze: HazeState,
    hasWallet: Boolean?,
    userInfo: UserInfo?,
    onSetWallet: () -> Unit
) {
    FrostedCard(
        hazeState = haze,
        corner = 20.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Text("지갑", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))

            when (hasWallet) {
                null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("지갑 정보를 확인하는 중...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                true -> {
                    val addr = userInfo?.walletAddress.orEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        SelectionContainer {
                            Text(
                                text = "지갑 주소: $addr",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                false -> {
                    Text("등록된 지갑이 없습니다.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onSetWallet,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("지갑 주소 설정", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackendStatusCard(
    haze: HazeState,
    isLoggedIn: Boolean,
    loginState: LoginResult?,
    userInfo: com.example.myapplication.data.model.UserInfo?,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    FrostedCard(
        hazeState = haze,
        corner = 20.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("백엔드 로그인", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRetry) { Text("재조회") }
                    if (isLoggedIn) TextButton(onClick = onSignOut) { Text("로그아웃") }
                }
            }
            Spacer(Modifier.height(12.dp))

            when {
                loginState is LoginResult.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("로그인 상태 확인 중...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                loginState is LoginResult.Success -> {
                    val u = loginState.userInfo
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E8)) {
                        Text(
                            "✅ 로그인됨: ${u.name} (${u.email})",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                userInfo != null -> {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E8)) {
                        Text(
                            "✅ 로그인됨: ${userInfo.name} (${userInfo.email})",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                loginState is LoginResult.Error -> {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFEBEE)) {
                        Text(
                            "❌ 로그인 실패: ${loginState.message}",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
                isLoggedIn -> {
                    Text("로그인됨 (프로필 로딩 중…)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    Text("로그인 상태를 확인할 수 없습니다.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(haze: HazeState) {
    FrostedCard(
        hazeState = haze,
        corner = 20.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Text("설정", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            SettingRow(Icons.Default.Notifications, "알림 설정", "푸시 알림 및 리마인더") { /* TODO */ }
            SettingRow(Icons.Default.Palette, "테마 설정", "앱 테마 및 색상 변경") { /* TODO */ }
            SettingRow(Icons.Default.Backup, "데이터 백업", "기록 데이터 백업 및 복원") { /* TODO */ }
            SettingRow(Icons.Default.Help, "도움말", "자주 묻는 질문 및 문의") { /* TODO */ }
            SettingRow(Icons.Default.Info, "앱 정보", "버전 정보 및 라이선스") { /* TODO */ }
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

/* Utils */
private fun shorten(addr: String, head: Int = 10, tail: Int = 6) =
    if (addr.length <= head + tail) addr else "${addr.take(head)}...${addr.takeLast(tail)}"
