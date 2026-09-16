package dev.anonymous.eilaji.utils

import android.animation.ValueAnimator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlin.math.roundToInt

object UtilsAnimation {
    fun animationProgress(
        indicator: CircularProgressIndicator,
        pageNum: Float,
        currentPage: Int,
        isForward: Boolean
    ) {
        val max = indicator.max
        val step = max / pageNum
        val from = indicator.progress
        val to = ((currentPage + 1) * step).roundToInt().coerceIn(0, max)
        val animator = ValueAnimator.ofInt(from, to)
        animator.interpolator = LinearOutSlowInInterpolator()
        animator.duration = 300
        animator.addUpdateListener { valueAnimator: ValueAnimator ->
            indicator.progress = (valueAnimator.animatedValue as Int).coerceIn(0, max)
        }
        animator.start()
    }
}