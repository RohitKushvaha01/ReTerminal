package com.rk.terminal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.rk.resources.drawables
import com.rk.resources.strings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.terminal.MkSession
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

class SessionService : Service() {
    private val sessions = hashMapOf<String, TerminalSession>()
    val sessionList = mutableStateMapOf<String, Int>()
    val sessionOrder = mutableStateListOf<String>()
    var currentSession = mutableStateOf(Pair("main", com.rk.settings.Settings.working_Mode))

    inner class SessionBinder : Binder() {
        fun getService(): SessionService = this@SessionService
        
        fun terminateAllSessions() {
            sessions.values.forEach { it.finishIfRunning() }
            sessions.clear()
            sessionList.clear()
            sessionOrder.clear()
            updateNotification()
        }

        fun createSession(
            id: String,
            client: TerminalSessionClient,
            workingMode: Int
        ): TerminalSession {
            return MkSession.createSession(
                context = this@SessionService,
                sessionClient = client,
                sessionId = id,
                workingMode = workingMode
            ).also {
                sessions[id] = it
                sessionList[id] = workingMode
                if (!sessionOrder.contains(id)) {
                    sessionOrder.add(id)
                }
                updateNotification()
            }
        }

        fun getSession(id: String): TerminalSession? = sessions[id]

        fun renameSession(oldId: String, newId: String): Boolean {
            val trimmed = newId.trim()
            if (trimmed.isEmpty()) return false
            if (trimmed == oldId) return true
            if (sessions.containsKey(trimmed)) return false

            val session = sessions.remove(oldId) ?: return false
            val mode = sessionList.remove(oldId) ?: com.rk.settings.Settings.working_Mode
            sessions[trimmed] = session
            sessionList[trimmed] = mode

            val idx = sessionOrder.indexOf(oldId)
            if (idx != -1) {
                sessionOrder[idx] = trimmed
            } else {
                sessionOrder.add(trimmed)
            }

            if (currentSession.value.first == oldId) {
                currentSession.value = Pair(trimmed, mode)
            }
            return true
        }

        fun moveSession(fromIndex: Int, toIndex: Int) {
            if (fromIndex in sessionOrder.indices && toIndex in sessionOrder.indices && fromIndex != toIndex) {
                val item = sessionOrder.removeAt(fromIndex)
                sessionOrder.add(toIndex, item)
            }
        }

        fun sortSessions(ascending: Boolean = true) {
            val sorted = if (ascending) sessionOrder.sorted() else sessionOrder.sortedDescending()
            sessionOrder.clear()
            sessionOrder.addAll(sorted)
        }

        fun terminateSession(id: String) {
            sessions[id]?.apply {
                if (emulator != null) {
                    finishIfRunning()
                }
            }
            sessions.remove(id)
            sessionList.remove(id)
            sessionOrder.remove(id)
            if (sessions.isEmpty()) {
                stopSelf()
            } else {
                updateNotification()
            }
        }
    }

    private val binder = SessionBinder()
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        sessions.values.forEach { it.finishIfRunning() }
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_EXIT") {
            sessions.values.forEach { it.finishIfRunning() }
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val exitIntent = Intent(this, SessionService::class.java).apply {
            action = "ACTION_EXIT"
        }
        val exitPendingIntent = PendingIntent.getService(
            this, 1, exitIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ReTerminal")
            .setContentText(getNotificationContentText())
            .setSmallIcon(drawables.terminal)
            .setContentIntent(pendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    "EXIT",
                    exitPendingIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }

    private val CHANNEL_ID = "session_service_channel"

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Session Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for Terminal Service"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        val notification = createNotification()
        notificationManager.notify(1, notification)
    }

    private fun getNotificationContentText(): String {
        val count = sessions.size
        return if (count == 1) "1 session running" else "$count sessions running"
    }
}
