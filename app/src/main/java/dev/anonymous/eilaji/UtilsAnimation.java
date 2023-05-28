package dev.anonymous.eilaji;

import android.animation.ValueAnimator;

import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import com.google.android.material.progressindicator.CircularProgressIndicator;

public class UtilsAnimation {

    public static void animationProgress(CircularProgressIndicator indicator, float pageNum, int currentPage, boolean isForward) {
        int max = indicator.getMax();
        float value = max / pageNum;
        // بنزود 2 علشان لما نقدم بنكون راجعين 1 + لما نعمل رجوع بنرجع كمان واحد
        float startValue = value * (isForward ? currentPage : currentPage + 2);
        ValueAnimator animator = ValueAnimator.ofFloat(value);
        animator.setInterpolator(new LinearOutSlowInInterpolator());
        animator.setDuration(300);
        animator.addUpdateListener(valueAnimator -> {
            float animValue = ((float) valueAnimator.getAnimatedValue());
            int progress = Math.round(startValue + (isForward ? animValue : -animValue));
            indicator.setProgress(progress);
        });
        animator.start();
    }
}
