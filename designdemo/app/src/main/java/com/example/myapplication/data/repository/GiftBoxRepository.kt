package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.api.GiftBoxApiService
import com.example.myapplication.data.model.GiftBoxItem
import com.example.myapplication.data.model.MailItem
import com.example.myapplication.data.model.NftRenderingResponse
import com.example.myapplication.data.model.toMailItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GiftBoxRepository @Inject constructor(
    private val giftBoxApiService: GiftBoxApiService
) {

    /**
     * 발신함 조회
     */
    suspend fun getSentTokens(token: String): Result<List<MailItem>> {
        return try {
            val response = giftBoxApiService.getSentTokens("Bearer $token")
            if (response.isSuccessful) {
                val giftBoxItems = response.body() ?: emptyList()
                val mailItems = giftBoxItems.map { it.toMailItem() }
                Result.success(mailItems)
            } else {
                Log.e("GiftBoxRepository", "발신함 조회 실패: ${response.code()}")
                Result.failure(Exception("발신함 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("GiftBoxRepository", "발신함 조회 에러", e)
            Result.failure(e)
        }
    }

    /**
     * 수신함 조회
     */
    suspend fun getReceivedTokens(token: String): Result<List<MailItem>> {
        return try {
            val response = giftBoxApiService.getReceivedTokens("Bearer $token")
            if (response.isSuccessful) {
                val giftBoxItems = response.body() ?: emptyList()
                val mailItems = giftBoxItems.map { it.toMailItem() }
                Result.success(mailItems)
            } else {
                Log.e("GiftBoxRepository", "수신함 조회 실패: ${response.code()}")
                Result.failure(Exception("수신함 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("GiftBoxRepository", "수신함 조회 에러", e)
            Result.failure(e)
        }
    }

    /**
     * NFT 상세 정보 조회
     */
    suspend fun getNftDetails(token: String, letterId: Long): Result<NftRenderingResponse> {
        return try {
            val response = giftBoxApiService.getNftDetails("Bearer $token", letterId)
            if (response.isSuccessful) {
                val details = response.body()
                if (details != null) {
                    Result.success(details)
                } else {
                    Result.failure(Exception("NFT 상세 정보가 없습니다"))
                }
            } else {
                Log.e("GiftBoxRepository", "NFT 상세 조회 실패: ${response.code()}")
                Result.failure(Exception("NFT 상세 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("GiftBoxRepository", "NFT 상세 조회 에러", e)
            Result.failure(e)
        }
    }
}