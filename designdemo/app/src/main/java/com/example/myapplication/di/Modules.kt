package com.example.myapplication.di

import com.example.myapplication.data.api.AuthApiService
import com.example.myapplication.data.api.GiftBoxApiService
import com.example.myapplication.data.repo.FakeRecordRepository
import com.example.myapplication.data.repo.RecordRepository
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.GiftBoxRepository
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 간단한 Service Locator (Hilt 도입 전 임시 DI).
 * 화면/뷰모델에서 필요한 Repository를 여기서 주입해서 사용합니다.
 */
object ServiceLocator {
    
    private const val BASE_URL = "http://j13a209.p.ssafy.io:8080"
    
    // Firebase
    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    // Network
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val giftBoxApiService: GiftBoxApiService by lazy {
        retrofit.create(GiftBoxApiService::class.java)
    }

    // Repositories
    val recordRepository: RecordRepository by lazy { FakeRecordRepository() }
    val authRepository: AuthRepository by lazy {
        AuthRepository(authApiService, firebaseAuth)
    }
    val giftBoxRepository: GiftBoxRepository by lazy {
        GiftBoxRepository(giftBoxApiService)
    }
}
