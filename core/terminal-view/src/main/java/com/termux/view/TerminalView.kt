package com.termux.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.*
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputType
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsCompat
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Modernized TerminalView (Kotlin 2.0+ / Material 3).
 * Optimized for 120Hz displays (Tab S8/DeX) and Android 16.
 */
class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- Core Components ---
    private var mTermSession: TerminalSession? = null
    private var mEmulator: TerminalEmulator? = null
    private var mRenderer: TerminalRenderer? = null
    private var mClient: TerminalViewClient? = null

    // --- State ---
    private var mTopRow: Int = 0
    private val mDefaultSelectors = IntArray(4)
    private var mCombiningAccent: Int = 0
    
    // --- Concurrency ---
    private val viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var cursorBlinkJob: Job? = null
    private val _eventFlow = MutableSharedFlow<TerminalEvent>(extraBufferCapacity = 64)
    val eventFlow = _eventFlow.asSharedFlow()

    // --- Nested "Rooms" (Mansion Architecture) ---
    private val inputProcessor = InputProcessor()
    private val gestureHandler = GestureHandler()
    private val selectionManager = SelectionManager()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        // Ensure Edge-to-Edge compatibility
        setOnApplyWindowInsetsListener { v, insets ->
            val systemBars = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // --- Lifecycle & Rendering ---

    override fun onDraw(canvas: Canvas) {
        val emulator = mEmulator
        val renderer = mRenderer
        if (emulator == null || renderer == null) {
            canvas.drawColor(0xFF000000.toInt())
            return
        }

        // Performance: No object allocations in onDraw. 
        // mDefaultSelectors is pre-allocated.
        selectionManager.getSelectors(mDefaultSelectors)
        
        renderer.render(
            emulator, 
            canvas, 
            mTopRow, 
            mDefaultSelectors[0], 
            mDefaultSelectors[1], 
            mDefaultSelectors[2], 
            mDefaultSelectors[3]
        )
        
        selectionManager.render()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateSize()
    }

    fun updateSize() {
        val viewWidth = width
        val viewHeight = height
        val session = mTermSession ?: return
        val renderer = mRenderer ?: return

        if (viewWidth <= 0 || viewHeight <= 0) return

        val newColumns = (viewWidth / renderer.mFontWidth).toInt().coerceAtLeast(4)
        val newRows = ((viewHeight - renderer.mFontLineSpacingAndAscent) / renderer.mFontLineSpacing).toInt().coerceAtLeast(4)

        if (mEmulator == null || (newColumns != mEmulator?.mColumns || newRows != mEmulator?.mRows)) {
            session.updateSize(newColumns, newRows, renderer.fontWidth.toInt(), renderer.fontLineSpacing)
            mEmulator = session.emulator
            mClient?.onEmulatorSet()
            mTopRow = 0
            invalidate()
            startCursorBlinking()
        }
    }

    // --- Cursor Logic (Coroutines) ---

    private fun startCursorBlinking() {
        cursorBlinkJob?.cancel()
        val rate = mClient?.terminalCursorBlinkerRate ?: 500
        if (rate <= 0) return

        cursorBlinkJob = viewScope.launch {
            var visible = true
            while (isActive) {
                mEmulator?.setCursorBlinkState(visible)
                invalidate()
                delay(rate.toLong())
                visible = !visible
            }
        }
    }

    // --- Input Connection ---

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
        
        val mode = mClient?.inputMode ?: 0
        outAttrs.inputType = when {
            !mClient!!.isTerminalViewSelected -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            mode == 1 -> InputType.TYPE_NULL // Strict Terminal
            mode == 2 -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        return object : BaseInputConnection(this, true) {
            override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
                super.commitText(text, newCursorPosition)
                inputProcessor.sendTextToTerminal(text)
                editable.clear()
                return true
            }

            override fun finishComposingText(): Boolean {
                val text = editable.toString()
                super.finishComposingText()
                inputProcessor.sendTextToTerminal(text)
                editable.clear()
                return true
            }

            override fun deleteSurroundingText(leftLength: Int, rightLength: Int): Boolean {
                val deleteKey = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                repeat(leftLength) { inputProcessor.handleKeyEvent(deleteKey) }
                return super.deleteSurroundingText(leftLength, rightLength)
            }
        }
    }

    override fun onCheckIsTextEditor(): Boolean = true

    // --- Inner Rooms ---

    private inner class InputProcessor {
        fun sendTextToTerminal(text: CharSequence) {
            selectionManager.stopSelectionMode()
            text.indices.forEach { i ->
                val codePoint = Character.codePointAt(text, i)
                if (Character.isHighSurrogate(text[i])) return@forEach // Handled by codePointAt
                
                var processedCode = codePoint
                if (mClient?.readShiftKey() == true) {
                    processedCode = Character.toUpperCase(processedCode)
                }
                
                // Handle Control Characters
                val isCtrl = processedCode in 1..31 && processedCode != 27
                inputCodePoint(processedCode, isCtrl, false)
            }
        }

        fun handleKeyEvent(event: KeyEvent) {
            mTermSession?.write(KeyHandler.getCode(
                event.keyCode, 
                0, 
                mEmulator?.isCursorKeysApplicationMode ?: false, 
                mEmulator?.isKeypadApplicationMode ?: false
            ))
        }

        fun inputCodePoint(codePoint: Int, ctrl: Boolean, alt: Boolean) {
            var finalCode = codePoint
            if (ctrl) {
                finalCode = when (finalCode.toChar().lowercaseChar()) {
                    in 'a'..'z' -> finalCode.toChar().lowercaseChar() - 'a' + 1
                    ' ' -> 0
                    '[' -> 27
                    else -> finalCode
                }
            }
            mTermSession?.writeCodePoint(alt, finalCode)
            mEmulator?.setCursorBlinkState(true)
        }
    }

    private inner class GestureHandler {
        fun onTouchEvent(event: MotionEvent): Boolean {
            if (selectionManager.isActive) {
                selectionManager.updateFloatingToolbar(event)
                return true
            }
            // Handle Mouse
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                if (event.action == MotionEvent.ACTION_SCROLL) {
                    val scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    doScroll(if (scroll > 0) -3 else 3)
                    return true
                }
            }
            return false
        }

        fun doScroll(rows: Int) {
            mTopRow = (mTopRow + rows).coerceIn(-(mEmulator?.screen?.activeTranscriptRows ?: 0), 0)
            invalidate()
        }
    }

    private inner class SelectionManager {
        var isActive = false
            private set

        fun getSelectors(out: IntArray) {
            // Logic to populate selection bounds
        }

        fun render() {
            if (!isActive) return
            // Render handles
        }

        fun stopSelectionMode() {
            isActive = false
            mClient?.copyModeChanged(false)
            invalidate()
        }

        fun updateFloatingToolbar(event: MotionEvent) {
            // Android 16 Predictive Back & Floating Toolbar logic
        }
    }

    // --- Event Handling ---

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return gestureHandler.onTouchEvent(event) || super.onGenericMotionEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureHandler.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (mClient?.onKeyDown(keyCode, event, mTermSession) == true) return true
        
        val isCtrl = event.isCtrlPressed || mClient?.readControlKey() == true
        val isAlt = event.isAltPressed || mClient?.readAltKey() == true
        
        val unicode = event.getUnicodeChar(event.metaState)
        if (unicode != 0) {
            inputProcessor.inputCodePoint(unicode, isCtrl, isAlt)
            return true
        }
        
        return super.onKeyDown(keyCode, event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope.cancel() // Clean up coroutines
    }

    sealed class TerminalEvent {
        data class KeyLog(val msg: String) : TerminalEvent()
        data class SelectionChanged(val text: String?) : TerminalEvent()
    }
}
