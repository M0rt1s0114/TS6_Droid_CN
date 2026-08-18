package dev.tsdroid.diag

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight on-device diagnostic log.
 *
 * Writes key lifecycle events (connection, audio capture, crashes) to an
 * app-specific external directory so users can send us the file when
 * something goes wrong. Also mirrors every line to Logcat.
 */
object DiagLog {

    private const val MAX_LOG_BYTES = 512L * 1024L
    private const val TAG = "DiagLog"

    @Volatile
    private var logFile: File? = null

    fun init(directory: File) {
        try {
            directory.mkdirs()
            logFile = File(directory, "diag.log")
            i(TAG, "Diagnostic log initialized at ${logFile?.absolutePath}")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize diagnostic log", e)
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        append('I', tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable == null) Log.w(tag, message) else Log.w(tag, message, throwable)
        append('W', tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable == null) Log.e(tag, message) else Log.e(tag, message, throwable)
        append('E', tag, message, throwable)
    }

    fun crash(tag: String, throwable: Throwable) {
        Log.e(tag, "FATAL: uncaught exception", throwable)
        append('F', tag, throwable.toString(), throwable)
    }

    private fun append(level: Char, tag: String, message: String, throwable: Throwable?) {
        val file = logFile ?: return
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            synchronized(this) {
                if (file.length() > MAX_LOG_BYTES) {
                    val old = File(file.parentFile, "diag.old.log")
                    old.delete()
                    file.renameTo(old)
                }
                file.appendText("$timestamp $level/$tag: $message\n")
                if (throwable != null) {
                    file.appendText(Log.getStackTraceString(throwable))
                    file.appendText("\n")
                }
            }
        } catch (_: Throwable) {
            // Never let diagnostics break the app.
        }
    }
}
