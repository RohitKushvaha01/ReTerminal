package com.rk.terminal.ui.screens.terminal

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doOnTextChanged
import com.rk.libcommons.child
import com.rk.libcommons.dpToPx
import com.rk.libcommons.localDir
import com.rk.settings.Settings
import com.rk.terminal.service.SessionService
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.terminal.virtualkeys.*
import com.termux.terminal.TerminalColors
import com.termux.view.TerminalView
import java.io.FileInputStream
import java.util.*

@Composable
fun TerminalViewLayout(
    viewModel: TerminalViewModel,
    mainActivity: MainActivity,
    sessionBinder: SessionService.SessionBinder,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                TerminalView(ctx, null).apply {
                    viewModel.setTerminalView(this)
                    setTextSize(dpToPx(Settings.terminal_font_size.toFloat(), ctx))
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    
                    val client = TerminalBackEnd(this, mainActivity)
                    val service = sessionBinder.getService()
                    
                    val session = sessionBinder.getSession(service.currentSession.value.first)
                        ?: sessionBinder.createSession(
                            service.currentSession.value.first,
                            client,
                            Settings.working_Mode
                        )

                    session.updateTerminalSessionClient(client)
                    attachSession(session)
                    setTerminalViewClient(client)
                    setTypeface(TerminalUtils.typeface)

                    post {
                        val color = TerminalUtils.getViewColor()
                        val bgColor = TerminalUtils.getBackgroundColor()
                        keepScreenOn = true
                        requestFocus()
                        isFocusableInTouchMode = true

                        mEmulator?.mColors?.mCurrentColors?.apply {
                            set(256, color)
                            set(257, bgColor)
                            set(258, color)
                        }

                        val colorsFile = ctx.localDir().child("colors.properties")
                        if (colorsFile.exists() && colorsFile.isFile) {
                            val props = Properties()
                            FileInputStream(colorsFile).use { props.load(it) }
                            TerminalColors.COLOR_SCHEME.updateWith(props)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
            update = { view ->
                view.onScreenUpdated()
                val color = TerminalUtils.getViewColor()
                val bgColor = TerminalUtils.getBackgroundColor()
                view.mEmulator?.mColors?.mCurrentColors?.apply {
                    set(256, color)
                    set(257, bgColor)
                    set(258, color)
                }
            }
        )

        if (viewModel.showVirtualKeys) {
            VirtualKeysPager(viewModel)
        }
    }
}

@Composable
private fun VirtualKeysPager(viewModel: TerminalViewModel) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(75.dp)
    ) { page ->
        when (page) {
            0 -> {
                AndroidView(
                    factory = { ctx ->
                        VirtualKeysView(ctx, null).apply {
                            viewModel.setVirtualKeysView(this)
                            virtualKeysViewClient = viewModel.terminalView?.mTermSession?.let {
                                VirtualKeysListener(it)
                            }
                            buttonTextColor = onSurfaceColor
                            reload(VirtualKeysInfo(VIRTUAL_KEYS, "", VirtualKeysConstants.CONTROL_CHARS_ALIASES))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(75.dp)
                )
            }
            1 -> {
                var text by rememberSaveable { mutableStateOf("") }
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(75.dp),
                    factory = { ctx ->
                        EditText(ctx).apply {
                            maxLines = 1
                            isSingleLine = true
                            imeOptions = EditorInfo.IME_ACTION_DONE
                            doOnTextChanged { t, _, _, _ -> text = t.toString() }
                            setOnEditorActionListener { _, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                    val terminal = viewModel.terminalView
                                    if (text.isEmpty()) {
                                        terminal?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                                        terminal?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                                    } else {
                                        terminal?.currentSession?.write(text)
                                        setText("")
                                    }
                                    true
                                } else false
                            }
                        }
                    },
                    update = { editText ->
                        if (editText.text.toString() != text) {
                            editText.setText(text)
                            editText.setSelection(text.length)
                        }
                    }
                )
            }
        }
    }
}
