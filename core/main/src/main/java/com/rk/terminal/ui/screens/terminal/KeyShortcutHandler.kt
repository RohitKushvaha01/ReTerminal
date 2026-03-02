package com.rk.terminal.ui.screens.terminal

import android.view.KeyEvent
import com.blankj.utilcode.util.ClipboardUtils
import com.rk.settings.Settings
import com.rk.terminal.ui.activities.terminal.MainActivity

/**
 * Centralized keyboard shortcut handler for the terminal.
 * Reads configurable bindings from Settings and dispatches actions.
 */
object KeyShortcutHandler {

    /**
     * Handle a key event. Returns true if the key was consumed by a shortcut.
     */
    fun handle(keyCode: Int, event: KeyEvent, activity: MainActivity): Boolean {
        if (!Settings.shortcuts_enabled) return false

        // Try each action's binding
        for (action in ShortcutAction.entries) {
            val binding = Settings.getShortcutBinding(action)
            if (binding.matches(event)) {
                return dispatch(action, activity)
            }
        }
        return false
    }

    private fun dispatch(action: ShortcutAction, activity: MainActivity): Boolean {
        return when (action) {
            ShortcutAction.PASTE -> handlePaste()
            ShortcutAction.NEW_SESSION -> handleNewSession(activity)
            ShortcutAction.CLOSE_SESSION -> handleCloseSession(activity)
            ShortcutAction.SWITCH_SESSION_PREV -> handleSwitchSession(activity, forward = false)
            ShortcutAction.SWITCH_SESSION_NEXT -> handleSwitchSession(activity, forward = true)
        }
    }

    private fun handlePaste(): Boolean {
        val clip = ClipboardUtils.getText()?.toString() ?: return true
        if (clip.trim().isNotEmpty()) {
            terminalView.get()?.mEmulator?.paste(clip)
        }
        return true
    }

    private fun handleNewSession(activity: MainActivity): Boolean {
        val binder = activity.sessionBinder ?: return true
        val service = binder.getService()

        val sessionId = generateUniqueSessionId(service.sessionList.keys.toList())
        terminalView.get()?.let {
            val client = TerminalBackEnd(it, activity)
            binder.createSession(sessionId, client, activity, workingMode = Settings.working_Mode)
        }
        changeSession(activity, session_id = sessionId)
        return true
    }

    private fun handleCloseSession(activity: MainActivity): Boolean {
        val binder = activity.sessionBinder ?: return true
        val service = binder.getService()
        val currentId = service.currentSession.value.first
        val sessionKeys = service.sessionList.keys.toList()

        if (sessionKeys.size <= 1) {
            binder.terminateSession(currentId)
            if (service.sessionList.isEmpty()) {
                activity.finish()
            }
        } else {
            val currentIndex = sessionKeys.indexOf(currentId)
            val nextId = if (currentIndex < sessionKeys.size - 1) {
                sessionKeys[currentIndex + 1]
            } else {
                sessionKeys[currentIndex - 1]
            }
            changeSession(activity, session_id = nextId)
            binder.terminateSession(currentId)
        }
        return true
    }

    private fun handleSwitchSession(activity: MainActivity, forward: Boolean): Boolean {
        val binder = activity.sessionBinder ?: return true
        val service = binder.getService()
        val sessionKeys = service.sessionList.keys.toList()

        if (sessionKeys.size <= 1) return true

        val currentId = service.currentSession.value.first
        val currentIndex = sessionKeys.indexOf(currentId)

        val nextIndex = if (forward) {
            (currentIndex + 1) % sessionKeys.size
        } else {
            (currentIndex - 1 + sessionKeys.size) % sessionKeys.size
        }

        changeSession(activity, session_id = sessionKeys[nextIndex])
        return true
    }

    private fun generateUniqueSessionId(existingIds: List<String>): String {
        var index = 1
        var newId: String
        do {
            newId = "main$index"
            index++
        } while (newId in existingIds)
        return newId
    }
}
