package com.zayad.megaclipboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class KeyboardLayoutEditorView(
    context: Context,
    initialItems: List<String>
) : View(context) {

    private val items =
        initialItems.toMutableList()

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private var draggedIndex = -1
    private var lastSwapIndex = -1

    private val rows =
        listOf(11, 11, 11)

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isClickable = true
    }

    fun getItems(): List<String> {
        return items.toList()
    }

    private fun rowForIndex(index: Int): Int {
        return when {
            index < 11 -> 0
            index < 22 -> 1
            else -> 2
        }
    }

    private fun indexAt(x: Float, y: Float): Int {

        val rowHeight =
            height.toFloat() / 3f

        val row =
            (y / rowHeight)
                .toInt()
                .coerceIn(0, 2)

        val count = rows[row]

        val keyWidth =
            width.toFloat() / count

        val column =
            (x / keyWidth)
                .toInt()
                .coerceIn(0, count - 1)

        val start =
            when (row) {
                0 -> 0
                1 -> 11
                else -> 22
            }

        return start + column
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        canvas.drawColor(
            Color.parseColor("#121212")
        )

        val rowHeight =
            height.toFloat() / 3f

        paint.textAlign =
            Paint.Align.CENTER

        paint.textSize =
            18f * resources.displayMetrics.scaledDensity

        for (row in 0..2) {

            val count = rows[row]

            val start =
                when (row) {
                    0 -> 0
                    1 -> 11
                    else -> 22
                }

            val keyWidth =
                width.toFloat() / count

            for (column in 0 until count) {

                val index =
                    start + column

                val left =
                    column * keyWidth + 3f

                val top =
                    row * rowHeight + 4f

                val right =
                    left + keyWidth - 6f

                val bottom =
                    top + rowHeight - 8f

                val rect =
                    RectF(
                        left,
                        top,
                        right,
                        bottom
                    )

                paint.color =
                    if (index == draggedIndex)
                        Color.parseColor("#1565C0")
                    else
                        Color.parseColor("#333333")

                canvas.drawRoundRect(
                    rect,
                    10f,
                    10f,
                    paint
                )

                paint.color =
                    Color.WHITE

                val fm =
                    paint.fontMetrics

                val textY =
                    rect.centerY() -
                    (fm.ascent + fm.descent) / 2f

                canvas.drawText(
                    items[index],
                    rect.centerX(),
                    textY,
                    paint
                )
            }
        }
    }

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                draggedIndex =
                    indexAt(
                        event.x,
                        event.y
                    )

                lastSwapIndex =
                    draggedIndex

                invalidate()

                performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                if (draggedIndex >= 0) {

                    val target =
                        indexAt(
                            event.x,
                            event.y
                        )

                    if (
                        target != draggedIndex &&
                        target != lastSwapIndex
                    ) {

                        val temp =
                            items[draggedIndex]

                        items[draggedIndex] =
                            items[target]

                        items[target] =
                            temp

                        draggedIndex =
                            target

                        lastSwapIndex =
                            target

                        performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        )

                        invalidate()
                    }
                }

                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {

                draggedIndex = -1
                lastSwapIndex = -1

                invalidate()

                return true
            }
        }

        return true
    }
}
