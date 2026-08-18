package dev.tsdroid.bridge

import android.content.Context
import dev.tsdroid.diag.DiagLog
import java.io.File

/**
 * Copies the bundled DPDFNet model from APK assets into app-private storage.
 *
 * We deliberately use filesDir instead of cacheDir: the model is ~10 MB and
 * cache may be evicted under storage pressure, which would silently degrade
 * every subsequent connection until the next app start.
 */
object DenoiserModelManager {

    private const val TAG = "DenoiserModel"
    private const val ASSET_PATH = "denoiser/dpdfnet2_48khz_hr.onnx"
    private const val MODEL_FILE_NAME = "dpdfnet2_48khz_hr.onnx"

    @Volatile
    private var preparedPath: String? = null

    /**
     * Returns the absolute path to a usable model file, or null when the
     * model is unavailable (asset missing, copy failed, zero bytes).
     */
    fun ensureModel(context: Context): String? {
        preparedPath?.let { existing ->
            if (File(existing).length() > 0) return existing
        }

        synchronized(this) {
            preparedPath?.let { existing ->
                if (File(existing).length() > 0) return existing
            }

            val target = File(context.filesDir, "denoiser").let { dir ->
                dir.mkdirs()
                File(dir, MODEL_FILE_NAME)
            }

            try {
                if (target.length() <= 0) {
                    context.assets.open(ASSET_PATH).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                if (target.length() > 0) {
                    DiagLog.i(TAG, "Denoiser model ready at ${target.absolutePath}")
                    preparedPath = target.absolutePath
                    return target.absolutePath
                }
            } catch (e: Exception) {
                DiagLog.e(TAG, "Failed to prepare denoiser model", e)
                target.delete()
            }
            return null
        }
    }
}
