package ai.obsidian.agent

import ai.obsidian.agent.crypto.Envelope
import ai.obsidian.agent.net.C2Client
import ai.obsidian.agent.util.Probe
import android.app.Application
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.Executor

class AgentApp : Application() {

    lateinit var cfg: Config
    lateinit var store: Store
    lateinit var audit: Audit

    var client: C2Client? = null
        private set

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val mainExecutor: Executor = Executor { runnable ->
        Handler(Looper.getMainLooper()).post(runnable)
    }

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        audit = Audit(this)
        cfg = Config(this)
        if (cfg.enrolled) {
            cfg.setRung(Probe.detect(this))
            client = C2Client(this, Envelope.ctx(this))
            audit.info("boot", "rung=${cfg.rung}")
        }
    }

    fun onboard(relay: String, deviceId: String, apiKey: String, opPub: String, chat: String) {
        cfg.save(relay, deviceId, apiKey, opPub, chat)
        Envelope.ensureIdentity(this, deviceId, opPub)
        cfg.enrolled = true
        client = C2Client(this, Envelope.ctx(this))
        cfg.setRung(Probe.detect(this))
        audit.info("enroll", deviceId)
    }

    fun requireClient(): C2Client = client ?: error("not enrolled")
}
