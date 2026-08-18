package dev.tsdroid.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.request.ImageResult
import coil3.request.SuccessResult
import dev.tsdroid.background.CustomBackgroundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object AnimeWallpaperState {
    val currentUrl = mutableStateOf<String?>(null)
    val customBitmap = mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    val dominantColor = mutableStateOf<Color?>(null)

    /** Average relative luminance of the current wallpaper, 0 = black, 1 = white. */
    val backgroundLuminance = mutableFloatStateOf(0.5f)

    /** Best readable text color directly on top of the current wallpaper. */
    val recommendedContentColor = mutableStateOf<Color?>(null)

    private var fetched = false

    suspend fun ensureFetched(context: Context) {
        if (fetched) return
        fetched = true
        WallpaperCacheManager.init(context)
        refreshCustomBackground(context)
        if (customBitmap.value == null) {
            fetchOnlineWallpaper(context)
        }
    }

    fun refreshCustomBackground(context: Context) {
        val customFile = CustomBackgroundManager.getActiveBackground(context)
        if (customFile != null) {
            val bm = BitmapFactory.decodeFile(customFile.absolutePath)
            if (bm != null) {
                customBitmap.value = bm.asImageBitmap()
                extractColorFromBitmap(bm)
            }
        } else {
            customBitmap.value = null
        }
    }

    private suspend fun fetchOnlineWallpaper(context: Context) {
        withContext(Dispatchers.IO) {
            var networkSuccess = false
            try {
                val url = URL("https://www.loliapi.com/acg/pe/")
                val conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val redirect = conn.getHeaderField("Location")
                val finalUrl = if (!redirect.isNullOrBlank()) redirect else conn.url.toString()
                conn.disconnect()
                currentUrl.value = finalUrl

                val imgConn = URL(finalUrl).openConnection() as HttpURLConnection
                imgConn.connectTimeout = 8000
                imgConn.readTimeout = 8000
                val bitmap = BitmapFactory.decodeStream(imgConn.inputStream)
                imgConn.disconnect()
                if (bitmap != null) {
                    extractColorFromBitmap(bitmap)
                    WallpaperCacheManager.saveToCache(context, finalUrl)
                    networkSuccess = true
                }
            } catch (_: Exception) {
            }
            if (!networkSuccess) {
                WallpaperCacheManager.getRandomCachedFile()?.let { cached ->
                    BitmapFactory.decodeFile(cached.absolutePath)?.let { bm ->
                        currentUrl.value = "file://${cached.absolutePath}"
                        extractColorFromBitmap(bm)
                    }
                }
            }
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
            backgroundLuminance.value = luminance
            recommendedContentColor.value = if (luminance > 0.5f) {
                Color(0xFF141414) // bright image → dark text
            } else {
                Color(0xFFF4F4F5) // dark image → light text
            }
        }
    }

    fun extractDominantColor(result: ImageResult) {
        if (result !is SuccessResult) return
        val bitmap = when (val image = result.image) {
            is BitmapImage -> image.bitmap
            else -> return
        }
        extractColorFromBitmap(bitmap)
    }
}

@Composable
fun AnimeBackground(enabled: Boolean) {
    if (!enabled) return

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AnimeWallpaperState.ensureFetched(context)
    }

    val customBmp = AnimeWallpaperState.customBitmap.value
    val url = AnimeWallpaperState.currentUrl.value

    var imageLoaded by remember { mutableStateOf(false) }
    val imageAlpha by animateFloatAsState(
        targetValue = if (imageLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "bgFadeIn",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.75f)
    ) {
        if (customBmp != null) {
            imageLoaded = true
            androidx.compose.foundation.Image(
                bitmap = customBmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = imageAlpha },
                onSuccess = { state ->
                    imageLoaded = true
                    AnimeWallpaperState.extractDominantColor(state.result)
                },
            )
        }

        // Adaptive readability scrim. If the theme draws light text, bright
        // wallpapers get a darker scrim; if it draws dark text, dark
        // wallpapers get a lighter scrim. Dark/light judgment follows the
        // actual onSurface color, so it works for system, forced and AMOLED
        // theme modes without extra plumbing.
        val textIsLight = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
        val wallpaperLuminance = AnimeWallpaperState.backgroundLuminance.value
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
