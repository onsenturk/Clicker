@file:androidx.media3.common.util.UnstableApi

package com.streamvault.app.player

import android.content.Context
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.streamvault.player.LiveAudioPcmBuffer
import com.streamvault.player.LiveAudioTap
import com.streamvault.player.Media3PlayerEngine
import com.streamvault.player.PlaybackState
import com.streamvault.player.PlayerRenderSurfaceType
import com.streamvault.player.PlayerSurfaceResizeMode
import dagger.hilt.android.EntryPointAccessors
import leakcanary.AppWatcher
import leakcanary.DetectLeaksAfterTestSuccess
import leakcanary.LeakAssertions
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerMemoryInstrumentationTest {
    @get:Rule
    val detectLeaks = DetectLeaksAfterTestSuccess()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val client = OkHttpClient()
    private var retainedEngine: Media3PlayerEngine? = null

    @After
    fun cleanUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            retainedEngine?.release()
            retainedEngine = null
        }
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }

    @Test
    fun terminalRelease_doesNotRetainLiveAudioCallback() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val engine = createEngine()
            retainedEngine = engine
            val tap = ProbeAudioTap()
            engine.setLiveAudioTap(tap)
            engine.release()
            engine.setLiveAudioTap(tap)
            AppWatcher.objectWatcher.expectWeaklyReachable(tap, "Released player must detach live audio callback")
        }

        LeakAssertions.assertNoLeaks()
        assertThat(requireNotNull(retainedEngine).playbackState.value).isEqualTo(PlaybackState.IDLE)
    }

    @Test
    fun repeatedBindResetAndRelease_doesNotRetainEnginesOrViews() {
        repeat(10) { iteration ->
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val engine = createEngine()
                val view = engine.createRenderView(
                    context,
                    PlayerSurfaceResizeMode.FIT,
                    PlayerRenderSurfaceType.SURFACE_VIEW
                ) as PlayerView
                try {
                    engine.bindRenderView(view, PlayerSurfaceResizeMode.FIT)
                    assertThat(view.player).isNotNull()
                    engine.resetForReuse()
                    assertThat(view.player).isNull()
                    engine.bindRenderView(view, PlayerSurfaceResizeMode.FIT)
                } finally {
                    engine.releaseRenderView(view)
                    engine.release()
                }
                engine.release()
                assertThat(view.player).isNull()
                AppWatcher.objectWatcher.expectWeaklyReachable(view, "Released render view $iteration")
                AppWatcher.objectWatcher.expectWeaklyReachable(engine, "Released player engine $iteration")
            }
        }
    }

    private fun createEngine(): Media3PlayerEngine {
        val dependencies = EntryPointAccessors.fromApplication(context, PlayerMemoryEntryPoint::class.java)
        return Media3PlayerEngine(
            context,
            client,
            dependencies.playbackCompatibilityRepository(),
            dependencies.audioCompatibilityMemoryStore(),
            dependencies.playbackSupportSnapshotStore()
        )
    }

    private class ProbeAudioTap : LiveAudioTap {
        var observedBuffers = 0

        override fun onPcmAudio(buffer: LiveAudioPcmBuffer) {
            observedBuffers++
        }
    }

}