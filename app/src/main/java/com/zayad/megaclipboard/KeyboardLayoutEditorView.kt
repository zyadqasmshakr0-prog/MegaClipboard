package com.zayad.megaclipboard
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class KeyboardLayoutEditorView(context: Context, initialItems: List<String>) : View(context) {
    private val items = initialItems.toMutableList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var draggedIndex = -1
    private var dragging = false
    private var floatingX = 0f
    private var floatingY = 0f
    private val rowCounts = listOf(12, 12, 12, 6)
    
    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isClickable = true
    }

    fun getItems(): List<String> = items.toList()

    private fun indexAt(x: Float, y: Float): Int {
        val rowHeight = height.toFloat() / 4f
        val row = (y / rowHeight).toInt().coerceIn(0, 3)
        val count = rowCounts[row]
        val keyWidth = width.toFloat() / count
        val column = (x / keyWidth).toInt().coerceIn(0, count - 1)
        var sum = 0
        for (i in 0 until row) { sum += rowCounts[i] }
        return sum + column
    }

    private fun getRowKeyWidth(index: Int): Float {
        var sum = 0
        for (i in 0..3) {
            if (index < sum + rowCounts[i]) return width.toFloat() / rowCounts[i]
            sum += rowCounts[i]
        }
        return width.toFloat() / 12
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#121212"))
        val rowHeight = height.toFloat() / 4f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 14f * resources.displayMetrics.scaledDensity

        var startIndex = 0
        for (row in 0..3) {
            val count = rowCounts[row]
            val keyWidth = width.toFloat() / count
            for (column in 0 until count) {
                val index = startIndex + column
                if (!(dragging && index == draggedIndex)) {
                    drawButton(canvas, items[index], column * keyWidth, row * rowHeight, keyWidth, rowHeight, false)
                } else {
                    paint.color = Color.parseColor("#1A1A1A")
                    val rect = RectF(column * keyWidth + 2f, row * rowHeight + 3f, column * keyWidth + keyWidth - 4f, row * rowHeight + rowHeight - 6f)
                    canvas.drawRoundRect(rect, 8f, 8f, paint)
                }
                startIndex++
            }
        }

        if (dragging && draggedIndex >= 0) {
            val w = getRowKeyWidth(draggedIndex)
            drawButton(canvas, items[draggedIndex], floatingX - w / 2, floatingY - rowHeight / 2, w, rowHeight, true)
        }
    }

    private fun drawButton(canvas: Canvas, text: String, x: Float, y: Float, w: Float, h: Float, isFloating: Boolean) {
        val rect = RectF(x + 2f, y + 3f, x + w - 4f, y + h - 6f)
        paint.color = if (isFloating) Color.parseColor("#1565C0") else Color.parseColor("#333333")
        if (isFloating) {
            canvas.save()
            canvas.scale(1.15f, 1.15f, rect.centerX(), rect.centerY())
            canvas.drawRoundRect(rect, 12f, 12f, paint)
        } else {
            canvas.drawRoundRect(rect, 8f, 8f, paint)
        }
        paint.color = Color.WHITE
        val fm = paint.fontMetrics
        val textY = rect.centerY() - (fm.ascent + fm.descent) / 2f
        var txt = text
        if (txt == "مسافة") txt = "␣"
        canvas.drawText(txt, rect.centerX(), textY, paint)
        if (isFloating) canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                draggedIndex = indexAt(event.x, event.y)
                floatingX = event.x
                floatingY = event.y
                dragging = true
                invalidate()
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    floatingX = event.x
                    floatingY = event.y
                    val target = indexAt(event.x, event.y)
                    if (target != draggedIndex && target in 0..41) {
                        val temp = items[draggedIndex]
                        items[draggedIndex] = items[target]
                        items[target] = temp
                        draggedIndex = target
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                invalidate()
            }
        }
        return true
    }
}