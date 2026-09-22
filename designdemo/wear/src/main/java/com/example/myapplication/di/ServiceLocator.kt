package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.Recorder
import com.example.myapplication.data.WearMessageRepository

object ServiceLocator {
    fun recorder(ctx: Context) = Recorder(ctx)
    fun wearRepo(ctx: Context) = WearMessageRepository(ctx)
}
