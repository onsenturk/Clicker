@file:androidx.media3.common.util.UnstableApi

package com.streamvault.player.playback

import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioSink
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Proxy
import org.junit.Assert.assertThrows
import org.junit.Test

class AudioSinkOwningRendererTest {
    @Test
    fun sharedSink_isReleasedOnceAfterRendererCleanup() {
        val events = mutableListOf<String>()
        val sink = LiveAudioTapAudioSink(proxy<AudioSink> { events += "sink" }) { null }
        val firstRenderer = AudioSinkOwningRenderer(proxy<Renderer> { events += "first" }, sink)
        val secondRenderer = AudioSinkOwningRenderer(proxy<Renderer> { events += "second" }, sink)

        firstRenderer.release()
        secondRenderer.release()

        assertThat(events).containsExactly("first", "sink", "second").inOrder()
    }

    @Test
    fun sinkCleanup_runsEvenWhenRendererReleaseFails() {
        var sinkReleased = false
        val renderer = AudioSinkOwningRenderer(
            proxy<Renderer> { throw IllegalStateException("renderer release failed") },
            proxy<AudioSink> { sinkReleased = true }
        )

        assertThrows(IllegalStateException::class.java) { renderer.release() }
        assertThat(sinkReleased).isTrue()
    }

    private inline fun <reified Interface> proxy(crossinline release: () -> Unit): Interface =
        Proxy.newProxyInstance(Interface::class.java.classLoader, arrayOf(Interface::class.java)) { _, method, _ ->
            check(method.name == "release") { "Unexpected method ${method.name}" }
            release()
            null
        } as Interface
}