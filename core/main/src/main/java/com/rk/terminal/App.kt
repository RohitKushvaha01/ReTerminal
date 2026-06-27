package com.rk.terminal

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StrictMode
import com.github.anrwatchdog.ANRWatchDog
import com.rk.libcommons.application
import com.rk.resources.Res
import com.rk.terminal.ui.screens.terminal.TerminalUtils
import com.rk.update.UpdateManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

class App : Application() {

    companion object {
        fun getTempDir(context: Context): File {
            val tmp = File(context.cacheDir, "tmp")
            if (!tmp.exists()) {
                tmp.mkdir()
            }
            return tmp
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        application = this
        Res.application = this
        TerminalUtils.init(this)

        GlobalScope.launch(Dispatchers.IO) {
            getTempDir(this@App).apply {
                if (exists() && listFiles().isNullOrEmpty().not()) {
                    deleteRecursively()
                }
            }
        }

        ANRWatchDog().start()

        UpdateManager(this).onUpdate()

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().apply {
                    detectAll()
                    penaltyLog()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                            violation.printStackTrace()
                        }
                    }
                }.build()
            )
        }
    }
}
