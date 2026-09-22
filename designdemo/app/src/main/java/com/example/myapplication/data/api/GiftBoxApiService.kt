package com.example.myapplication.data.api

import com.example.myapplication.data.model.GiftBoxItem
import com.example.myapplication.data.model.NftRenderingRequest
import com.example.myapplication.data.model.NftRenderingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * 선물함 관련 API 서비스 인터페이스
 */
interface GiftBoxApiService {

    /**
     * 발신함 조회
     */
    @GET("/api/v1/nft/sentTokens")
    suspend fun getSentTokens(
        @Header("Authorization") authorization: String
    ): Response<List<GiftBoxItem>>

    /**
     * 수신함 조회
     */
    @GET("/api/v1/nft/receivedTokens")
    suspend fun getReceivedTokens(
        @Header("Authorization") authorization: String
    ): Response<List<GiftBoxItem>>

    /**
     * NFT 상세보기
     */
    @GET("/api/v1/nft/rendering")
    suspend fun getNftDetails(
        @Header("Authorization") authorization: String,
        @Query("letterId") letterId: Long
    ): Response<NftRenderingResponse>
}