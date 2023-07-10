package dev.anonymous.eilaji.utils;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.navigation.NavOptions;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Locale;

import dev.anonymous.eilaji.R;

public class GeneralUtils {

    private static volatile GeneralUtils Instance;

    private GeneralUtils() {
    }

    public static synchronized GeneralUtils getInstance() {
        if (Instance == null) {
            Instance = new GeneralUtils();
        }
        return Instance;
    }

    public RequestBuilder<Drawable> loadImage(@NonNull String link) {
        assert AppController.getInstance() != null;
        return Glide
                .with(AppController.getInstance())
                .load(link)
                .placeholder(R.color.place_holder_color)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop();
    }

    public void showSnackBar(View view, String text) {
        Snackbar.make(view, text, Snackbar.LENGTH_SHORT).show();
    }

    public static String formatTimeStamp(Long timeStamp) {
        SimpleDateFormat format = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return format.format(timeStamp);
    }

    public static NavOptions getNavOptions() {
        return new NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_left)
                .setExitAnim(R.anim.slide_out_right)
                .setPopEnterAnim(R.anim.slide_in_right)
                .setPopExitAnim(R.anim.slide_out_left)
                .build();
    }
}
