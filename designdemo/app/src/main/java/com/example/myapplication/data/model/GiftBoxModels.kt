package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

/**
 * 선물함 관련 데이터 모델들
 */

// 선물함 아이템 (발신함/수신함 API 응답)
data class GiftBoxItem(
    @SerializedName("letterId")
    val letterId: Long,
    @SerializedName("reservationDate")
    val reservationDate: String,
    @SerializedName("receiverEmail")
    val receiverEmail: String,
    @SerializedName("title")
    val title: String
)

// NFT 상세 정보 (rendering API 요청)
data class NftRenderingRequest(
    @SerializedName("letterId")
    val letterId: Long
)

// NFT 상세 정보 (rendering API 응답)
data class NftRenderingResponse(
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("audioData")
    val audioData: String
)

// MailScreen에서 사용하는 MailItem으로 변환하는 확장 함수
fun GiftBoxItem.toMailItem(): MailItem {
    return MailItem(
        id = letterId.toString(),
        letterId = letterId, // 원본 letterId 보관
        title = title,
        dateTime = try {
            // ISO 형식 날짜를 LocalDateTime으로 변환
            LocalDateTime.parse(reservationDate.replace("Z", ""))
        } catch (e: Exception) {
            LocalDateTime.now()
        },
        partnerLabel = receiverEmail, // 이메일을 파트너 라벨로 사용
        content = "" // 상세보기에서 별도로 로드
    )
}

// 기존 MailItem 데이터 클래스 (MailScreen에서 사용)
data class MailItem(
    val id: String,
    val letterId: Long, // API 호출용 원본 letterId
    val title: String,
    val dateTime: LocalDateTime,
    val partnerLabel: String,
    val content: String
)