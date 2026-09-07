package com.streamvault.player.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackLogSanitizerTest {
    @Test
    fun `redacts live username and password path segments`() {
        val sanitized = PlaybackLogSanitizer.sanitizeUrl(
            "https://example.test/live/user-name/password-value/61351.m3u8"
        )

        assertThat(sanitized).isEqualTo("example.test/live/<redacted>/<redacted>/61351.m3u8")
    }

    @Test
    fun `redacts opaque segment token path`() {
        val sanitized = PlaybackLogSanitizer.sanitizeUrl(
            "https://example.test/r/a4b5206dad97d1070000000000006eb4/61351_1429.ts"
        )

        assertThat(sanitized).isEqualTo("example.test/r/<redacted>/61351_1429.ts")
    }

    @Test
    fun `redacts opaque segment token in messages`() {
        val sanitized = PlaybackLogSanitizer.sanitizeMessage(
            "read target=https://example.test/r/a4b5206dad97d1070000000000006eb4/61351_1429.ts"
        )

        assertThat(sanitized).doesNotContain("a4b5206dad97d1070000000000006eb4")
        assertThat(sanitized).contains("/r/<redacted>/61351_1429.ts")
    }

    @Test
    fun `request shape log redacts credentials without exposing header values`() {
        val message = requireNotNull(playbackRequestShapeLogMessage(
            url = "https://url-user:url-password@example.test/live/path-user/path-password/61351.m3u8?token=query-token",
            userAgent = "private-user-agent",
            headers = mapOf(
                "Authorization" to "Bearer private-access-token",
                "Cookie" to "mac=private-device-address",
                "Referer" to "https://private-referrer.test/?secret=private-value"
            ),
            preload = false
        ))

        assertThat(message).contains("target=example.test/live/<redacted>/<redacted>/61351.m3u8")
        assertThat(message).contains("cookie=true auth=true")
        listOf(
            "url-user", "url-password", "path-user", "path-password", "query-token",
            "private-user-agent", "private-access-token", "private-device-address", "private-referrer"
        ).forEach { sensitiveValue ->
            assertThat(message).doesNotContain(sensitiveValue)
        }
    }

    @Test
    fun `request shape log skips requests without provider authentication headers`() {
        assertThat(playbackRequestShapeLogMessage(
            url = "https://example.test/video.m3u8",
            userAgent = null,
            headers = emptyMap(),
            preload = false
        )).isNull()
    }
}
