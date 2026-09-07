@file:androidx.media3.common.util.UnstableApi

package com.streamvault.app.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Debug
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.streamvault.domain.model.StreamInfo
import com.streamvault.domain.model.StreamType
import com.streamvault.player.Media3PlayerEngine
import com.streamvault.player.PlaybackState
import com.streamvault.player.PlayerRenderSurfaceType
import com.streamvault.player.PlayerSurfaceResizeMode
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import leakcanary.AppWatcher
import leakcanary.DetectLeaksAfterTestSuccess
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LivePlaybackValidationTest {
    @get:Rule
    val detectLeaks = DetectLeaksAfterTestSuccess()

    @Test
    fun france24English_keepsRenderingForTwoMinutes() = validateChannel(
        "france24-english",
        "France 24 English",
        "https://live.france24.com/hls/live/2037218/F24_EN_HI_HLS/master_2300.m3u8"
    )

    @Test
    fun france24French_keepsRenderingForTwoMinutes() = validateChannel(
        "france24-french",
        "France 24 French",
        "https://live.france24.com/hls/live/2037179/F24_FR_HI_HLS/master_2300.m3u8"
    )

    private fun validateChannel(channelId: String, title: String, url: String) = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(
            "External live streams require -PlivePlaybackValidation=true",
            InstrumentationRegistry.getArguments().getString("livePlaybackValidation") == "true"
        )
        assumeTrue("Media session inspection requires API 29 or newer", Build.VERSION.SDK_INT >= 29)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dependencies = EntryPointAccessors.fromApplication(context, PlayerMemoryEntryPoint::class.java)
        val client = OkHttpClient()
        val outputRoot = requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")) {
            "Run with Gradle connectedDebugAndroidTest to preserve playback evidence"
        }
        val output = File(outputRoot, "live-playback-validation/$channelId").apply {
            check(isDirectory || mkdirs()) { "Cannot create playback evidence directory" }
        }
        val marker = "$channelId-${SystemClock.elapsedRealtime()}"
        val hashes = mutableListOf<String>()
        val samples = JSONArray()
        var engine: Media3PlayerEngine? = null
        var renderView: PlayerView? = null
        val readLogLevel = shellOutput("getprop log.tag.PlayerDataReadStats").trim()
        val scenario = ActivityScenario.launch(LivePlaybackValidationActivity::class.java)
        try {
            shellOutput("setprop log.tag.PlayerDataReadStats DEBUG")
            Log.i(TAG, "$marker begin")
            scenario.onActivity { activity ->
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val player = Media3PlayerEngine(
                    context,
                    client,
                    dependencies.playbackCompatibilityRepository(),
                    dependencies.audioCompatibilityMemoryStore(),
                    dependencies.playbackSupportSnapshotStore()
                )
                engine = player
                player.mediaSessionId = "live-validation-$channelId"
                val view = player.createRenderView(
                    activity,
                    PlayerSurfaceResizeMode.FIT,
                    PlayerRenderSurfaceType.SURFACE_VIEW
                ) as PlayerView
                renderView = view
                view.useController = false
                activity.setContentView(view)
                player.bindRenderView(view, PlayerSurfaceResizeMode.FIT)
                player.prepare(StreamInfo(url = url, title = title, streamType = StreamType.HLS))
            }
            val player = requireNotNull(engine)
            withTimeout(45_000L) {
                combine(player.isPlaying, player.videoFormat, player.playbackState) { playing, format, state ->
                    check(state != PlaybackState.ERROR) { "Live channel failed before its first video frame" }
                    playing && format.width > 0 && format.height > 0
                }.first { it }
            }
            val startedAt = SystemClock.elapsedRealtime()
            repeat(FRAME_COUNT) { frameIndex ->
                delay((startedAt + frameIndex * FRAME_INTERVAL_MS - SystemClock.elapsedRealtime()).coerceAtLeast(0))
                val bounds = Rect()
                instrumentation.runOnMainSync {
                    check(requireNotNull(renderView).videoSurfaceView?.getGlobalVisibleRect(bounds) == true)
                }
                val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                try {
                    File(output, "frame_%03d.png".format(frameIndex)).outputStream().use { stream ->
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                    }
                    hashes += videoHash(bitmap, bounds)
                } finally {
                    bitmap.recycle()
                }
                samples.put(
                    JSONObject()
                        .put("frame", frameIndex)
                        .put("elapsedMs", SystemClock.elapsedRealtime() - startedAt)
                        .put("videoSha256", hashes.last())
                        .put("state", player.playbackState.value.name)
                        .put("isPlaying", player.isPlaying.value)
                        .put("nativeHeapBytes", Debug.getNativeHeapAllocatedSize())
                        .put("javaHeapBytes", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                )
                check(player.playbackState.value != PlaybackState.ERROR) { "Player entered ERROR during capture" }
            }
            val mediaSessions = shellOutput("dumpsys media_session")
            File(output, "media-session.txt").writeText(redactUrls(mediaSessions))
            val logs = shellOutput("logcat -d --pid=${Process.myPid()} -v time")
                .substringAfter("$marker begin")
                .lineSequence()
                .filter { PLAYBACK_MARKERS.containsMatchIn(it) }
                .joinToString("\n", transform = ::redactUrls)
            File(output, "playback.log").writeText(logs)
            val sessionPlaying = if (Build.VERSION.SDK_INT >= 29) {
                instrumentation.uiAutomation.adoptShellPermissionIdentity(android.Manifest.permission.MEDIA_CONTENT_CONTROL)
                try {
                    val manager = context.getSystemService(MediaSessionManager::class.java)
                    val controller = manager.getActiveSessions(null).singleOrNull {
                        it.packageName == context.packageName &&
                            it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) == title
                    }
                    controller?.playbackState?.let { state ->
                        state.state == android.media.session.PlaybackState.STATE_PLAYING && state.errorMessage == null
                    } == true
                } finally {
                    instrumentation.uiAutomation.dropShellPermissionIdentity()
                }
            } else {
                false
            }
            File(output, "summary.json").writeText(
                JSONObject()
                    .put("channel", title)
                    .put("frames", hashes.size)
                    .put("intervalMs", FRAME_INTERVAL_MS)
                    .put("uniqueVideoHashes", hashes.toSet().size)
                    .put("finalState", player.playbackState.value.name)
                    .put("finalIsPlaying", player.isPlaying.value)
                    .put("mediaSessionPlayingWithNoError", sessionPlaying)
                    .put("samples", samples)
                    .toString(2)
            )
            assertThat(hashes.toSet().size).isAtLeast(50)
            assertThat(hashes.windowed(6).all { it.toSet().size > 1 }).isTrue()
            assertThat(player.isPlaying.value).isTrue()
            assertThat(player.playbackState.value).isEqualTo(PlaybackState.READY)
            assertThat(sessionPlaying).isTrue()
            assertThat(logs).contains("prepare resolvedStreamType=HLS")
            assertThat(logs).contains("first-frame-success")
            assertThat(Regex("read-(progress|close) streamType=HLS bytes=[1-9][0-9]*")
                .containsMatchIn(logs)).isTrue()
            assertThat(Regex("fatal-error|Player stuck|state=ERROR|prepare resolvedStreamType=MPEG_TS_LIVE|source-malformed live-ts-fallback")
                .containsMatchIn(logs)).isFalse()
        } finally {
            instrumentation.runOnMainSync {
                renderView?.let { view ->
                    engine?.releaseRenderView(view)
                    AppWatcher.objectWatcher.expectWeaklyReachable(view, "Live validation view released")
                }
                engine?.let { player ->
                    player.release()
                    AppWatcher.objectWatcher.expectWeaklyReachable(player, "Live validation engine released")
                }
                renderView = null
                engine = null
            }
            scenario.close()
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
            shellOutput("setprop log.tag.PlayerDataReadStats '${readLogLevel.replace("'", "")}'")
        }
    }

    private fun videoHash(bitmap: Bitmap, bounds: Rect): String {
        check(bounds.intersect(0, 0, bitmap.width, bitmap.height))
        bounds.inset(bounds.width() / 4, bounds.height() / 4)
        val cropped = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
        val sampled = Bitmap.createScaledBitmap(cropped, 160, 90, true)
        try {
            val pixels = IntArray(sampled.width * sampled.height)
            sampled.getPixels(pixels, 0, sampled.width, 0, 0, sampled.width, sampled.height)
            val bytes = ByteBuffer.allocate(pixels.size * Int.SIZE_BYTES)
            pixels.forEach(bytes::putInt)
            return MessageDigest.getInstance("SHA-256").digest(bytes.array())
                .joinToString("") { "%02x".format(it) }
        } finally {
            if (sampled !== cropped) sampled.recycle()
            if (cropped !== bitmap) cropped.recycle()
        }
    }

    private fun shellOutput(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText() }

    private fun redactUrls(value: String): String = Regex("https?://[^\\s]+").replace(value, "<url>")

    private companion object {
        const val TAG = "LivePlaybackValidation"
        const val FRAME_COUNT = 61
        const val FRAME_INTERVAL_MS = 2_000L
        val PLAYBACK_MARKERS = Regex(
            "prepare resolvedStreamType=|first-frame-success|read-(progress|close) streamType=|retry category=|" +
                "live-recovery |fatal-error|Player stuck|state=ERROR|source-malformed live-ts-fallback"
        )
    }
}