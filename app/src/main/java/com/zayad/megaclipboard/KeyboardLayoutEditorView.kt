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
    private var lastSwapIndex = -1
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
                val left = column * keyWidth + 2f
                val top = row * rowHeight + 3f
                val right = left + keyWidth - 4f
                val bottom = top + rowHeight - 6f
                val rect = RectF(left, top, right, bottom)
                paint.color = if (index == draggedIndex) Color.parseColor("#1565C0") else Color.parseColor("#333333")
                canvas.drawRoundRect(rect, 8f, 8f, paint)
                paint.color = Color.WHITE
                val fm = paint.fontMetrics
                val textY = rect.centerY() - (fm.ascent + fm.descent) / 2f
                var txt = items[index]
                if (txt == "مسافة") txt = "␣"
                canvas.drawText(txt, rect.centerX(), textY, paint)
            }
            startIndex += count
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                draggedIndex = indexAt(event.x, event.y)
                lastSwapIndex = draggedIndex
                invalidate()
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggedIndex >= 0) {
                    val target = indexAt(event.x, event.y)
                    if (target != draggedIndex && target != lastSwapIndex) {
                        val temp = items[draggedIndex]
                        items[draggedIndex] = items[target]
                        items[target] = temp
                        draggedIndex = target
                        lastSwapIndex = target
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggedIndex = -1
                lastSwapIndex = -1
                invalidate()
            }
        }
        return true
    }
}