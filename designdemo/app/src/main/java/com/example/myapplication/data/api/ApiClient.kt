package com.example.myapplication.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API 클라이언트 설정
 */
object ApiClient {
    private const val BASE_URL = "http://j13a209.p.ssafy.io:8080/"
    private const val REFLECTION_BASE_URL = "http://j13a209.p.ssafy.io:8082/"
    private const val GMS_BASE_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/"
    private const val VOICE_BASE_URL = "http://j13a209.p.ssafy.io:8081/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val reflectionRetrofit = Retrofit.Builder()
        .baseUrl(REFLECTION_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val gmsRetrofit = Retrofit.Builder()
        .baseUrl(GMS_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val voiceRetrofit = Retrofit.Builder()
        .baseUrl(VOICE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val storyApiService: StoryApiService = retrofit.create(StoryApiService::class.java)
    val calendarApiService: CalendarApiService = retrofit.create(CalendarApiService::class.java)
    val reflectionApiService: ReflectionApiService = reflectionRetrofit.create(ReflectionApiService::class.java)
    val gptApiService: GptApiService = gmsRetrofit.create(GptApiService::class.java)
    val letterApiService: LetterApiService = retrofit.create(LetterApiService::class.java)
    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val nftApiService: NftApiService = retrofit.create(NftApiService::class.java)
    val letterRetrospectApiService: LetterRetrospectApiService = retrofit.create(LetterRetrospectApiService::class.java)
    val letterGenerationApiService: LetterGenerationApiService = reflectionRetrofit.create(LetterGenerationApiService::class.java)
    val voiceApiService: VoiceApiService = voiceRetrofit.create(VoiceApiService::class.java)
}
