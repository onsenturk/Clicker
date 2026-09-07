package com.streamvault.app.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.streamvault.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val UPDATE_SHA256 = Regex("^[a-fA-F0-9]{64}$")
private const val MAX_UPDATE_BYTES = 256L * 1024 * 1024

internal fun normalizedUpdateSha256(value: String?): String? =
    value?.trim()?.takeIf(UPDATE_SHA256::matches)?.lowercase()

internal data class UpdatePackageIdentity(
    val packageName: String,
    val versionCode: Long,
    val signers: Set<String>,
    val signerHistory: Set<String> = signers
)

internal fun updatePackageValidationError(
    installed: UpdatePackageIdentity,
    candidate: UpdatePackageIdentity
): String? = when {
    candidate.packageName != installed.packageName ->
        "The downloaded APK is not an update for this app."
    candidate.versionCode < installed.versionCode ->
        "The downloaded APK is older than the installed app."
    installed.signers.isEmpty() || candidate.signers.isEmpty() ->
        "The update signing identity could not be verified."
    installed.signers.size > 1 || candidate.signers.size > 1 ->
        if (installed.signers == candidate.signers) null
        else "The update signing identity does not match this app."
    !candidate.signerHistory.containsAll(installed.signers) ||
        !candidate.signerHistory.containsAll(candidate.signers) ->
        "The update signing identity does not match this app."
    else -> null
}

@Singleton
class AppUpdateVerifier @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    @Synchronized
    fun verifyAndStage(download: File, expectedSha256: String?): Result<File> {
        val expectedHash = normalizedUpdateSha256(expectedSha256)
            ?: return Result.error("Update checksum is missing or invalid. Check for updates and download again.")
        var pending: File? = null
        try {
            if (!download.isFile) return Result.error("Downloaded update file is missing.")
            val directory = File(context.cacheDir, "verified_updates")
            if (!directory.isDirectory && !directory.mkdirs()) {
                throw IOException("Could not create private update cache")
            }
            pending = File.createTempFile("pending-", ".apk", directory)
            val actualHash = pending.outputStream().buffered().use { output ->
                digestUpdate(download, output)
            }
            if (actualHash != expectedHash) {
                return Result.error("Downloaded update failed its integrity check. Download it again.")
            }

            val candidate = readArchiveIdentity(pending)
                ?: return Result.error("The downloaded file is not a signed Android application.")
            val installed = readInstalledIdentity()
            updatePackageValidationError(installed, candidate)?.let { error ->
                return Result.error(error)
            }

            val verified = File(directory, "$expectedHash.apk")
            if (verified.exists()) {
                if (digestUpdate(verified) != expectedHash) {
                    verified.delete()
                    return Result.error("The private update copy failed verification. Download it again.")
                }
            } else if (!pending.renameTo(verified)) {
                throw IOException("Could not publish the verified update copy")
            }
            verified.setLastModified(System.currentTimeMillis())
            directory.listFiles()
                ?.filter { it != verified && it.extension == "apk" && UPDATE_SHA256.matches(it.nameWithoutExtension) }
                ?.sortedByDescending(File::lastModified)
                ?.drop(2)
                ?.forEach(File::delete)
            return Result.success(verified)
        } catch (error: IOException) {
            return Result.error("Update verification could not read the APK.", error)
        } catch (error: SecurityException) {
            return Result.error("Update signing verification failed.", error)
        } catch (error: PackageManager.NameNotFoundException) {
            return Result.error("The installed app identity could not be verified.", error)
        } finally {
            pending?.delete()
        }
    }

    private fun readInstalledIdentity(): UpdatePackageIdentity {
        val flags = signingFlags()
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, flags)
        }
        return info.toUpdateIdentity()
    }

    private fun readArchiveIdentity(file: File): UpdatePackageIdentity? {
        val flags = signingFlags()
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
        }
        return info?.toUpdateIdentity()
    }

    @Suppress("DEPRECATION")
    private fun signingFlags(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        PackageManager.GET_SIGNATURES
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.toUpdateIdentity(): UpdatePackageIdentity {
        val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.apkContentsSigners
        } else {
            signatures
        }
        val history = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.signingCertificateHistory ?: current
        } else {
            current
        }
        return UpdatePackageIdentity(
            packageName = packageName.orEmpty(),
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong(),
            signers = current.orEmpty().map { sha256Hex(it.toByteArray()) }.toSet(),
            signerHistory = history.orEmpty().map { sha256Hex(it.toByteArray()) }.toSet()
        )
    }

    private fun digestUpdate(file: File, output: OutputStream? = null): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_UPDATE_BYTES) throw IOException("Update exceeds the size limit")
                digest.update(buffer, 0, count)
                output?.write(buffer, 0, count)
            }
        }
        if (total == 0L) throw IOException("Update is empty")
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { byte -> "%02x".format(byte) }
}