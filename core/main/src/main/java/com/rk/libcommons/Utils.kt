package com.rk.libcommons

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import com.blankj.utilcode.util.ThreadUtils
import com.rk.resources.getString
import com.rk.terminal.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.roundToInt


@OptIn(DelicateCoroutinesApi::class)
 fun runOnUiThread(runnable: Runnable) {
    GlobalScope.launch(Dispatchers.Main) { runnable.run() }
}

fun toast(@StringRes resId: Int) {
    toast(resId.getString())
}

fun toast(message: String?) {
    if (message.isNullOrBlank()) {
        Log.w("UTILS", "Toast with null or empty message")
        return
    }
    if (message == "Job was cancelled") {
        Log.w("TOAST", message)
        return
    }
    if (currentActivity.get() == null){
        return
    }
    runOnUiThread { Toast.makeText(currentActivity.get(), message, Toast.LENGTH_SHORT).show() }
}

 fun toast(e: Exception? = null) {
    e?.printStackTrace()
    if (e != null) {
        toast(e.message)
    }
}

 fun toast(t: Throwable? = null) {
    t?.printStackTrace()
    toast(t?.message)
}

 fun String?.toastIt() {
    toast(this)
}

 fun toastCatching(block: () -> Unit): Exception? {
    try {
        block()
        return null
    } catch (e: Exception) {
        e.printStackTrace()
        toast(e.message)
        if (BuildConfig.DEBUG) {
            throw e
        }
        return e
    }
}

fun isDarkMode(ctx: Context): Boolean {
    return ((ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
}

 fun dpToPx(dp: Float, ctx: Context): Int {
    val density = ctx.resources.displayMetrics.density
    return (dp * density).roundToInt()
}

 fun isMainThread(): Boolean {
    return ThreadUtils.isMainThread()
}
