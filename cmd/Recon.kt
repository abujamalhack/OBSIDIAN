package ai.obsidian.agent.cmd

import ai.obsidian.agent.root.Shell
import android.content.Context

object Recon {

    fun run(context: Context, sub: String): String = when (sub) {
        "system" -> Shell.run(context, "getprop | grep -E 'ro.build|ro.boot|ro.product'")
        "patch" -> Shell.run(
            context,
            "getprop ro.build.version.security_patch; getprop ro.vendor.build.security_patch"
        )
        "perms" -> Shell.run(context, "dumpsys package ${context.packageName} | grep granted=true")
        "privacy" -> Shell.run(context, "dumpsys privacyd; settings get secure; settings get global")
        "apps" -> Shell.run(context, "pm list packages -3 -f")
        "net" -> Shell.run(context, "ip -o addr; cat /proc/net/tcp; getprop | grep -i proxy")
        "selinux" -> Shell.run(
            context,
            "getenforce; cat /proc/self/attr/current; getprop ro.boot.verifiedbootstate"
        )
        "boot" -> Shell.run(
            context,
            "getprop ro.boot.verifiedbootstate; getprop ro.boot.flash.locked; getprop ro.debuggable"
        )
        else -> "recon system|patch|perms|privacy|apps|net|selinux|boot"
    }
}
