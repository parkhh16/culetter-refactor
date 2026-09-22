// app/src/main/java/com/example/myapplication/ui/components/mail/MailSummaryUi.kt
package com.example.myapplication.ui.components.mail

import java.time.LocalDateTime

data class MailSummaryUi(
    val id: String,
    val title: String,
    val dateTime: LocalDateTime,
    val partnerLabel: String
)
