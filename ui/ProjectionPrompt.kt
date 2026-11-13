package ai.obsidian.agent.ui

import ai.obsidian.agent.svc.ScreenService
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class ProjectionPrompt : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(MediaProjectionManager::class.java)
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    @Deprecated("Activity result API not required for a single consent hop")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ScreenService.startWith(this, resultCode, data)
        }
        finish()
    }

    companion object {
        private const val REQUEST_CODE = 9001
    }
}
