package dev.andikune.homeio.presentation.wallpaper

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import dev.andikune.homeio.domain.model.SettingsConfig
import java.io.File

@Composable
fun WallpaperView(config: SettingsConfig) {
    val context = LocalContext.current

    LaunchedEffect(config.wallpaperMode, config.customPhotoPath) {
        Log.d("WallpaperDebug", "Mode: ${config.wallpaperMode}, Path: ${config.customPhotoPath}")
    }

    key(config.wallpaperMode, config.customPhotoPath, config.customVideoPath, config.customActiveType, config.wallpaperScaleMode) {
        when (config.wallpaperMode) {
            "SINEMATIK" -> {
                val resId = context.resources.getIdentifier("wp_video", "raw", context.packageName)
                if (resId != 0) {
                    VideoWallpaper(
                        uri = getResourceUri(context.packageName, resId),
                        scaleMode = config.wallpaperScaleMode,
                        forceSoftware = config.forceSoftwareDecoder
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)))
                }
            }
            "ESTETIK" -> {
                val resId = context.resources.getIdentifier("wp_photo", "raw", context.packageName)
                if (resId != 0) {
                    ImageWallpaper(model = getResourceUri(context.packageName, resId), scaleMode = config.wallpaperScaleMode)
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFF2E1A2E)))
                }
            }
            "OLED" -> {
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
            "KUSTOM" -> {
                if (config.customActiveType == "VIDEO" && !config.customVideoPath.isNullOrBlank()) {
                    VideoWallpaper(
                        uri = getSafeUri(config.customVideoPath),
                        scaleMode = config.wallpaperScaleMode,
                        forceSoftware = config.forceSoftwareDecoder
                    )
                } else if (config.customActiveType == "PHOTO" && !config.customPhotoPath.isNullOrBlank()) {
                    ImageWallpaper(model = getSafeModel(config.customPhotoPath), scaleMode = config.wallpaperScaleMode)
                } else if (!config.wallpaperPath.isNullOrBlank()) {
                    if (config.wallpaperType == "VIDEO") {
                        VideoWallpaper(
                            uri = getSafeUri(config.wallpaperPath),
                            scaleMode = config.wallpaperScaleMode,
                            forceSoftware = config.forceSoftwareDecoder
                        )
                    } else {
                        ImageWallpaper(model = getSafeModel(config.wallpaperPath), scaleMode = config.wallpaperScaleMode)
                    }
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFF1A2E1A)))
                }
            }
            else -> {
                Box(Modifier.fillMaxSize().background(Color(0xFF0A0E1A)))
            }
        }
    }
}

private fun getResourceUri(packageName: String, resId: Int): Uri {
    return Uri.parse("android.resource://$packageName/$resId")
}

private fun getSafeUri(path: String): Uri {
    return if (path.startsWith("content://") || path.startsWith("http") || path.startsWith("android.resource://")) {
        Uri.parse(path)
    } else {
        Uri.fromFile(File(path))
    }
}

private fun getSafeModel(path: String): Any {
    return if (path.startsWith("content://") || path.startsWith("http") || path.startsWith("android.resource://")) {
        Uri.parse(path)
    } else {
        File(path)
    }
}

@Composable
private fun ImageWallpaper(model: Any, scaleMode: String) {
    val contentScale = when (scaleMode) {
        "FIT" -> ContentScale.Fit
        "FILL" -> ContentScale.Crop
        "ZOOM" -> ContentScale.Crop
        "CROP" -> ContentScale.Crop
        else -> ContentScale.Crop
    }

    Image(
        painter = rememberAsyncImagePainter(model = model),
        contentDescription = "Wallpaper Image",
        contentScale = contentScale,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun VideoWallpaper(uri: Uri, scaleMode: String, forceSoftware: Boolean = false) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val exoPlayer = remember(uri, forceSoftware) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true) // Wajib ON buat fallback

        if (forceSoftware) {
            // Memaksa ExoPlayer mencoba Software Decoder / FFmpeg (jika ada)
            renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        ExoPlayer.Builder(context, renderersFactory).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            volume = 0f
            
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("VideoWallpaper", "Error playing video: ${error.errorCodeName} - ${error.message}", error)
                }
            })
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> exoPlayer.play()
                Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.stop()
                    exoPlayer.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                val playerView = PlayerView(ctx).apply {
                    useController = false
                    this.player = exoPlayer
                    resizeMode = when (scaleMode) {
                        "FIT" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        "FILL" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        "ZOOM" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        "CROP" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
                addView(playerView)
            }
        }
    )
}
