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
    private var hoverIndex = -1
    private var dragging = false
    private var floatingX = 0f
    private var floatingY = 0f
    private val rowCounts = listOf(12, 12, 12, 6)
    
    init { isClickable = true }
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
                val textToDraw = items.getOrElse(index) { "?" }
                
                if (dragging && index == draggedIndex) {
                    // ترك مكان الزر الأصلي فارغاً باللون الأسود
                    paint.color = Color.parseColor("#1A1A1A")
                    val rect = RectF(column * keyWidth + 2f, row * rowHeight + 3f, column * keyWidth + keyWidth - 4f, row * rowHeight + rowHeight - 6f)
                    canvas.drawRoundRect(rect, 8f, 8f, paint)
                } else {
                    // تظليل المكان المستهدف باللون الأخضر
                    val isTarget = (dragging && index == hoverIndex)
                    drawButton(canvas, textToDraw, column * keyWidth, row * rowHeight, keyWidth, rowHeight, false, isTarget)
                }
                startIndex++
            }
        }

        if (dragging && draggedIndex >= 0) {
            val w = getRowKeyWidth(draggedIndex)
            val floatText = items.getOrElse(draggedIndex) { "?" }
            drawButton(canvas, floatText, floatingX - w / 2, floatingY - rowHeight / 2, w, rowHeight, true, false)
        }
    }

    private fun drawButton(canvas: Canvas, text: String, x: Float, y: Float, w: Float, h: Float, isFloating: Boolean, isTarget: Boolean) {
        val rect = RectF(x + 2f, y + 3f, x + w - 4f, y + h - 6f)
        
        if (isTarget) {
            paint.color = Color.parseColor("#2E7D32") // لون أخضر لمكان السقوط
        } else {
            paint.color = if (isFloating) Color.parseColor("#1565C0") else Color.parseColor("#333333")
        }
        
        if (isFloating) {
            canvas.save()
            canvas.scale(1.2f, 1.2f, rect.centerX(), rect.centerY())
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
                parent?.requestDisallowInterceptTouchEvent(true)
                draggedIndex = indexAt(event.x, event.y)
                hoverIndex = draggedIndex
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
                    if (target != hoverIndex && target in 0 until items.size) {
                        hoverIndex = target
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                // التبديل الحقيقي يحدث فقط عندما ترفع إصبعك!
                if (dragging && draggedIndex >= 0 && hoverIndex >= 0 && draggedIndex != hoverIndex) {
                    val temp = items[draggedIndex]
                    items[draggedIndex] = items[hoverIndex]
                    items[hoverIndex] = temp
                }
                dragging = false
                draggedIndex = -1
                hoverIndex = -1
                invalidate()
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            }
        }
        return true
    }
}