package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.myapplication.R
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.navigation.AppNavGraph
import com.example.myapplication.navigation.NavItem
import com.example.myapplication.ui.components.BottomNav

// 🔹 Haze
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze // (당신 프로젝트에서 사용하던 API에 맞춤: 소스 쪽)
import dev.chrisbanes.haze.hazeSource

@Composable
fun App() {
    val nav = rememberNavController()
    val backstack by nav.currentBackStackEntryAsState()
    val currentRoute = backstack?.destination?.route ?: NavItem.Home.route

    // 🔹 전역 Haze 상태 (글래스 블러 공유용)
    val haze = remember { HazeState() }

    // 🔹 전역 배경(default.png 이미지) + 전역 Haze 소스
    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(haze) // ✅ 전역 배경을 Haze 소스로 등록 (자식들은 이 상태로 hazeChild 적용)
    ) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.raw.default_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(Modifier.fillMaxSize()) {
            // (1) 콘텐츠
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // ✅ 필요하면 이 haze를 NavGraph 내부로 내려서 각 화면에서 사용
                AppNavGraph(
                    navController = nav,
                    hazeState = haze,
                    /* 예: 필요시 전달하도록 시그니처 확장
                       hazeState = haze
                    */
                )
            }

            // ✅ 로그인 화면 or 메타마스크 로그인에서는 바텀 네비 숨김
            if (currentRoute != NavItem.Login.route && currentRoute != NavItem.MetaMaskConnect.route) {
                // FrostedBottomNav(...) 또는 BottomNav(...)
                BottomNav(
                    currentRoute = currentRoute,
                    onSelect = { item ->
                        nav.navigate(item.route) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    hazeState = haze
                )
            }
        }
    }
}
