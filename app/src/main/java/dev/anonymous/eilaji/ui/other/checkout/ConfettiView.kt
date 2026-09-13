package dev.anonymous.eilaji.ui.other.checkout

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class P(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var size: Float, var rot: Float, var rotV: Float, var color: Int, var alpha: Int
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var parts: List<P> = emptyList()
    private var progress = 0f
    private var animator: ValueAnimator? = null

    private val palette = intArrayOf(
        0xFFBA324F.toInt(), 0xFFFF74B1.toInt(), 0xFFFFC4D6.toInt(),
        0xFF5E70FF.toInt(), 0xFFFF8705.toInt(), 0xFF4CAF50.toInt(), 0xFFFFFFFF.toInt()
    )

    fun burst(count: Int = 90) {
        animator?.cancel()
        val w = (width.takeIf { it > 0 } ?: 600).toFloat()
        val cx = w / 2f
        parts = (0 until count).map {
            val ang = Random.nextFloat() * Math.PI * 2
            val speed = 400 + Random.nextFloat() * 900
            P(
                x = cx + (Random.nextFloat() - 0.5f) * 80, y = -20f,
                vx = (cos(ang) * speed * 0.6).toFloat(),
                vy = (kotlin.math.abs(sin(ang)) * speed * 0.7 + 500).toFloat(),
                size = 8 + Random.nextFloat() * 12,
                rot = Random.nextFloat() * 360, rotV = (Random.nextFloat() - 0.5f) * 720,
                color = palette[Random.nextInt(palette.size)], alpha = 255
            )
        }
        progress = 0f
        visibility = VISIBLE
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1700
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
                if (progress >= 1f) postDelayed({ visibility = GONE }, 200)
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (parts.isEmpty()) return
        val h = height.toFloat()
        val t = progress
        for (p in parts) {
            val tt = t * 1.7f
            val x = p.x + p.vx * tt
            val y = p.y + p.vy * tt - 0.5f * 900 * tt * tt * -1f
            if (y > h + 40) continue
            paint.color = p.color
            paint.alpha = ((1f - t) * 255).toInt().coerceIn(0, 255)
            canvas.save()
            canvas.translate(x, y.coerceAtMost(h + 30))
            canvas.rotate(p.rot + p.rotV * t)
            val s = p.size * (1f - t * 0.3f)
            canvas.drawRoundRect(-s / 2, -s / 4, s / 2, s / 4, 3f, 3f, paint)
            canvas.restore()
        }
    }
}
