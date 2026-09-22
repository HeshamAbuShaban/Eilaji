package dev.anonymous.eilaji.utils

import android.app.Activity
import android.view.ViewGroup
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur

object Glass {
    /** Frosted-glass background for bottom bars. */
    fun frost(activity: Activity?, blurView: BlurView, root: ViewGroup, radius: Float = 18f, overlay: Int = 0x40FFFFFF) {
        try {
            val decor = activity?.window?.decorView?.background ?: return
            blurView.setupWith(root, RenderScriptBlur(activity))
                .setFrameClearDrawable(decor)
                .setBlurRadius(radius)
                .setBlurAutoUpdate(true)
                .setHasFixedTransformationMatrix(false)
                .setOverlayColor(overlay)
        } catch (_: Exception) {}
    }
}
