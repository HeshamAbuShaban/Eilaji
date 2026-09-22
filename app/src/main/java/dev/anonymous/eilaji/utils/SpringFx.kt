package dev.anonymous.eilaji.utils

import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import dev.anonymous.eilaji.R

object SpringFx {
    /** Springy press: squash on down, overshoot back on up. */
    fun pressable(v: android.view.View, scale: Float = 0.9f) {
        try {
            v.setOnTouchListener { view, e ->
                when (e.action) {
                    android.view.MotionEvent.ACTION_DOWN -> springScale(view, scale)
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> springScale(view, 1f)
                }
                false
            }
        } catch (_: Exception) {}
    }

    fun springScale(v: android.view.View, to: Float) {
        try {
            listOf(SpringAnimation(v, SpringAnimation.SCALE_X, to), SpringAnimation(v, SpringAnimation.SCALE_Y, to)).forEach {
                it.spring.stiffness = SpringForce.STIFFNESS_MEDIUM
                it.spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                it.start()
            }
        } catch (_: Exception) {
            try { v.scaleX = to; v.scaleY = to } catch (_: Exception) {}
        }
    }

    fun pop(v: android.view.View) {
        try {
            v.scaleX = 0.7f; v.scaleY = 0.7f
            springScale(v, 1f)
        } catch (_: Exception) {}
    }

    /** Brand gradient text (primary → pink) for hero headers. */
    fun gradientText(tv: TextView) {
        try {
            val ctx = tv.context
            val start = ContextCompat.getColor(ctx, R.color.primary_color)
            val end = ContextCompat.getColor(ctx, R.color.primary_pink)
            tv.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    tv.viewTreeObserver.removeOnPreDrawListener(this)
                    try {
                        val w = tv.width.toFloat()
                        if (w > 0) {
                            tv.paint.shader = LinearGradient(0f, 0f, w, tv.textSize, start, end, Shader.TileMode.CLAMP)
                            tv.invalidate()
                        }
                    } catch (_: Exception) {}
                    return true
                }
            })
        } catch (_: Exception) {}
    }
}
