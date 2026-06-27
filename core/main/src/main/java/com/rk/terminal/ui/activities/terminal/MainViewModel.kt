package com.rk.terminal.ui.activities.terminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.rk.settings.Settings
import com.rk.terminal.service.SessionService

class MainViewModel : ViewModel() {
    var sessionBinder by mutableStateOf<SessionService.SessionBinder?>(null)
        private set
    
    var isBound by mutableStateOf(false)
        private set

    var showStatusBar by mutableStateOf(Settings.statusBar)
    var horizontalStatusBar by mutableStateOf(Settings.horizontal_statusBar)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            sessionBinder = service as SessionService.SessionBinder
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            sessionBinder = null
        }
    }

    fun startAndBindService(context: Context) {
        val intent = Intent(context, SessionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            sessionBinder = null
        }
    }
}
