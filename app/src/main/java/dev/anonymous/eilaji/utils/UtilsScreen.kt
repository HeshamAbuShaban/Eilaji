package dev.anonymous.eilaji.utils

import android.content.res.Resources

object UtilsScreen {
    val screenWidth: Int
        get() = Resources.getSystem().displayMetrics.widthPixels
}