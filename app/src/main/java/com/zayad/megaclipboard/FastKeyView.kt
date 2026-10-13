package com.zayad.megaclipboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.HapticFeedbackConstants
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class FastKeyView(context: Context) : View(context) {

    var text: CharSequence = ""
        set(value) {
            field = value
            invalidate()
        }

    var textSize: Float = 18f
        set(value) {
            field = value
            paint.textSize = value * resources.displayMetrics.scaledDensity
            invalidate()
        }

    var textColor: Int = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    var keyColor: Int = Color.rgb(45, 45, 45)
        set(value) {
            field = value
            invalidate()
        }

    var pressedKeyColor: Int = Color.rgb(70, 70, 70)
    var cornerRadius: Float = 10f
    var enableKeyHaptic: Boolean = true

    var longPressOptions: List<String> = emptyList()
    var onLongPressOptionSelected: ((String) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 18f * resources.displayMetrics.scaledDensity
        color = Color.WHITE
        typeface = android.graphics.Typeface.create(
            "sans-serif",
            android.graphics.Typeface.NORMAL
        )
    }

    private val rect = RectF()
    private var pressed = false
    private var longPressTriggered = false
    private var selectedOption = -1

    private var previewWindow: PopupWindow? = null
    private var optionsWindow: PopupWindow? = null
    private var optionViews: List<TextView> = emptyList()

    private var optionsScreenLeft = 0
    private var optionsScreenTop = 0
    private var optionWidth = 1
    private var optionsHeight = 0

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private val longPressRunnable = Runnable {
        if (pressed && longPressOptions.isNotEmpty()) {
            longPressTriggered = true
            selectedOption = -1
            dismissPreview()

            if (enableKeyHaptic) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }

            showOptionsPopup()
        }
    }

    init {
        isClickable = true
        isFocusable = false
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        rect.set(
            1f,
            1f,
            width.toFloat() - 1f,
            height.toFloat() - 1f
        )

        paint.style = Paint.Style.FILL
        paint.color = if (pressed) pressedKeyColor else keyColor

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        paint.color = textColor
        paint.textSize = textSize * resources.displayMetrics.scaledDensity

        val fm = paint.fontMetrics
        val y = height / 2f - (fm.ascent + fm.descent) / 2f

        canvas.drawText(text.toString(), width / 2f, y, paint)
    }

    private fun screenPosition(): IntArray {
        return IntArray(2).also { getLocationOnScreen(it) }
    }

    private fun showAtScreenPosition(
        popup: PopupWindow,
        screenX: Int,
        screenY: Int
    ) {
        val parent = rootView ?: this
        val parentPosition = IntArray(2)
        parent.getLocationOnScreen(parentPosition)

        popup.showAtLocation(
            parent,
            Gravity.TOP or Gravity.START,
            screenX - parentPosition[0],
            screenY - parentPosition[1]
        )
    }

    // نافذة صغيرة تظهر مباشرة عند لمس المفتاح.
    private fun showPreview() {
        dismissPreview()

        val label = TextView(context).apply {
            text = this@FastKeyView.text
            gravity = Gravity.CENTER
            textSize = 27f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#242424"))
            includeFontPadding = false
        }

        val w = dp(54)
        val h = dp(48)

        previewWindow = PopupWindow(label, w, h, false).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isTouchable = false
            isFocusable = false
            elevation = dp(5).toFloat()
        }

        val pos = screenPosition()
        val screenWidth = resources.displayMetrics.widthPixels
        val x = (pos[0] + width / 2 - w / 2)
            .coerceIn(0, max(0, screenWidth - w))

        // فوق المفتاح بقليل، وليس أسفل الشاشة.
        val y = pos[1] - h - dp(3)

        try {
            showAtScreenPosition(previewWindow!!, x, y)
        } catch (_: Exception) {
            dismissPreview()
        }
    }

    private fun dismissPreview() {
        try {
            previewWindow?.dismiss()
        } catch (_: Exception) {
        }
        previewWindow = null
    }

    private fun showOptionsPopup() {
        dismissOptionsPopup()
        if (longPressOptions.isEmpty()) return

        val screenWidth = resources.displayMetrics.widthPixels
        val availableWidth = max(dp(1), screenWidth - dp(12))

        optionWidth = min(
            dp(46),
            max(dp(34), availableWidth / longPressOptions.size)
        )

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#242424"))
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }

        val cells = mutableListOf<TextView>()

        longPressOptions.forEachIndexed { index, option ->
            val cell = TextView(context).apply {
                text = option
                gravity = Gravity.CENTER
                textSize = 19f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    optionWidth,
                    dp(48)
                ).apply {
                    setMargins(dp(1), 0, dp(1), 0)
                }
                includeFontPadding = false
            }

            row.addView(cell)
            cells.add(cell)
        }

        optionViews = cells
        optionsHeight = dp(52)

        val popupWidth = min(
            availableWidth,
            optionWidth * longPressOptions.size + dp(8)
        )

        optionsWindow = PopupWindow(
            row,
            popupWidth,
            optionsHeight,
            false
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = false
            isTouchable = false
            elevation = dp(8).toFloat()
        }

        val pos = screenPosition()
        optionsScreenLeft = (
            pos[0] + width / 2 - popupWidth / 2
        ).coerceIn(0, max(0, screenWidth - popupWidth))

        // ضع قائمة البدائل فوق المفتاح الحالي.
        // في الصف الثاني تظهر قرب الصف الأول، وفي الصف الأول فوقه قليلًا.
        optionsScreenTop = pos[1] - optionsHeight - dp(4)

        try {
            showAtScreenPosition(
                optionsWindow!!,
                optionsScreenLeft,
                optionsScreenTop
            )
        } catch (_: Exception) {
            dismissOptionsPopup()
        }
    }

    private fun updateSelectedOption(rawX: Float, rawY: Float) {
        val inPopupY =
            rawY >= optionsScreenTop &&
            rawY <= optionsScreenTop + optionsHeight

        val index = if (inPopupY) {
            floor((rawX - optionsScreenLeft) / optionWidth).toInt()
        } else {
            -1
        }

        selectedOption =
            if (index in longPressOptions.indices) index else -1

        optionViews.forEachIndexed { i, view ->
            view.setBackgroundColor(
                if (i == selectedOption) {
                    Color.parseColor("#1976D2")
                } else {
                    Color.parseColor("#333333")
                }
            )
        }
    }

    private fun dismissOptionsPopup() {
        try {
            optionsWindow?.dismiss()
        } catch (_: Exception) {
        }
        optionsWindow = null
        optionViews = emptyList()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressed = true
                longPressTriggered = false
                selectedOption = -1
                invalidate()

                if (enableKeyHaptic) {
                    performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                }

                showPreview()

                if (longPressOptions.isNotEmpty()) {
                    postDelayed(
                        longPressRunnable,
                        ViewConfiguration.getLongPressTimeout().toLong()
                    )
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (longPressTriggered) {
                    updateSelectedOption(event.rawX, event.rawY)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                removeCallbacks(longPressRunnable)

                val wasLongPress = longPressTriggered
                val chosen = selectedOption

                pressed = false
                invalidate()
                dismissPreview()

                if (wasLongPress) {
                    dismissOptionsPopup()

                    if (chosen in longPressOptions.indices) {
                        onLongPressOptionSelected?.invoke(
                            longPressOptions[chosen]
                        )
                    }

                    longPressTriggered = false
                    selectedOption = -1
                    return true
                }

                dismissOptionsPopup()
                longPressTriggered = false
                selectedOption = -1
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                pressed = false
                longPressTriggered = false
                selectedOption = -1
                dismissPreview()
                dismissOptionsPopup()
                invalidate()
                return true
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPressRunnable)
        dismissPreview()
        dismissOptionsPopup()
        super.onDetachedFromWindow()
    }
}