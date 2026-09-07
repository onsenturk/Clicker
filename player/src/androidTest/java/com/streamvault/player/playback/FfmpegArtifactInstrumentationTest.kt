@file:androidx.media3.common.util.UnstableApi

package com.streamvault.player.playback

import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FfmpegArtifactInstrumentationTest {

    @Test
    fun bundledFfmpegLibrary_loadsNativeCode() {
        assertThat(FfmpegLibrary.isAvailable()).isTrue()
        assertThat(FfmpegLibrary.getVersion()).isNotEmpty()
    }

    @Test
    fun bundledFfmpegLibrary_supportsMpegLayerTwoAudio() {
        assertThat(FfmpegLibrary.supportsFormat("audio/mpeg-L2")).isTrue()
    }
}