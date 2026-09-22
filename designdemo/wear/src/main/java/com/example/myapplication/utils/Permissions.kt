package com.example.myapplication.utils

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

fun ComponentActivity.hasPerm(p: String) =
    ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

fun ComponentActivity.requestPerms(
    onResult: (Map<String, Boolean>) -> Unit
) = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), onResult)

