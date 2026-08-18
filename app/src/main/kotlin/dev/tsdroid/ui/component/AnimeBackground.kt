package dev.tsdroid.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import dev.tsdroid.background.CustomBackgroundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Custom user background only.
 *
 * The previous online "anime wallpaper" mode (loliapi.com + wallpaper cache)
 * was removed for size and readability reasons. See README "已移除的二次元
 * 背景实现" for the exact implementation if it ever needs to be restored.
 */
object AnimeWallpaperState {
    val customBitmap = mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    val dominantColor = mutableStateOf<Color?>(null)

    /** Average relative luminance of the current wallpaper, 0 = black, 1 = white. */
    val backgroundLuminance = mutableFloatStateOf(0.5f)

    /** Best readable text color directly on top of the current wallpaper. */
    val recommendedContentColor = mutableStateOf<Color?>(null)

    /** Loads or clears the custom background. IO-safe. */
    suspend fun refreshCustomBackground(context: Context) {
        withContext(Dispatchers.IO) {
            val customFile = CustomBackgroundManager.getActiveBackground(context)
            if (customFile != null) {
                val bm = BitmapFactory.decodeFile(customFile.absolutePath)
                if (bm != null) {
                    customBitmap.value = bm.asImageBitmap()
                    extractColorFromBitmap(bm)
                    return@withContext
                }
            }

            customBitmap.value = null
            dominantColor.value = null
            backgroundLuminance.floatValue = 0.5f
            recommendedContentColor.value = null
        }
    }

    private fun extractColorFromBitmap(bitmap: Bitmap) {
        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return
        val scaled = Bitmap.createScaledBitmap(mutable, 48, 48, true)
        var r = 0; var g = 0; var b = 0; var count = 0
        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val pixel = scaled.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) > 128) {
                    r += android.graphics.Color.red(pixel)
                    g += android.graphics.Color.green(pixel)
                    b += android.graphics.Color.blue(pixel)
                    count++
                }
            }
        }
        if (count > 0) {
            val rn = r.toFloat() / count / 255f
            val gn = g.toFloat() / count / 255f
            val bn = b.toFloat() / count / 255f
            dominantColor.value = Color(
                red = rn.coerceIn(0f, 1f),
                green = gn.coerceIn(0f, 1f),
                blue = bn.coerceIn(0f, 1f),
            )

            // Relative luminance (Rec. 709) tells us whether dark or light
            // text wins on this particular wallpaper.
            val luminance = 0.2126f * rn + 0.7152f * gn + 0.0722f * bn
            backgroundLuminance.floatValue = luminance
            recommendedContentColor.value = if (luminance > 0.5f) {
                Color(0xFF141414) // bright image → dark text
            } else {
                Color(0xFFF4F4F5) // dark image → light text
            }
        }
    }
}

/**
 * Renders the user-selected custom background, if any. No online fetch, no
 * cache: this keeps APK size and startup work down and never fights the UI
 * for readability.
 */
@Composable
fun CustomBackground() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AnimeWallpaperState.refreshCustomBackground(context)
    }

    val customBmp = AnimeWallpaperState.customBitmap.value
    if (customBmp == null) return

    var imageLoaded by remember(customBmp) { mutableStateOf(false) }
    LaunchedEffect(customBmp) { imageLoaded = true }
    val imageAlpha by animateFloatAsState(
        targetValue = if (imageLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "customBgFadeIn",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.75f)
    ) {
        Image(
            bitmap = customBmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = imageAlpha },
        )

        // Adaptive readability scrim: bright wallpapers get a dark scrim for
        // light text and vice versa, following the active theme.
        val textIsLight = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
        val wallpaperLuminance = AnimeWallpaperState.backgroundLuminance.floatValue
        val boost = if (textIsLight) {
            ((wallpaperLuminance - 0.35f) / 0.65f).coerceIn(0f, 1f)
        } else {
            ((0.65f - wallpaperLuminance) / 0.65f).coerceIn(0f, 1f)
        }
        val scrimColor = if (textIsLight) Color.Black else Color.White
        val scrimAlpha = 0.06f + 0.34f * boost

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scrimColor.copy(alpha = scrimAlpha),
                            scrimColor.copy(alpha = scrimAlpha * 0.35f),
                            scrimColor.copy(alpha = scrimAlpha),
                        )
                    )
                )
        )
    }
}
