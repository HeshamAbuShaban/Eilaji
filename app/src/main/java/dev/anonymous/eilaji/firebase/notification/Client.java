package dev.anonymous.eilaji.firebase.notification;

import android.os.Build;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Client {
    private static Retrofit retrofit;
    private static final String BASE_URL = "https://fcm.googleapis.com/";

    public static Retrofit getClient(){
        if (retrofit == null){
            // IMPORTANT: FCM Server Key should be managed on a secure backend server
            // This client-side implementation is for development only
            // In production, move notification sending logic to your backend
            String fcmServerKey = BuildConfig.FCM_SERVER_KEY;
            
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(new okhttp3.OkHttpClient.Builder()
                            .addInterceptor(chain -> {
                                okhttp3.Request original = chain.request();
                                okhttp3.Request request = original.newBuilder()
                                        .header("Content-Type", "application/json")
                                        .header("Authorization", "key=" + fcmServerKey)
                                        .method(original.method(), original.body())
                                        .build();
                                return chain.proceed(request);
                            })
                            .build())
                    .build();
        }
        return retrofit;
    }
}
