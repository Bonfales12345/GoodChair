package com.goodchair.launcher.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: CharSequence,
    val packageName: CharSequence,
    val icon: Drawable,
    val isSystemApp: Boolean = false
)