@file:androidx.media3.common.util.UnstableApi

package com.streamvault.player.playback

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.audio.AudioSink
import com.google.common.truth.Truth.assertThat
import com.streamvault.player.LiveAudioPcmBuffer
import com.streamvault.player.LiveAudioTap
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import org.junit.Test

class LiveAudioTapAudioSinkTest {
    @Test
    fun configurationAndPartialBuffers_preservePcmFormatAndTimestamps() {
        var delegatedConfig: AudioSink.AudioSinkConfig? = null
        val captured = mutableListOf<LiveAudioPcmBuffer>()
        val delegate = Proxy.newProxyInstance(
            AudioSink::class.java.classLoader,
            arrayOf(AudioSink::class.java)
        ) { _, method, arguments ->
            when (method.name) {
                "configure" -> {
                    delegatedConfig = arguments[0] as AudioSink.AudioSinkConfig
                    null
                }
                "handleBuffer" -> {
                    val buffer = arguments[0] as ByteBuffer
                    buffer.position(buffer.position() + 4)
                    !buffer.hasRemaining()
                }
                else -> error("Unexpected method ${method.name}")
            }
        } as AudioSink
        val sink: AudioSink = LiveAudioTapAudioSink(delegate) { LiveAudioTap { captured += it } }
        val format = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_RAW)
            .setSampleRate(48_000)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_16BIT)
            .build()
        val configuration = AudioSink.AudioSinkConfig.Builder(format).build()
        val buffer = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))

        sink.configure(configuration)
        assertThat(sink.handleBuffer(buffer, 1_000_000L, 1)).isFalse()
        assertThat(sink.handleBuffer(buffer, 1_000_000L, 1)).isTrue()

        assertThat(delegatedConfig).isSameInstanceAs(configuration)
        assertThat(captured).hasSize(2)
        assertThat(captured[0].data).isEqualTo(byteArrayOf(1, 2, 3, 4))
        assertThat(captured[1].data).isEqualTo(byteArrayOf(5, 6, 7, 8))
        assertThat(captured.map { it.presentationTimeUs }).containsExactly(1_000_000L, 1_000_020L).inOrder()
        assertThat(captured.map { it.sampleRate }).containsExactly(48_000, 48_000)
        assertThat(captured.map { it.channelCount }).containsExactly(2, 2)
        assertThat(captured.map { it.encoding }).containsExactly(C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT)

        sink.configure(AudioSink.AudioSinkConfig.Builder(format.buildUpon().setPcmEncoding(C.ENCODING_PCM_FLOAT).build()).build())
        sink.handleBuffer(ByteBuffer.wrap(byteArrayOf(9, 10, 11, 12)), 2_000_000L, 1)
        assertThat(captured).hasSize(2)
    }
}