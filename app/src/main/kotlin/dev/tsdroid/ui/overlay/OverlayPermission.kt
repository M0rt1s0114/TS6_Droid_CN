package dev.tsdroid.ui.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object OverlayPermission {
    fun canDrawOverlay(context: Context): Boolean {
        // minSdk is 29, so the runtime overlay permission always applies.
        return Settings.canDrawOverlays(context)
    }

    fun createPermissionRequestIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
    }
}
