package dev.anonymous.eilaji.utils

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.PendingNotificationDto
import dev.anonymous.eilaji.network.RegisterPushTokenRequest
import dev.anonymous.eilaji.network.websocket.PushPayload
import dev.anonymous.eilaji.network.websocket.WebSocketManager
import dev.anonymous.eilaji.storage.AppSharedPreferences
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

/**
 * Self-hosted push. No Firebase, no third party.
 *
 * This foreground service keeps one WebSocket open to the Eilaji backend so order, prescription
 * and chat updates arrive within seconds. When Android closes the socket anyway, the periodic
 * [PushSyncWorker] drains everything the server parked for this account. Medication reminders do
 * not come through here: they stay on the local alarm, which is exact.
 */
class PushService : Service() {

    private var socket: WebSocketManager? = null
    private val prefs by lazy { AppSharedPreferences.getInstance(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        val token = prefs.getToken()
        if (token.isNullOrBlank()) {
            stopSelf()
            return
        }
        registerDevice(token)
        connect(token)
        startFallbackSchedule()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            teardown()
            stopSelf()
            return START_NOT_STICKY
        }
        if (socket?.isConnected != true) {
            val token = prefs.getToken()
            if (token.isNullOrBlank()) {
                stopSelf()
                return START_NOT_STICKY
            }
            connect(token)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        try {
            socket?.disconnect()
            socket?.onPush = null
        } catch (_: Exception) {
        }
        socket = null
    }

    private fun startInForeground() {
        PushNotifications.ensureChannel(this)
        val notification: Notification = NotificationCompat.Builder(this, PushNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(getString(R.string.push_service_title))
            .setContentText(getString(R.string.push_service_body))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // Foreground start can be refused in the background; the fallback worker still runs.
        }
    }

    private fun connect(token: String) {
        try {
            val manager = WebSocketManager(token)
            socket = manager
            manager.onPush = java.util.function.Consumer { payload -> PushNotifications.show(applicationContext, payload) }
            manager.onConnected = Runnable {
                // Live socket is back: collect anything that was parked while it was down.
                drainPending()
            }
            manager.connect()
        } catch (_: Exception) {
        }
    }

    private fun registerDevice(token: String) {
        // A stable per-install id: the backend uses it to know which device is reachable.
        val deviceId = try {
            var existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing.isNullOrBlank()) {
                existing = UUID.randomUUID().toString()
                prefs.putString(KEY_DEVICE_ID, existing)
            }
            existing
        } catch (_: Exception) {
            UUID.randomUUID().toString()
        }
        try {
            NetworkModule.provideApiService(applicationContext)
                .registerPushToken(RegisterPushTokenRequest(deviceId, "android"))
                .enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<Any>> {
                    override fun onResponse(call: Call<dev.anonymous.eilaji.network.ApiResponse<Any>>, response: Response<dev.anonymous.eilaji.network.ApiResponse<Any>>) {}
                    override fun onFailure(call: Call<dev.anonymous.eilaji.network.ApiResponse<Any>>, t: Throwable) {}
                })
        } catch (_: Exception) {
        }
    }

    private fun drainPending() {
        try {
            NetworkModule.provideApiService(applicationContext)
                .getPendingNotifications()
                .enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<List<PendingNotificationDto>>> {
                    override fun onResponse(
                        call: Call<dev.anonymous.eilaji.network.ApiResponse<List<PendingNotificationDto>>>,
                        response: Response<dev.anonymous.eilaji.network.ApiResponse<List<PendingNotificationDto>>>
                    ) {
                        val items = response.body()?.data ?: return
                        items.forEach { dto ->
                            PushNotifications.show(
                                applicationContext,
                                PushPayload(dto.id, dto.title, dto.body, dto.route)
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<dev.anonymous.eilaji.network.ApiResponse<List<PendingNotificationDto>>>,
                        t: Throwable
                    ) {}
                })
        } catch (_: Exception) {
        }
    }

    private fun startFallbackSchedule() {
        try {
            PushSyncWorker.schedule(applicationContext)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val ACTION_STOP = "dev.anonymous.eilaji.STOP_PUSH"
        const val NOTIFICATION_ID = 7701
        private const val KEY_DEVICE_ID = "push_device_id"

        fun start(context: Context) {
            if (AppSharedPreferences.getInstance(context).getToken().isNullOrBlank()) return
            try {
                val intent = Intent(context, PushService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (_: Exception) {
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, PushService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}