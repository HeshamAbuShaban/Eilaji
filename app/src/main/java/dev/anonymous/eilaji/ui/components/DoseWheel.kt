package dev.anonymous.eilaji.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import dev.anonymous.eilaji.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Two stacked spinning wheels (hour above, minute below) with a fixed centre marker.
 *
 * Built for a patient setting a dose time one-handed: the wheels are large, each detent gives a
 * short haptic tick, and the selected row is always the highlighted one under the marker.
 * A one-handed alternative to the platform time picker, and it renders the same in RTL.
 */
class DoseWheel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onTimeChanged: ((hour: Int, minute: Int) -> Unit)? = null

    private val rowHeight = 112f * resources.displayMetrics.density
    private var value = floatArrayOf(0f, 0f) // continuous wheel positions
    private var minuteStep = 5

    private var items: List<List<String>> = listOf(listOf(""), listOf(""))
    private var picking = 0

    private val density = resources.displayMetrics.density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBounds = Rect()

    private val brand = ContextCompat.getColor(context, R.color.primary_color)
    private val labelColor = ContextCompat.getColor(context, R.color.darkGray)

    init {
        contentDescription = context.getString(R.string.dose_wheel_content_description)
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        textPaint.color = brand
        markerPaint.color = brand
        markerPaint.strokeWidth = 2f * density
        bandPaint.color = brand
        bandPaint.alpha = 20
        buildHours()
        val now = java.util.Calendar.getInstance()
        setTime(now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE))
    }

    /** Sets the wheel granularity (5 or 15 minute steps). */
    fun setMinuteStep(step: Int) {
        minuteStep = step.coerceIn(1, 30)
        buildHours()
        invalidate()
    }

    fun setTime(hour: Int, minute: Int) {
        value[0] = ((hour % 24) + 24) % 24f
        value[1] = (minute.coerceIn(0, 59) / minuteStep).toFloat()
        invalidate()
    }

    /** Selected hour, and the minute snapped back to the step. */
    fun selectedTime(): Pair<Int, Int> {
        val hour = (((value[0].roundToInt()) % 24) + 24) % 24
        val minute = (value[1].roundToInt() * minuteStep).coerceIn(0, 59)
        return hour to minute
    }

    fun selectedTimeText(): String {
        val (h, m) = selectedTime()
        val calendarFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return try {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, h)
                set(java.util.Calendar.MINUTE, m)
                set(java.util.Calendar.SECOND, 0)
            }
            calendarFormat.format(cal.time).uppercase()
        } catch (_: Exception) {
            String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m)
        }
    }

    private fun buildHours() {
        items = listOf(
            (0..23).map { String.format(java.util.Locale.getDefault(), "%02d", it) },
            (0 until (60 / minuteStep)).map { String.format(java.util.Locale.getDefault(), "%02d", it * minuteStep) }
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize((280 * density).toInt(), widthMeasureSpec)
        val height = resolveSize((rowHeight * 2.6f).toInt(), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f
        val upperCentre = centerY - rowHeight / 2f
        val lowerCentre = centerY + rowHeight / 2f

        // selection bands
        canvas.drawRoundRect(0f, upperCentre - rowHeight / 2f, width.toFloat(), upperCentre + rowHeight / 2f, 16f * density, 16f * density, bandPaint)
        canvas.drawRoundRect(0f, lowerCentre - rowHeight / 2f, width.toFloat(), lowerCentre + rowHeight / 2f, 16f * density, 16f * density, bandPaint)

        drawWheel(canvas, items[0], value[0], upperCentre, 22f)
        drawWheel(canvas, items[1], value[1], lowerCentre, 22f)

        // marker lines
        val lineHalf = 24f * density
        canvas.drawLine(lineHalf, upperCentre, width - lineHalf, upperCentre, markerPaint)
        canvas.drawLine(lineHalf, lowerCentre, width - lineHalf, lowerCentre, markerPaint)

        // side labels
        textPaint.color = labelColor
        textPaint.textSize = 12f * density
        canvas.drawText(
            context.getString(R.string.dose_wheel_hour),
            12f * density,
            upperCentre - rowHeight / 2f - 8f * density,
            textPaint
        )
        canvas.drawText(
            context.getString(R.string.dose_wheel_minute),
            12f * density,
            lowerCentre + rowHeight / 2f + 20f * density,
            textPaint
        )
        textPaint.color = brand
    }

    private fun drawWheel(canvas: Canvas, list: List<String>, position: Float, centerY: Float, baseSp: Float) {
        if (list.isEmpty()) return
        list.forEachIndexed { index, text ->
            val delta = index - position
            // only the nearest rows are drawn, keeps the draw cheap
            if (abs(delta) > 2.5f) return@forEachIndexed
            val y = centerY + delta * rowHeight / 2f
            val closeness = (1f - min(abs(delta) / 2.5f, 1f))
            textPaint.textSize = baseSp * density * (0.6f + 0.4f * closeness)
            textPaint.color = if (abs(delta) < 0.5f) brand else labelColor
            textPaint.alpha = (60 + 195 * closeness).toInt()
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            canvas.drawText(text, width / 2f, y + textBounds.height() / 2f, textPaint)
            textPaint.alpha = 255
        }
    }

    private val gesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            picking = if (e.y < height / 2f) 0 else 1
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val list = items[picking]
            if (list.isEmpty()) return false
            val delta = -distanceY / (rowHeight / 2f)
            val before = value[picking]
            value[picking] = (value[picking] + delta).coerceIn(0f, (list.size - 1).toFloat())
            if (before.roundToInt() != value[picking].roundToInt()) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            invalidate()
            onTimeChanged?.let { val (h, m) = selectedTime(); it(h, m) }
            return true
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return gesture.onTouchEvent(event) || super.onTouchEvent(event)
    }
}