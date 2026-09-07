package com.streamvault.app.update

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.streamvault.domain.model.Result
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUpdateVerifierInstrumentationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val verifier = AppUpdateVerifier(context)
    private val ownedFiles = mutableListOf<File>()

    @After
    fun cleanUp() {
        ownedFiles.forEach(File::delete)
    }

    @Test
    fun signedInstalledApk_isVerifiedAndOnlyThePrivateCopyIsShared() {
        val source = File(context.applicationInfo.sourceDir)
        val download = File.createTempFile("update-test-", ".apk", context.cacheDir).also(ownedFiles::add)
        source.copyTo(download, overwrite = true)
        val expected = sha256(download)

        val verified = successfulFile(verifier.verifyAndStage(download, expected))
        assertThat(verified.parentFile?.canonicalPath)
            .isEqualTo(File(context.cacheDir, "verified_updates").canonicalPath)
        assertThat(verified.canonicalPath).isNotEqualTo(download.canonicalPath)
        assertThat(sha256(verified)).isEqualTo(expected)

        download.writeText("replaced after verification")
        assertThat(sha256(verified)).isEqualTo(expected)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", verified)
        val sharedHash = context.contentResolver.openInputStream(uri)!!.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            hex(digest.digest())
        }
        assertThat(sharedHash).isEqualTo(expected)
        assertNoPendingCopies()
    }

    @Test
    fun changedDownload_failsIntegrityWithoutPublishingAnApk() {
        val source = File(context.applicationInfo.sourceDir)
        val download = File.createTempFile("update-tamper-test-", ".apk", context.cacheDir).also(ownedFiles::add)
        source.copyTo(download, overwrite = true)
        val expected = sha256(download)
        RandomAccessFile(download, "rw").use { file ->
            val original = file.readByte().toInt()
            file.seek(0)
            file.writeByte(original xor 1)
        }

        val result = verifier.verifyAndStage(download, expected)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("integrity")
        assertNoPendingCopies()
    }

    @Test
    fun differentSignedPackage_isRejectedEvenWithCorrectChecksum() {
        val instrumentationApk = File(InstrumentationRegistry.getInstrumentation().context.applicationInfo.sourceDir)

        val result = verifier.verifyAndStage(instrumentationApk, sha256(instrumentationApk))

        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).contains("not an update")
        assertNoPendingCopies()
    }

    @Test
    fun malformedApk_isRejectedEvenWithCorrectChecksum() {
        val download = File.createTempFile("update-invalid-test-", ".apk", context.cacheDir).also(ownedFiles::add)
        download.writeText("not an APK")

        val result = verifier.verifyAndStage(download, sha256(download))

        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertNoPendingCopies()
    }

    @Test
    fun privateCopyTampering_isDetectedOnRepeatVerification() {
        val download = File(context.applicationInfo.sourceDir)
        val expected = sha256(download)
        val verified = successfulFile(verifier.verifyAndStage(download, expected))
        verified.writeText("corrupt private copy")

        val result = verifier.verifyAndStage(download, expected)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat(verified.exists()).isFalse()
        assertNoPendingCopies()
    }

    private fun successfulFile(result: Result<File>): File {
        assertThat(result).isInstanceOf(Result.Success::class.java)
        return (result as Result.Success).data.also(ownedFiles::add)
    }

    private fun assertNoPendingCopies() {
        val pending = File(context.cacheDir, "verified_updates").listFiles().orEmpty()
            .filter { it.name.startsWith("pending-") }
        assertThat(pending).isEmpty()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return hex(digest.digest())
    }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { byte -> "%02x".format(byte) }
}