package dev.anonymous.eilaji.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources

object UtilsScreen {
    val screenWidth: Int
        get() = Resources.getSystem().displayMetrics.widthPixels

    fun isDarkMode(ctx: Context): Boolean {
        val darkModeFlag = ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
    }
}