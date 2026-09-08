package com.streamvault.player.playback

import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.junit.Test

class PlayerAddressHealthEventListenerTest {

    @Test
    fun `callFailed marks the last connected address unhealthy`() {
        val address = InetAddress.getByName("192.0.2.10")
        val socketAddress = InetSocketAddress(address, 443)
        val healthStore = PlayerAddressHealthStore()
        val listener = PlayerAddressHealthEventListener(healthStore)
        val call = OkHttpClient().newCall(Request.Builder().url("https://example.test/movie.mkv").build())

        listener.connectEnd(call, socketAddress, Proxy.NO_PROXY, Protocol.HTTP_1_1)
        assertThat(healthStore.health("example.test", 443, address))
            .isEqualTo(PlayerAddressHealth.HEALTHY)

        listener.callFailed(call, IOException("read timeout"))

        assertThat(healthStore.health("example.test", 443, address))
            .isEqualTo(PlayerAddressHealth.UNHEALTHY)
    }

}
