package com.example.myapplication.navigation

/**
 * 화면 라우트를 한 곳에서 관리하기 위한 sealed class.
 * 문자열이 흩어지지 않게 하려는 목적.
 */
sealed class NavItem(val route: String) {
    data object Login : NavItem("login")
    data object MetaMaskConnect : NavItem("metamask_connect")
    data object Home : NavItem("home")
    data object ThemeSetup : NavItem("theme_setup")
    data object Record : NavItem("record")          // 기록 버튼 목적지
    data object RecordDetail : NavItem("record_detail") {  // 기록 상세 화면
        fun createRoute() = "record_detail"
    }
//    data object Retrospect : NavItem("retrospect")  // 회고 버튼 목적지
    data object RetrospectCompleted : NavItem("retrospect_completed")
    data object RetrospectCall : NavItem("retrospect_call")
    data object RetrospectDetail : NavItem("retrospect_detail") {
        fun createRoute(title: String, content: String): String {
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encodedContent = java.net.URLEncoder.encode(content, "UTF-8")
            return "retrospect_detail?title=$encodedTitle&content=$encodedContent"
        }
    }

    data object Calendar : NavItem("calendar")
    data object Archive : NavItem("archive")
    data object Mail : NavItem("mail")
    data object Profile : NavItem("profile")

    object LetterParams : NavItem("letter_params")
    object LetterEdit : NavItem("letter/edit")
    object GiftSend : NavItem( "gift_send" )
}
