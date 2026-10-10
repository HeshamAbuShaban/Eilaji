package dev.anonymous.eilaji.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import dev.anonymous.eilaji.R
import kotlin.math.abs

/**
 * The order state rail: PENDING → CONFIRMED → PREPARING → SHIPPED → DELIVERED, with a
 * cancellation branch that stays readable in Arabic RTL.
 *
 * A rail beats a spinner for one reason: a patient can see how many steps are left. The last
 * dot grows into a check or a cross, so the end state is readable without colour.
 */
class StepStepper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class State { PENDING, CONFIRMED, PREPARING, SHIPPED, DELIVERED, CANCELLED }

    var state: State = State.PENDING
        set(value) {
            field = value
            updateDescription()
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val brand = ContextCompat.getColor(context, R.color.primary_color)
    private val inactive = ContextCompat.getColor(context, R.color.gray_dark)
    private val errorColor = ContextCompat.getColor(context, android.R.color.holo_red_dark)

    private val radius = 9f * density

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * density }

    private var lastTouchIndex = -1

    init {
        contentDescription = describe(state)
        isFocusable = true
    }

    private fun describe(value: State): String =
        context.getString(R.string.step_stepper_description, value.name.lowercase())

    private fun updateDescription() {
        contentDescription = describe(state)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(resolveSize((320 * density).toInt(), widthMeasureSpec), resolveSize((48 * density).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val count = order.size
        val step = if (count > 1) width / (count - 1f) else width.toFloat()
        val y = height / 2f
        val cancelled = state == State.CANCELLED

        // track
        linePaint.color = inactive
        linePaint.alpha = 80
        canvas.drawLine(0f, y, (step * (count - 1)), y, linePaint)

        // progress up to the current index
        val currentIndex = order.indexOf(state)
        if (cancelled) {
            linePaint.color = errorColor
            linePaint.alpha = 255
            canvas.drawLine(0f, y, (step * (count - 1)), y, linePaint)
        } else if (currentIndex > 0) {
            linePaint.color = brand
            linePaint.alpha = 255
            canvas.drawLine(0f, y, step * currentIndex, y, linePaint)
        }

        order.forEachIndexed { index, _ ->
            val cx = step * index
            val isDone = !cancelled && index <= currentIndex
            val isCurrent = !cancelled && index == currentIndex

            fillPaint.color = if (isDone) brand else inactive
            fillPaint.alpha = if (isDone) 255 else 60
            canvas.drawCircle(cx, y, if (isCurrent) radius * 1.25f else radius, fillPaint)

            if (isCurrent) {
                ringPaint.color = brand
                canvas.drawCircle(cx, y, radius * 1.9f, ringPaint)
            }

            // last node carries a check mark when delivered
            if (state == State.DELIVERED && index == count - 1) {
                val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ContextCompat.getColor(context, android.R.color.white)
                    strokeWidth = 3f * density
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                val r = radius * 0.55f
                canvas.drawLine(cx - r, y, cx - r * 0.1f, y + r * 0.8f, white)
                canvas.drawLine(cx - r * 0.1f, y + r * 0.8f, cx + r, y - r * 0.8f, white)
            }
        }

        // cancellation mark
        if (cancelled) {
            val cx = width.toFloat()
            fillPaint.color = errorColor
            fillPaint.alpha = 255
            canvas.drawCircle(cx, y, radius * 1.15f, fillPaint)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, android.R.color.white)
                strokeWidth = 3f * density
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(cx - radius * 0.5f, y - radius * 0.5f, cx + radius * 0.5f, y + radius * 0.5f, p)
            canvas.drawLine(cx + radius * 0.5f, y - radius * 0.5f, cx - radius * 0.5f, y + radius * 0.5f, p)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val count = order.size
            val step = if (count > 1) width / (count - 1f) else width.toFloat()
            val index = if (step > 0) (event.x / step).toInt().coerceIn(0, count - 1) else 0
            if (index != lastTouchIndex) {
                lastTouchIndex = index
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
            return true
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private val order = listOf(State.PENDING, State.CONFIRMED, State.PREPARING, State.SHIPPED, State.DELIVERED)
        fun fromApi(status: String?): State = try {
            State.valueOf(status?.trim()?.uppercase() ?: "PENDING")
        } catch (_: Exception) {
            State.PENDING
        }

        /** 0..1 progress, used by the Compose island already on the tracking screen. */
        fun progressOf(status: String?): Float {
            val index = order.indexOf(fromApi(status))
            return if (index < 0) 0f else index / (order.size - 1f)
        }
    }
}