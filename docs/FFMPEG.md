# FFmpeg Integration

StreamVault bundles the Media3 FFmpeg audio decoder artifact for unsupported IPTV audio codecs.

Current product scope:

- Audio fallback only
- Media3 version: `1.11.0`
- Supported ABIs: `arm64-v8a`, `armeabi-v7a`
- Enabled decoders: `ac3`, `eac3`, `dca`, `mp2`, `mp3`, `truehd`
- License target: LGPL-compatible build only
- FFmpeg source: `n6.0.1`, commit `c41ff724ede7da657762d61097e26fac296c53bf`
- Media3 source: `1.11.0`, commit `2bc207851df311340767e913931ca7b28cab1794`
- Native API: `25`; NDK: `28.2.13676358`; ELF load alignment: `16384` bytes

Important product notes:

- Local FFmpeg fallback improves playback on this device only; Cast receivers may still use different codec support.
- Recordings keep their source codecs. Successful in-app playback does not guarantee the same file will play everywhere else.

Refresh procedure:

1. Rebuild the AndroidX FFmpeg decoder against the exact Media3 version in the version catalog. The Windows procedure is below.
2. Replace [the versioned AAR](../player/libs/media3-decoder-ffmpeg-1.11.0.aar) only with the actual rebuilt output, never by renaming an older decoder.
3. Update [its provenance manifest](../player/libs/media3-decoder-ffmpeg-1.11.0.properties), including the AAR SHA-256, if the output or toolchain changes.
4. Run `:player:verifyLocalFfmpegArtifact` and the player/app test builds.
5. Run native decoder instrumentation and sustained multi-channel playback with post-release leak checks. A successful build is not playback validation.

## Windows rebuild

Prerequisites: JDK 17, Git for Windows (including Git Bash), Android SDK 36, and NDK `28.2.13676358`. Run from the repository root in PowerShell. Source checkouts and intermediate output stay under the ignored build directory. The source directories must not already exist when cloning.

```powershell
git clone --depth 1 --branch 1.11.0 --filter=blob:none --sparse https://github.com/androidx/media.git build/dependency-upgrade/media3-source
git -C build/dependency-upgrade/media3-source sparse-checkout set libraries/decoder_ffmpeg
git clone --depth 1 --branch n6.0.1 https://github.com/FFmpeg/FFmpeg.git build/dependency-upgrade/ffmpeg-source

$mediaSource = "$PWD/build/dependency-upgrade/media3-source"
$ffmpegSource = "$PWD/build/dependency-upgrade/ffmpeg-source"
$nativeOutput = "$PWD/build/dependency-upgrade/native-libs"
$sdk = "$env:LOCALAPPDATA/Android/Sdk"
$ndk = "$sdk/ndk/28.2.13676358"
$bash = 'C:/Program Files/Git/bin/bash.exe'

foreach ($abi in @('arm64-v8a', 'armeabi-v7a')) {
	& $bash tools/ffmpeg/build-native-windows.sh $ffmpegSource $ndk $nativeOutput $abi $mediaSource
	if ($LASTEXITCODE -ne 0) { throw "Native build failed for $abi" }
}

$previousAndroidHome = $env:ANDROID_HOME
try {
	$env:ANDROID_HOME = $sdk
	.\gradlew.bat -p tools/ffmpeg assembleRelease "-Pmedia3Source=$mediaSource" "-PffmpegSource=$ffmpegSource" "-PffmpegNativeOutput=$nativeOutput/jni" --max-workers=2
	if ($LASTEXITCODE -ne 0) { throw 'Decoder AAR build failed' }
} finally {
	$env:ANDROID_HOME = $previousAndroidHome
}
```

[The native script](../tools/ffmpeg/build-native-windows.sh) rejects different source revisions or tracked source modifications, disables GPL/nonfree/version-3 components and external library autodetection, and links the official JNI wrapper. [The packaging project](../tools/ffmpeg/build.gradle.kts) compiles the official Java sources against Media3 `1.11.0` and embeds the Media3 and FFmpeg license texts in `classes.jar`.

The resulting artifact is `tools/ffmpeg/build/outputs/aar/media3-decoder-ffmpeg-release.aar`. Compare its SHA-256 to the provenance manifest before replacing the bundled AAR. Exact ZIP bytes can vary across build environments; any different artifact needs a new recorded hash and validation. Keep source checkouts, static libraries, and build commands available for the applicable LGPL replacement/relinking obligations.
