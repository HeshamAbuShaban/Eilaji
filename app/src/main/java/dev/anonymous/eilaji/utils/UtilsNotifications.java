package dev.anonymous.eilaji.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import dev.anonymous.eilaji.R;

public class UtilsNotifications {
    private static final String TAG = "UtilsNotifications";

    public static void setUpNotification(
            Context context,
            int notificationId,
            String title,
            String contentText,
            Bitmap largeImage,
            Bitmap bigPicture
    ) {
        String NOT_CHANNEL_NAME = "notification messaging";
        long[] NOT_VIBRATION_PATTERN = {0, 120, 70, 150, 70, 600};

        NotificationCompat.Builder notBuilder =
                new NotificationCompat.Builder(context, String.valueOf(notificationId));
        notBuilder
                .setContentTitle(title)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVibrate(NOT_VIBRATION_PATTERN)
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true);

        if (largeImage != null) {
            notBuilder.setLargeIcon(largeImage);
        } else {
            Bitmap defaultUserImage = getBitmapFromVectorDrawable(context, R.drawable.ic_default_user);
            notBuilder.setLargeIcon(defaultUserImage);
        }

        if (bigPicture != null) {
            notBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bigPicture));
        } else {
            notBuilder.setContentText(contentText);
        }

        NotificationManagerCompat notManager = NotificationManagerCompat.from(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notManager.getNotificationChannel(NOT_CHANNEL_NAME) == null) {
                NotificationChannel notChannel = new NotificationChannel(
                        String.valueOf(notificationId),
                        NOT_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH);

                notChannel.setDescription("NOT_CHANNEL_Description");
                notChannel.setVibrationPattern(NOT_VIBRATION_PATTERN);
                notChannel.enableVibration(true);
                notManager.createNotificationChannel(notChannel);
            }
        }

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notManager.notify(notificationId, notBuilder.build());
        }
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        assert drawable != null;
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static void getBitmapFromUrl(String strURL, LoadImageListener listener) {
        if (strURL == null) {
            listener.onLoadImageListener(null);
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(strURL);
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                listener.onLoadImageListener(bitmap);
            } catch (IOException e) {
                Log.e(TAG, "getBitmapFromUrl: " + strURL);
                e.printStackTrace();
                listener.onLoadImageListener(null);
            }
        }).start();
    }

    public interface LoadImageListener {
        void onLoadImageListener(Bitmap imageBitmap);
    }
}
