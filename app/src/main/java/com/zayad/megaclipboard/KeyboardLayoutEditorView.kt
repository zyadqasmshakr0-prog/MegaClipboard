package com.zayad.megaclipboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.floor

class KeyboardLayoutEditorView(
    context: Context,
    initialItems: List<String>
) : View(context) {

    companion object {
        private val ROW_COUNTS = intArrayOf(12, 12, 12, 6)
        private const val TOTAL_KEYS = 42
    }

    private val items = initialItems.toMutableList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var draggedIndex = -1
    private var hoverIndex = -1
    private var dragging = false
    private var floatingX = 0f
    private var floatingY = 0f
    private var downX = 0f
    private var downY = 0f

    init {
        isClickable = true
        isFocusable = true

        require(items.size == TOTAL_KEYS) {
            "يجب أن تحتوي لوحة المفاتيح على 42 زرًا"
        }
    }

    fun getItems(): List<String> = items.toList()

    private fun indexAt(x: Float, y: Float): Int {
        if (
            width <= 0 || height <= 0 ||
            x < 0f || y < 0f ||
            x >= width.toFloat() ||
            y >= height.toFloat()
        ) return -1

        val rowHeight = height.toFloat() / 4f
        val row = floor(y / rowHeight).toInt()
            .coerceIn(0, 3)

        val count = ROW_COUNTS[row]
        val keyWidth = width.toFloat() / count
        val column = floor(x / keyWidth).toInt()
            .coerceIn(0, count - 1)

        var index = column

        for (r in 0 until row) {
            index += ROW_COUNTS[r]
        }

        return index.takeIf { it in 0 until items.size } ?: -1
    }

    private fun keyRect(index: Int): RectF {
        var start = 0

        for (row in 0..3) {
            val count = ROW_COUNTS[row]

            if (index < start + count) {
                val column = index - start
                val keyWidth = width.toFloat() / count
                val rowHeight = height.toFloat() / 4f

                return RectF(
                    column * keyWidth + 2f,
                    row * rowHeight + 3f,
                    (column + 1) * keyWidth - 2f,
                    (row + 1) * rowHeight - 3f
                )
            }

            start += count
        }

        return RectF()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.parseColor("#121212"))

        if (width <= 0 || height <= 0) return

        paint.textAlign = Paint.Align.CENTER
        paint.textSize =
            14f * resources.displayMetrics.scaledDensity

        for (index in items.indices) {
            val rect = keyRect(index)

            if (dragging && index == draggedIndex) {
                paint.color = Color.parseColor("#1A1A1A")
                canvas.drawRoundRect(rect, 8f, 8f, paint)
            } else {
                val target =
                    dragging &&
                    index == hoverIndex &&
                    index != draggedIndex

                drawButton(
                    canvas,
                    items[index],
                    rect,
                    false,
                    target
                )
            }
        }

        if (dragging && draggedIndex in items.indices) {
            val original = keyRect(draggedIndex)
            val floating = RectF(
                floatingX - original.width() / 2f,
                floatingY - original.height() / 2f,
                floatingX + original.width() / 2f,
                floatingY + original.height() / 2f
            )

            drawButton(
                canvas,
                items[draggedIndex],
                floating,
                true,
                false
            )
        }
    }

    private fun drawButton(
        canvas: Canvas,
        text: String,
        rect: RectF,
        floating: Boolean,
        target: Boolean
    ) {
        paint.color = when {
            target -> Color.parseColor("#2E7D32")
            floating -> Color.parseColor("#1565C0")
            else -> Color.parseColor("#333333")
        }

        if (floating) {
            canvas.save()
            canvas.scale(
                1.08f, 1.08f,
                rect.centerX(), rect.centerY()
            )
        }

        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.color = Color.WHITE

        val fm = paint.fontMetrics
        val textY =
            rect.centerY() - (fm.ascent + fm.descent) / 2f

        val displayText = if (text == "مسافة") "␣" else text

        canvas.drawText(
            displayText,
            rect.centerX(),
            textY,
            paint
        )

        if (floating) canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val index = indexAt(event.x, event.y)

                if (index !in items.indices) return false

                parent?.requestDisallowInterceptTouchEvent(true)

                downX = event.x
                downY = event.y
                floatingX = event.x
                floatingY = event.y
                draggedIndex = index
                hoverIndex = index
                dragging = true

                performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS
                )

                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false

                floatingX = event.x
                floatingY = event.y

                val target = indexAt(event.x, event.y)

                if (target != hoverIndex) {
                    hoverIndex = target

                    if (target >= 0) {
                        performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP
                        )
                    }
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!dragging) return false

                val target = indexAt(event.x, event.y)

                if (
                    draggedIndex in items.indices &&
                    target in items.indices &&
                    draggedIndex != target
                ) {
                    val temp = items[draggedIndex]
                    items[draggedIndex] = items[target]
                    items[target] = temp

                    performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP
                    )
                }

                finishDrag()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                finishDrag()
                return true
            }
        }

        return true
    }

    private fun finishDrag() {
        dragging = false
        draggedIndex = -1
        hoverIndex = -1

        parent?.requestDisallowInterceptTouchEvent(false)
        invalidate()
    }
}