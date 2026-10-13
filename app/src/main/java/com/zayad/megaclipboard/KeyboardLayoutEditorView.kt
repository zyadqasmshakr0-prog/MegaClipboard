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
    initialItems: List<String>,
    private val onItemsChanged: ((List<String>) -> Unit)? = null
) : View(context) {

    private val items = initialItems.toMutableList()
    private val originalItems = initialItems.toList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var draggedIndex = -1
    private var hoverIndex = -1
    private var dragging = false

    private var floatingX = 0f
    private var floatingY = 0f

    private val rowCounts = listOf(12, 12, 12, 6)

    private val density: Float
        get() = resources.displayMetrics.density

    init {
        isClickable = true
        isFocusable = true
    }

    fun getItems(): List<String> = items.toList()

    fun resetItems() {
        items.clear()
        items.addAll(originalItems)
        finishDragging()
    }

    private fun indexAt(x: Float, y: Float): Int {
        if (
            width <= 0 ||
            height <= 0 ||
            items.isEmpty() ||
            x < 0f ||
            y < 0f ||
            x >= width.toFloat() ||
            y >= height.toFloat()
        ) {
            return -1
        }

        val rowHeight = height.toFloat() / rowCounts.size
        val row = floor(y / rowHeight)
            .toInt()
            .coerceIn(0, rowCounts.lastIndex)

        val keyWidth = width.toFloat() / rowCounts[row]
        val column = floor(x / keyWidth)
            .toInt()
            .coerceIn(0, rowCounts[row] - 1)

        var index = column

        for (r in 0 until row) {
            index += rowCounts[r]
        }

        return if (index in items.indices) index else -1
    }

    private fun getRowKeyWidth(index: Int): Float {
        var start = 0

        for (count in rowCounts) {
            if (index < start + count) {
                return width.toFloat() / count
            }
            start += count
        }

        return width.toFloat() / 12f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.parseColor("#121212"))

        if (width <= 0 || height <= 0) return

        val rowHeight = height.toFloat() / rowCounts.size
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 14f * resources.displayMetrics.scaledDensity

        var startIndex = 0

        for (row in rowCounts.indices) {
            val count = rowCounts[row]
            val keyWidth = width.toFloat() / count

            for (column in 0 until count) {
                val index = startIndex + column
                val text = items.getOrElse(index) { "" }

                val x = column * keyWidth
                val y = row * rowHeight

                if (dragging && index == draggedIndex) {
                    val rect = RectF(
                        x + 2f * density,
                        y + 3f * density,
                        x + keyWidth - 4f * density,
                        y + rowHeight - 6f * density
                    )

                    paint.color = Color.parseColor("#1A1A1A")
                    canvas.drawRoundRect(
                        rect,
                        8f * density,
                        8f * density,
                        paint
                    )
                } else {
                    drawButton(
                        canvas = canvas,
                        text = text,
                        x = x,
                        y = y,
                        w = keyWidth,
                        h = rowHeight,
                        isFloating = false,
                        isTarget = dragging &&
                            index == hoverIndex &&
                            index != draggedIndex
                    )
                }

                startIndex++
            }
        }

        if (dragging && draggedIndex in items.indices) {
            val keyWidth = getRowKeyWidth(draggedIndex)
            val floatingText = items[draggedIndex]

            drawButton(
                canvas = canvas,
                text = floatingText,
                x = floatingX - keyWidth / 2f,
                y = floatingY - rowHeight / 2f,
                w = keyWidth,
                h = rowHeight,
                isFloating = true,
                isTarget = false
            )
        }
    }

    private fun drawButton(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        isFloating: Boolean,
        isTarget: Boolean
    ) {
        val rect = RectF(
            x + 2f * density,
            y + 3f * density,
            x + w - 4f * density,
            y + h - 6f * density
        )

        paint.color = when {
            isFloating -> Color.parseColor("#1565C0")
            isTarget -> Color.parseColor("#2E7D32")
            else -> Color.parseColor("#333333")
        }

        if (isFloating) {
            canvas.save()
            canvas.scale(
                1.08f,
                1.08f,
                rect.centerX(),
                rect.centerY()
            )
        }

        canvas.drawRoundRect(
            rect,
            if (isFloating) 12f * density else 8f * density,
            if (isFloating) 12f * density else 8f * density,
            paint
        )

        paint.color = Color.WHITE

        val fontMetrics = paint.fontMetrics
        val textY = rect.centerY() -
            (fontMetrics.ascent + fontMetrics.descent) / 2f

        val displayText = when (text) {
            "مسافة" -> "␣"
            else -> text
        }

        canvas.drawText(
            displayText,
            rect.centerX(),
            textY,
            paint
        )

        if (isFloating) {
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                val index = indexAt(event.x, event.y)

                if (index !in items.indices) {
                    return false
                }

                parent?.requestDisallowInterceptTouchEvent(true)

                draggedIndex = index
                hoverIndex = index
                floatingX = event.x
                floatingY = event.y
                dragging = true

                performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )

                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false

                // حرّك الزر مع الإصبع في جميع الاتجاهات.
                floatingX = event.x.coerceIn(
                    0f,
                    width.toFloat()
                )

                floatingY = event.y.coerceIn(
                    0f,
                    height.toFloat()
                )

                // لا تختَر هدفًا وهميًا عند الخروج من حدود المحرر.
                val target = indexAt(event.x, event.y)

                if (target != hoverIndex) {
                    hoverIndex = target

                    if (target >= 0) {
                        performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        )
                    }
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)

                if (
                    dragging &&
                    draggedIndex in items.indices &&
                    hoverIndex in items.indices &&
                    draggedIndex != hoverIndex
                ) {
                    val temp = items[draggedIndex]
                    items[draggedIndex] = items[hoverIndex]
                    items[hoverIndex] = temp
onItemsChanged?.invoke(items.toList())
                }

                finishDragging()

                performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                finishDragging()
                return true
            }

            else -> return dragging
        }
    }

    private fun finishDragging() {
        dragging = false
        draggedIndex = -1
        hoverIndex = -1
        invalidate()
    }
}