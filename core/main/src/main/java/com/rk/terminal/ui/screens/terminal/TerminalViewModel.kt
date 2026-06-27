package com.rk.terminal.ui.screens.terminal

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import com.google.android.material.R
import com.rk.settings.Settings
import com.rk.terminal.service.SessionService
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysListener
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysView
import com.termux.view.TerminalView
import java.lang.ref.WeakReference

class TerminalViewModel : ViewModel() {
    private var terminalViewRef = WeakReference<TerminalView>(null)
    private var virtualKeysViewRef = WeakReference<VirtualKeysView>(null)

    val terminalView: TerminalView? get() = terminalViewRef.get()
    val virtualKeysView: VirtualKeysView? get() = virtualKeysViewRef.get()

    fun setTerminalView(view: TerminalView?) { terminalViewRef = WeakReference(view) }
    fun setVirtualKeysView(view: VirtualKeysView?) { virtualKeysViewRef = WeakReference(view) }

    var bitmap by mutableStateOf<ImageBitmap?>(null)
    var wallAlpha by mutableFloatStateOf(Settings.wallTransparency)
    var backgroundBlur by mutableFloatStateOf(Settings.background_blur)

    var showToolbar by mutableStateOf(Settings.toolbar)
    var showVirtualKeys by mutableStateOf(Settings.virtualKeys)
    var showHorizontalToolbar by mutableStateOf(Settings.toolbar)

    fun setFont(typeface: Typeface) {
        TerminalUtils.typeface = typeface
        terminalView?.apply {
            setTypeface(typeface)
            onScreenUpdated()
        }
    }

    fun changeSession(context: Context, sessionBinder: SessionService.SessionBinder, sessionId: String) {
        val terminal = terminalView ?: return
        val activity = context as? MainActivity ?: return
        val client = TerminalBackEnd(terminal, activity)
        
        val session = sessionBinder.getSession(sessionId)
            ?: sessionBinder.createSession(sessionId, client, Settings.working_Mode)
            
        session.updateTerminalSessionClient(client)
        terminal.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        terminal.attachSession(session)
        terminal.setTerminalViewClient(client)
        
        terminal.post {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
            terminal.keepScreenOn = true
            terminal.requestFocus()
            terminal.isFocusableInTouchMode = true

            terminal.mEmulator?.mColors?.mCurrentColors?.apply {
                set(256, typedValue.data)
                set(257, TerminalUtils.getBackgroundColor())
                set(258, typedValue.data)
            }
        }
        
        virtualKeysView?.apply {
            virtualKeysViewClient = terminal.mTermSession?.let { VirtualKeysListener(it) }
        }
        
        sessionBinder.getService().currentSession.value = Pair(sessionId, sessionBinder.getService().sessionList[sessionId]!!)
    }
}
