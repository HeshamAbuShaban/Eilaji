package dev.anonymous.eilaji.network

import android.content.Context
import com.google.gson.GsonBuilder
import dev.anonymous.eilaji.BuildConfig
import dev.anonymous.eilaji.storage.AppSharedPreferences
import androidx.lifecycle.MutableLiveData
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/** One-shot signal used to send the user back to login when the session cannot be renewed. */
object SessionEvents {
    private val _expired = MutableLiveData<Boolean>()
    val expired: MutableLiveData<Boolean> = _expired

    fun notifyExpired() {
        _expired.postValue(true)
    }
}

object NetworkModule {

    private const val BASE_URL = "https://api.eilaji.com/api/v1/"

    private const val DEV_BASE_URL = "http://localhost:8080/api/v1/"
    private const val DEV_EMULATOR_URL = "http://10.0.2.2:8080/api/v1/"

    private const val TIMEOUT_SECONDS = 30L
    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024

    @Volatile
    private var retrofitInstance: Retrofit? = null

    @Volatile
    private var apiServiceInstance: ApiService? = null

    @Volatile
    private var refreshApiServiceInstance: ApiService? = null

    private val refreshLock = Any()

    private fun baseUrl(): String =
        if (BuildConfig.DEBUG) {
            if (isEmulator()) DEV_EMULATOR_URL else DEV_BASE_URL
        } else {
            BASE_URL
        }

    private fun isEmulator(): Boolean {
        return try {
            val fingerprint = android.os.Build.FINGERPRINT
            fingerprint.startsWith("generic") || fingerprint.startsWith("unknown") ||
                android.os.Build.MODEL.contains("sdk_gphone") || android.os.Build.MODEL.contains("emulator")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Shared client for the whole process. One instance means one connection pool, one DNS cache
     * and one HTTP cache instead of a new pool per screen.
     */
    fun provideOkHttpClient(context: Context): OkHttpClient {
        val appContext = context.applicationContext
        val sharedPreferences = AppSharedPreferences.getInstance(appContext)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Never log bodies in release: they carry tokens, addresses and phone numbers.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val token = sharedPreferences.getToken()
            val builder = request.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
            if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
            chain.proceed(builder.build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .cache(Cache(File(appContext.cacheDir, "http"), CACHE_SIZE_BYTES))
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Answers a 401 with a fresh token pair: refreshes once, then replays the original request.
     * When the refresh itself fails the session is cleared and the app is sent back to login
     * instead of failing every call with a confusing 401.
     */
    private fun buildAuthenticator(appContext: Context): okhttp3.Authenticator {
        return okhttp3.Authenticator { _, response ->
            val prefs = AppSharedPreferences.getInstance(appContext)
            val refreshToken = prefs.getRefreshToken()
            if (responseCount(response) >= 2 || refreshToken.isNullOrBlank()) {
                prefs.clearAll()
                SessionEvents.notifyExpired()
                return@Authenticator null
            }

            val newToken = synchronized(refreshLock) {
                val current = prefs.getToken()
                // Another request refreshed while this one waited.
                if (!current.isNullOrBlank() && current != response.request.header("Authorization")?.removePrefix("Bearer ")) {
                    return@synchronized current
                }
                try {
                    val call = provideRefreshApiService(appContext).refreshToken(RefreshTokenRequest(refreshToken))
                    val refreshResponse = call.execute()
                    val body = refreshResponse.body()
                    if (refreshResponse.isSuccessful && body?.success == true && body.data != null) {
                        prefs.putToken(body.data.accessToken)
                        prefs.putRefreshToken(body.data.refreshToken)
                        body.data.accessToken
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }

            if (newToken.isNullOrBlank()) {
                prefs.clearAll()
                SessionEvents.notifyExpired()
                null
            } else {
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }
        }
    }

    private fun responseCount(response: okhttp3.Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    fun provideRetrofit(context: Context): Retrofit {
        return retrofitInstance ?: synchronized(this) {
            retrofitInstance ?: buildRetrofit(context.applicationContext).also { retrofitInstance = it }
        }
    }

    private fun buildRetrofit(appContext: Context): Retrofit {
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()

        val client = provideOkHttpClient(appContext).newBuilder()
            .authenticator(buildAuthenticator(appContext))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun provideApiService(context: Context): ApiService {
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: provideRetrofit(context).create(ApiService::class.java)
                .also { apiServiceInstance = it }
        }
    }

    /**
     * A client without the authenticator, so a refresh that itself answers 401 cannot loop.
     */
    fun provideRefreshApiService(context: Context): ApiService {
        return refreshApiServiceInstance ?: synchronized(this) {
            refreshApiServiceInstance ?: Retrofit.Builder()
                .baseUrl(baseUrl())
                .client(provideOkHttpClient(context.applicationContext))
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create()
                    )
                )
                .build()
                .create(ApiService::class.java)
                .also { refreshApiServiceInstance = it }
        }
    }

    /** Base URL without the api/v1/ part, used to build image URLs. */
    @JvmStatic
    fun getBaseUrlForImages(): String = baseUrl().replace("api/v1/", "")
}
