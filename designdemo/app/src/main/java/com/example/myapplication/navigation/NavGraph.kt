// app/src/main/java/com/example/myapplication/navigation/AppNavGraph.kt
package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.common.SimpleCenter
import com.example.myapplication.data.state.RecordState
import dev.chrisbanes.haze.HazeState

@Composable
fun AppNavGraph(
    navController: NavHostController,
    hazeState: HazeState
) {
    NavHost(
        navController = navController,
        startDestination = NavItem.Login.route
    ) {
        composable(NavItem.Login.route) {
            LoginScreen(nav = navController)
        }

        composable(NavItem.MetaMaskConnect.route) {
            MetaMaskConnectScreen(nav = navController)
        }

        composable(
            route = "${NavItem.Home.route}?refresh={refresh}",
            arguments = listOf(
                androidx.navigation.navArgument("refresh") {
                    defaultValue = false
                    type = androidx.navigation.NavType.BoolType
                }
            )
        ) { backStackEntry ->
            val refresh = backStackEntry.arguments?.getBoolean("refresh") ?: false
            HomeScreen(
                onRecordClick = { navController.navigate(NavItem.Record.route) },
                onRetrospectClick = { navController.navigate(NavItem.RetrospectCall.route) },
                onRecordDetailClick = {
                    navController.navigate(NavItem.RecordDetail.createRoute())
                },
                onRetrospectDetailClick = { title, content ->
                    navController.navigate(NavItem.RetrospectDetail.createRoute(title, content))
                },
                onThemeSetupClick = { navController.navigate(NavItem.ThemeSetup.route) },
                hazeState = hazeState,
                shouldRefresh = refresh
            )
        }

        composable(NavItem.ThemeSetup.route) {
            ThemeSetupScreen(
                onBackClick = { navController.popBackStack() },
                onSuccess = { 
                    navController.navigate("${NavItem.Home.route}?refresh=true") {
                        popUpTo(NavItem.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                hazeState = hazeState
            )
        }

        composable(NavItem.Record.route) {
            RecordScreen(
                onBackClick = { navController.popBackStack() },
                hazeState = hazeState
            )
        }

        composable(NavItem.RecordDetail.route) {
            val selectedRecord = RecordState.getSelectedRecord()
            if (selectedRecord != null) {
                RecordDetailScreen(
                    record = selectedRecord,
                    onBackClick = {
                        RecordState.clearSelectedRecord()
                        navController.popBackStack()
                    },
                    hazeState = hazeState
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(NavItem.Calendar.route) {
            CalendarScreen(
                onRetrospectDetailClick = { title, content ->
                    navController.navigate(NavItem.RetrospectDetail.createRoute(title, content))
                }
            )
        }

//        composable(NavItem.Retrospect.route) {
//            val homeViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.myapplication.ui.viewmodel.HomeViewModel>()
//            val homeUiState by homeViewModel.uiState.collectAsState()
//            if (homeUiState.storyData?.records?.isNotEmpty() == true) {
//                ReflectionScreen(
//                    records = homeUiState.storyData!!.records,
//                    onBackClick = { navController.popBackStack() },
//                    hazeState = hazeState
//                )
//            } else {
//                SimpleCenter("회고할 기록이 없습니다.")
//            }
//        }

        composable(NavItem.RetrospectCall.route) {
            val homeViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel<com.example.myapplication.ui.viewmodel.HomeViewModel>()
            val homeUiState by homeViewModel.uiState.collectAsState()
            val records = homeUiState.storyData?.records ?: emptyList()

            if (records.isNotEmpty()) {
                CallReflectionScreen(
                    records = records,
                    onBackClick = { navController.popBackStack() },
                    hazeState = hazeState,

                    // ✅ "완료" 버튼 → 홈 이동 (상단 스택 정리)
                    onHome = {
                        navController.navigate(NavItem.Home.route) {
                            // 로그인(Login)이 startDestination이라면, 그 위 스택을 깨끗이 비우고 홈만 올립니다.
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },

                    // (선택) COMPLETED 시 별도 완료 라우트로 이동하고 싶다면 켜세요
                    // onNavigateToCompleted = {
                    //     navController.navigate(NavItem.RetrospectCompleted.route) {
                    //         launchSingleTop = true
                    //     }
                    // }
                )
            } else {
                SimpleCenter("회고할 기록이 없습니다.")
            }
        }




        composable(
            route = "${NavItem.RetrospectDetail.route}?title={title}&content={content}",
            arguments = listOf(
                androidx.navigation.navArgument("title") { defaultValue = "" },
                androidx.navigation.navArgument("content") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
            val encodedContent = backStackEntry.arguments?.getString("content") ?: ""
            val title = try { java.net.URLDecoder.decode(encodedTitle, "UTF-8") } catch (e: Exception) { encodedTitle }
            val content = try { java.net.URLDecoder.decode(encodedContent, "UTF-8") } catch (e: Exception) { encodedContent }

            RetrospectDetailScreen(
                title = title,
                content = content,
                onBackClick = { navController.popBackStack() },
                hazeState = hazeState
            )
        }

        // Drawer(보관함)
        composable(NavItem.Archive.route)  {
            DrawerScreen(
                onCreateLetterClick = {
                    navController.navigate(NavItem.LetterParams.route)
                },
                onNavigateToGiftSend = { letterId ->
                    navController.navigate("${NavItem.GiftSend.route}?letterId=$letterId")
                }
            )
        }

        // 1) 파라미터 입력 → 편지 생성 후 편집 화면으로
        composable(NavItem.LetterParams.route) {
            LetterParamsScreen(
                onBack = { navController.popBackStack() },
                onLetterCreated = { title, content ->
                    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                    val encodedContent = java.net.URLEncoder.encode(content, "UTF-8")
                    navController.navigate("${NavItem.LetterEdit.route}?title=${encodedTitle}&content=${encodedContent}") {
                        // LetterParams를 백스택에서 제거하고 LetterEdit로 이동
                        popUpTo(NavItem.LetterParams.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 2) 편집 화면 → 저장 시 Drawer로 복귀
        composable(
            route = "${NavItem.LetterEdit.route}?title={title}&content={content}",
            arguments = listOf(
                androidx.navigation.navArgument("title") {
                    defaultValue = "보통을 채우는 커피"
                },
                androidx.navigation.navArgument("content") {
                    defaultValue = "블라블라블라블라블 ....\n블라블라블라블라블 ...."
                }
            )
        ) { backStackEntry ->
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: "보통을 채우는 커피"
            val encodedContent = backStackEntry.arguments?.getString("content") ?: "블라블라블라블라블 ....\n블라블라블라블라블 ...."
            val title = try { java.net.URLDecoder.decode(encodedTitle, "UTF-8") } catch (e: Exception) { encodedTitle }
            val content = try { java.net.URLDecoder.decode(encodedContent, "UTF-8") } catch (e: Exception) { encodedContent }

            LetterEditScreen(
                hazeState = hazeState,
                title = title,
                initialContent = content,
                onBack = { navController.popBackStack() },
                onSaveAndGoDrawer = {
                    navController.navigate(NavItem.Archive.route) {
                        // Drawer가 백스택에 있으면 거기로 올라가고, 없으면 새로 띄움
                        popUpTo(NavItem.Archive.route) { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(
            route = "${NavItem.GiftSend.route}?letterId={letterId}",
            arguments = listOf(
                androidx.navigation.navArgument("letterId") {
                    defaultValue = 1
                }
            )
        ) { backStackEntry ->
            val letterId = backStackEntry.arguments?.getInt("letterId") ?: 1

            GiftSendScreen(
                letterId = letterId,
                onClose = { navController.popBackStack() },
                onGiftDone = {
                    // 성공 시 Drawer로 돌아가고 싶다면:
                    navController.popBackStack()         // 현재 화면 닫기
                    // navController.navigate(NavItem.Archive.route)  // 필요 시 이동
                }
            )
        }

        composable(NavItem.Mail.route) {
            MailScreen(hazeState = hazeState)
        }

        composable(NavItem.Profile.route)  {
            MyInfoScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
