package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * NFT 민팅 관련 데이터 모델들
 */

// NFT 민팅 요청
data class NftMintingRequest(
    @SerializedName("receiverEmail")
    val receiverEmail: String,
    @SerializedName("reservationDate")
    val reservationDate: String,
    @SerializedName("letterId")
    val letterId: Int
)

// NFT 민팅 응답
data class NftMintingResponse(
    val status: Int,
    val code: Int,
    val message: String,
    val result: NftMintingData?
)

data class NftMintingData(
    val nftId: Int,
    val transactionHash: String?,
    val tokenUri: String?
)

// NFT 민팅 결과 상태
sealed class NftMintingResult {
    data class Success(val result: NftMintingData) : NftMintingResult()
    data class Error(val message: String) : NftMintingResult()
    object Loading : NftMintingResult()
}
