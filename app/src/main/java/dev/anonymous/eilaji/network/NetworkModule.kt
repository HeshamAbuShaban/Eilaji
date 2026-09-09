package dev.anonymous.eilaji.network

import android.content.Context
import com.google.gson.GsonBuilder
import dev.anonymous.eilaji.BuildConfig
import dev.anonymous.eilaji.storage.AppSharedPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val BASE_URL = "https://api.eilaji.com/api/v1/"

    private const val DEV_BASE_URL = "http://10.0.2.2:8080/api/v1/"
    private const val DEV_PHYSICAL_URL = "http://192.168.1.100:8080/api/v1/"

    fun provideOkHttpClient(context: Context): OkHttpClient {
        val sharedPreferences = AppSharedPreferences.Instance(context)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = sharedPreferences.getToken()

            val request = if (!token.isNullOrBlank()) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()
            } else {
                original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()
            }

            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun provideRetrofit(context: Context): Retrofit {
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()

        val baseUrl = if (BuildConfig.DEBUG) DEV_BASE_URL else BASE_URL

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun provideApiService(context: Context): ApiService {
        return provideRetrofit(context).create(ApiService::class.java)
    }
}