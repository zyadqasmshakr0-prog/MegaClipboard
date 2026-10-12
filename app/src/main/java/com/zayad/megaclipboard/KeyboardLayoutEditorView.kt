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

    init {
        isClickable = true
        isFocusable = true
    }

    fun getItems(): List<String> = items.toList()

    fun resetItems() {
        items.clear()
        items.addAll(originalItems)
        dragging = false
        draggedIndex = -1
        hoverIndex = -1
        invalidate()
    }

    private fun indexAt(x: Float, y: Float): Int {
        if (width <= 0 || height <= 0 || items.isEmpty()) return -1

        val safeY = y.coerceIn(0f, height.toFloat() - 0.01f)
        val rowHeight = height.toFloat() / rowCounts.size
        val row = floor(safeY / rowHeight).toInt().coerceIn(0, rowCounts.lastIndex)

        val count = rowCounts[row]
        val keyWidth = width.toFloat() / count
        val safeX = x.coerceIn(0f, width.toFloat() - 0.01f)
        val column = floor(safeX / keyWidth).toInt().coerceIn(0, count - 1)

        var sum = 0
        for (i in 0 until row) {
            sum += rowCounts[i]
        }

        val index = sum + column
        return if (index in items.indices) index else -1
    }

    private fun getRowKeyWidth(index: Int): Float {
        var sum = 0

        for (i in rowCounts.indices) {
            if (index < sum + rowCounts[i]) {
                return width.toFloat() / rowCounts[i]
            }
            sum += rowCounts[i]
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
                val textToDraw = items.getOrElse(index) { "" }

                if (dragging && index == draggedIndex) {
                    paint.color = Color.parseColor("#1A1A1A")

                    val rect = RectF(
                        column * keyWidth + 2f,
                        row * rowHeight + 3f,
                        column * keyWidth + keyWidth - 4f,
                        row * rowHeight + rowHeight - 6f
                    )

                    canvas.drawRoundRect(rect, 8f, 8f, paint)
                } else {
                    val isTarget =
                        dragging && index == hoverIndex && index != draggedIndex

                    drawButton(
                        canvas,
                        textToDraw,
                        column * keyWidth,
                        row * rowHeight,
                        keyWidth,
                        rowHeight,
                        false,
                        isTarget
                    )
                }

                startIndex++
            }
        }

        if (dragging && draggedIndex in items.indices) {
            val keyWidth = getRowKeyWidth(draggedIndex)
            val floatingText = items[draggedIndex]

            drawButton(
                canvas,
                floatingText,
                floatingX - keyWidth / 2f,
                floatingY - rowHeight / 2f,
                keyWidth,
                rowHeight,
                true,
                false
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
            x + 2f,
            y + 3f,
            x + w - 4f,
            y + h - 6f
        )

        paint.color = when {
            isTarget -> Color.parseColor("#2E7D32")
            isFloating -> Color.parseColor("#1565C0")
            else -> Color.parseColor("#333333")
        }

        if (isFloating) {
            canvas.save()
            canvas.scale(1.08f, 1.08f, rect.centerX(), rect.centerY())
        }

        canvas.drawRoundRect(
            rect,
            if (isFloating) 12f else 8f,
            if (isFloating) 12f else 8f,
            paint
        )

        paint.color = Color.WHITE
        val fontMetrics = paint.fontMetrics
        val textY = rect.centerY() -
            (fontMetrics.ascent + fontMetrics.descent) / 2f

        val displayText = if (text == "مسافة") "␣" else text
        canvas.drawText(displayText, rect.centerX(), textY, paint)

        if (isFloating) {
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val index = indexAt(event.x, event.y)
                if (index !in items.indices) return false

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

                floatingX = event.x.coerceIn(0f, width.toFloat())
                floatingY = event.y.coerceIn(0f, height.toFloat())

                val target = indexAt(event.x, event.y)

                if (target in items.indices && target != hoverIndex) {
                    hoverIndex = target

                    performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
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
                    val movedItem = items.removeAt(draggedIndex)
                    items.add(hoverIndex, movedItem)

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
        }

        return true
    }

    private fun finishDragging() {
        dragging = false
        draggedIndex = -1
        hoverIndex = -1
        invalidate()
    }
}