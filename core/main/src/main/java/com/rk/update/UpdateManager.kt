package com.rk.update

import android.content.Context
import com.rk.libcommons.ShizukuHelper

class UpdateManager(private val context: Context) {
    fun onUpdate() {
        ShizukuHelper.installRish(context)
    }
}
