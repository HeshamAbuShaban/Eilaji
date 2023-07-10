package dev.anonymous.eilaji.utils;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;

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
}
