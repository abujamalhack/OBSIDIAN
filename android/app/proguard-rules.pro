-keep class ai.obsidian.agent.crypto.** { *; }
-keep class ai.obsidian.agent.collect.** { *; }
-keep class ai.obsidian.agent.web.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**
