package com.example.myapplication.data.api

import com.example.myapplication.data.model.NftMintingRequest
import com.example.myapplication.data.model.NftMintingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * NFT 관련 API 서비스 인터페이스
 */
interface NftApiService {
    
    @POST("/api/v1/nft/minting")
    suspend fun mintNft(
        @Header("Authorization") authorization: String,
        @Body request: NftMintingRequest
    ): Response<String>
}
