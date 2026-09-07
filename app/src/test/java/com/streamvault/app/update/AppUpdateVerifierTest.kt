package com.streamvault.app.update

import com.google.common.truth.Truth.assertThat
import com.streamvault.data.preferences.AppUpdateArtifactMetadata
import com.streamvault.domain.model.Result
import org.junit.Test
import org.mockito.kotlin.mock

class AppUpdateVerifierTest {
    private val installed = UpdatePackageIdentity(
        packageName = "com.streamvault.app",
        versionCode = 19L,
        signers = setOf("official-signer")
    )

    @Test
    fun `missing or malformed checksums fail before accessing the filesystem`() {
        val verifier = AppUpdateVerifier(mock())
        listOf(null, "", " ", "sha256:not-a-digest", "a".repeat(63), "g".repeat(64)).forEach { checksum ->
            val result = verifier.verifyAndStage(java.io.File("missing.apk"), checksum)
            assertThat(result).isInstanceOf(Result.Error::class.java)
            assertThat((result as Result.Error).message).contains("checksum")
        }
    }

    @Test
    fun `checksum normalization accepts exactly a SHA256 digest`() {
        assertThat(normalizedUpdateSha256("  ${"AB".repeat(32)}  ")).isEqualTo("ab".repeat(32))
    }

    @Test
    fun `artifact checksum is bound to the downloaded version rather than the latest release`() {
        val metadata = AppUpdateArtifactMetadata("1.0.18", "ab".repeat(32))
        assertThat(updateArtifactChecksum("1.0.18", metadata)).isEqualTo(metadata.sha256)
        assertThat(updateArtifactChecksum("1.0.19", metadata)).isNull()
        assertThat(updateArtifactChecksum("1.0.18", null)).isNull()
        assertThat(updateArtifactChecksum("1.0.18", metadata.copy(sha256 = "invalid"))).isNull()
    }

    @Test
    fun `same package and signer accept a newer or equal version`() {
        assertThat(updatePackageValidationError(installed, installed.copy(versionCode = 20))).isNull()
        assertThat(updatePackageValidationError(installed, installed)).isNull()
    }

    @Test
    fun `different package is not offered as an update`() {
        assertThat(updatePackageValidationError(installed, installed.copy(packageName = "example.other.app")))
            .contains("not an update")
    }

    @Test
    fun `downgrade is rejected`() {
        assertThat(updatePackageValidationError(installed, installed.copy(versionCode = 18)))
            .contains("older")
    }

    @Test
    fun `unsigned or unreadable signing identity is rejected`() {
        assertThat(updatePackageValidationError(installed, installed.copy(signers = emptySet())))
            .contains("could not be verified")
        assertThat(updatePackageValidationError(installed.copy(signers = emptySet()), installed))
            .contains("could not be verified")
    }

    @Test
    fun `unrelated signing key is rejected`() {
        val candidate = installed.copy(signers = setOf("other-signer"), signerHistory = setOf("other-signer"))
        assertThat(updatePackageValidationError(installed, candidate)).contains("does not match")
    }

    @Test
    fun `verified forward rotation accepts the current installed key in candidate lineage`() {
        val candidate = installed.copy(
            signers = setOf("rotated-signer"),
            signerHistory = setOf("official-signer", "rotated-signer")
        )
        assertThat(updatePackageValidationError(installed, candidate)).isNull()
        assertThat(updatePackageValidationError(candidate, installed)).contains("does not match")
    }

    @Test
    fun `multiple signers must match exactly and cannot use a partial overlap`() {
        val multiple = installed.copy(signers = setOf("official-signer", "second-signer"))
        assertThat(updatePackageValidationError(multiple, multiple)).isNull()
        assertThat(updatePackageValidationError(multiple, installed)).contains("does not match")
        assertThat(updatePackageValidationError(installed, multiple)).contains("does not match")
    }

    @Test
    fun `incomplete candidate signing lineage is rejected`() {
        val candidate = installed.copy(signers = setOf("rotated-signer"))
        assertThat(updatePackageValidationError(installed, candidate)).contains("does not match")
    }
}