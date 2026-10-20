package dev.andikune.homeio.presentation.wallpaper

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import dev.andikune.homeio.domain.model.SettingsConfig
import dev.andikune.homeio.presentation.components.GlassColors
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun WallpaperView(
    config: SettingsConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val path = config.wallpaperPath

    val contentScale = when (config.wallpaperScaleMode.uppercase()) {
        "FIT" -> ContentScale.Fit
        "ZOOM" -> ContentScale.FillWidth
        "CROP" -> ContentScale.Crop
        else -> ContentScale.FillBounds
    }

    Box(modifier = modifier.fillMaxSize().background(GlassColors.DeepBackground)) {
        if (!path.isNullOrEmpty() && File(path).exists()) {
            val isVideo = config.wallpaperType.equals("VIDEO", ignoreCase = true) ||
                    path.endsWith(".mp4", ignoreCase = true) ||
                    path.endsWith(".mkv", ignoreCase = true)

            if (isVideo) {
                val exoPlayer = remember(path) {
                    ExoPlayer.Builder(context).build().apply {
                        val mediaItem = MediaItem.fromUri(Uri.fromFile(File(path)))
                        setMediaItem(mediaItem)
                        repeatMode = Player.REPEAT_MODE_ALL
                        volume = 0f // Mute
                        prepare()
                        playWhenReady = true
                    }
                }

                DisposableEffect(exoPlayer) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = when (config.wallpaperScaleMode.uppercase()) {
                                "FIT" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                "ZOOM" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                "CROP" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            }
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = File(path),
                    contentDescription = "Custom Wallpaper",
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Default iOS 26 Liquid Glass Ambient Backdrop
            DefaultAmbientBackdrop()
        }

        // Overlay to guarantee readability and frosted glass reflection contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x990A0E1A),
                            Color(0x660A0E1A),
                            Color(0xCC0A0E1A)
                        )
                    )
                )
        )
    }
}

@Composable
fun DefaultAmbientBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A),
                        Color(0xFF0A0E1A)
                    )
                )
            )
    ) {
        // Glowing cyan orb top right
        Box(
            modifier = Modifier
                .size(360.dp)
                .offset(x = 600.dp, y = (-80).dp)
                .clip(CircleShape)
                .background(Color(0x334DA3FF))
                .alpha(0.6f)
        )

        // Glowing indigo orb bottom left
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-60).dp, y = 200.dp)
                .clip(CircleShape)
                .background(Color(0x22818CF8))
                .alpha(0.5f)
        )
    }
}
