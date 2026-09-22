package com.example.myapplication.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionManager {

    private val REQUIRED = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    fun areAllPermissionsGranted(context: Context): Boolean =
        REQUIRED.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun getRequiredPermissions(): Array<String> = REQUIRED

    fun getMissingPermissions(context: Context): Array<String> =
        REQUIRED.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
            .toTypedArray()
}
