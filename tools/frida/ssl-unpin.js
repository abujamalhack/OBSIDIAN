Java.perform(function () {
  try {
    var Pinner = Java.use("okhttp3.CertificatePinner");
    Pinner.check.overload("java.lang.String", "java.util.List").implementation = function () {
      return;
    };
  } catch (e) {
    console.log("[-] okhttp pinner absent");
  }

  try {
    var Ext = Java.use("android.net.http.X509TrustManagerExtensions");
    Ext.checkServerTrusted.overload(
      "[Ljava.security.cert.X509Certificate;",
      "java.lang.String",
      "java.lang.String"
    ).implementation = function (chain) {
      return Java.use("java.util.Arrays").asList(chain);
    };
  } catch (e) {
    console.log("[-] X509TrustManagerExtensions absent");
  }

  try {
    var WVC = Java.use("android.webkit.WebViewClient");
    WVC.onReceivedSslError.implementation = function (view, handler, error) {
      handler.proceed();
    };
  } catch (e) {
    console.log("[-] WebViewClient absent");
  }

  console.log("[+] pinning hooks installed");
});
