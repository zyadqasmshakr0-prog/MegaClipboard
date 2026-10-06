package com.zayad.megaclipboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.HapticFeedbackConstants

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
            paint.color = value
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

        canvas.drawRoundRect(
            rect,
            cornerRadius,
            cornerRadius,
            paint
        )

        paint.color = textColor
        paint.textSize = textSize * resources.displayMetrics.scaledDensity

        val fm = paint.fontMetrics

        val y =
            height / 2f -
            (fm.ascent + fm.descent) / 2f

        canvas.drawText(
            text.toString(),
            width / 2f,
            y,
            paint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                pressed = true
                invalidate()

                if (enableKeyHaptic) {
                    performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                }

                // تنفيذ الضغط فورًا عند لمس المفتاح
                performClick()

                return true
            }

            MotionEvent.ACTION_UP -> {

                pressed = false
                invalidate()

                return true
            }

            MotionEvent.ACTION_CANCEL -> {

                pressed = false
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
}
