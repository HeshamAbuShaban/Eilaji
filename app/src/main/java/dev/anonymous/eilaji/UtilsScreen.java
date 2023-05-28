package dev.anonymous.eilaji;

import android.content.res.Resources;

public class UtilsScreen {
    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }
}
