package com.streamvault.app

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ForkIdentityTest {
    @Test
    fun installedIdentityIsSeparateFromUpstream() {
        assertThat(BuildConfig.OFFICIAL_APPLICATION_ID).isEqualTo("com.onsenturk.streamvault")
        assertThat(BuildConfig.APPLICATION_ID).isAnyOf(
            "com.onsenturk.streamvault",
            "com.onsenturk.streamvault.debug",
            "com.onsenturk.streamvault.beta"
        )
    }
}