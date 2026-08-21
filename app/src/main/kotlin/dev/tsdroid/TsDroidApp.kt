package dev.tsdroid

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import dev.tsdroid.data.BookmarkStore
import dev.tsdroid.diag.DiagLog
import dev.tsdroid.han.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TsDroidApp : Application() {

    companion object {
        const val CHANNEL_ID_CONNECTION = "ts_connection"

        init {
            System.loadLibrary("tslib_jni")
            try {
                // Optional tscore backend used for A/B testing. When the
                // library is absent the app keeps using tslib_jni only.
                System.loadLibrary("tscore_jni")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("TsDroidApp", "tscore_jni unavailable; legacy backend only", e)
            }
            try {
                // Optional AI-denoiser library; older builds or unsupported
                // ABIs simply keep using the native NoiseSuppressor fallback.
                System.loadLibrary("tslib_denoiser")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("TsDroidApp", "tslib_denoiser unavailable; AI denoising disabled", e)
            }
        }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        initDiagnostics()
        // Encrypt any bookmark passwords still stored in plaintext by older builds.
        appScope.launch {
            try {
                BookmarkStore(applicationContext).migrateLegacyPasswords()
            } catch (e: Exception) {
                Log.e("TsDroidApp", "Bookmark password migration failed", e)
            }
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // One-time migration: older builds created this channel with
        // IMPORTANCE_LOW, which makes some OEM ROMs (MIUI/HyperOS) hide the
        // notification quick actions. Recreate it once at DEFAULT level.
        val prefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("connection_channel_upgraded_to_default", false)) {
            manager.getNotificationChannel(CHANNEL_ID_CONNECTION)?.let { existing ->
                if (existing.importance != NotificationManager.IMPORTANCE_DEFAULT) {
                    manager.deleteNotificationChannel(CHANNEL_ID_CONNECTION)
                }
            }
            prefs.edit().putBoolean("connection_channel_upgraded_to_default", true).apply()
        }

        val channel = NotificationChannel(
            CHANNEL_ID_CONNECTION,
            getString(R.string.channel_connection),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.channel_connection_desc)
            // No sound/vibration for a persistent voice-call notification.
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Keeps a small diag.log for connection/audio issues and saves a
     * crash-<timestamp>.log for every uncaught exception. The files live in
     * Android/data/com.yuaxi.ts6droid.cn/files/logs/ and can be pulled with
     * adb or a USB file browser.
     */
    private fun initDiagnostics() {
        val logDir = File(getExternalFilesDir(null) ?: filesDir, "logs")
        DiagLog.init(logDir)

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val crashFile = File(logDir, "crash-$stamp.log")
                crashFile.writeText(
                    buildString {
                        appendLine("time=$stamp")
                        appendLine("thread=${thread.name}")
                        appendLine(Log.getStackTraceString(throwable))
                    }
                )
                DiagLog.e("TsDroidApp", "Crash saved to ${crashFile.absolutePath}", throwable)
            } catch (_: Throwable) {
                Log.e("TsDroidApp", "Failed to persist crash log", throwable)
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
        DiagLog.i("TsDroidApp", "Application started")
    }
}
