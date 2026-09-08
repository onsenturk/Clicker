# Dependency Upgrade Validation

Validated locally on Windows on 2026-09-08 against the then-uncommitted worktree based on `0901da4b19fc34687a26270021aaf185c934c398`. No commit, push, or publication had been performed at validation time. This report records the pre-identity dependency build; the subsequent fork identity is documented in [FORK_IDENTITY.md](FORK_IDENTITY.md). Historical production validation remains in [INITIAL_RELEASE_REVIEW.md](INITIAL_RELEASE_REVIEW.md).

## Versions

| Dependency | Previous | Validated |
| --- | --- | --- |
| Media3, including local FFmpeg decoder | 1.9.2 | 1.11.0 |
| Compose BOM | 2025.06.01 | 2026.03.00 |
| Resolved Compose UI/runtime/foundation | 1.10.3 | 1.10.5 |
| Compose Material3 | 1.3.2 | 1.4.0 |
| TV Foundation | 1.0.0-beta01 | 1.0.0 |
| TV Material | 1.0.1 | 1.1.0 |
| Security Crypto | 1.1.0-alpha06 | 1.1.0 |
| OkHttp family | 4.12.0 | 5.4.0 |
| R8 override | 8.13.19 | 8.13.23 |

AGP 8.10.1, Gradle 8.12, Kotlin 2.2.0, minSdk 25, compile/targetSdk 36, app version 1.0.17.1/code 19, and database schema 78 are unchanged.

OkHttp 5.5.0 was tested but its Android AAR requires compileSdk 37. Version 5.4.0 requires 36, so the SDK/AGP migration is deferred. The Compose BOM aligns the existing 1.10 family with the stable TV libraries rather than jumping to the newer 1.12 family. Security Crypto 1.1.0 is a stable maintenance upgrade, but its APIs remain deprecated; this work does not migrate encrypted storage.

## Compatibility Changes

- Local JVM tests select `okhttp-jvm` to load public-suffix resources; production and instrumentation retain `okhttp-android` and its packaged asset.
- Stalker TLS classification traverses causes and suppressed route failures with cycle detection. OkHttp fast fallback can report a connection failure with a suppressed certificate failure. Origin/SPKI approval and redirect security remain unchanged.
- App and data test APK packaging excludes only the duplicate Java OSGi manifest. Licenses and runtime assets remain included.
- The PCM tap intercepts Media3's `AudioSinkConfig` API. A new regression verifies forwarding, partial buffer capture, format changes, and timestamps. Shared audio-sink release tests remain passing.
- The video renderer workaround accepts Media3's new adaptive-format flag without changing its no-codec-reuse policy.
- Phone setup now gives Restore its own header space, with horizontally scrollable source tabs. Restore wraps its content width instead of taking the whole row. The reachability test scrolls Restore only when it is off-screen; touch, D-pad, and rapid URL-input assertions remain enabled.
- R8 8.13.19 spent an extended period recursively hashing computation trees during release optimization. The narrow 8.13.23 patch completed full minification; optimization and resource shrinking remain enabled.

## Native Decoder

The new AAR was compiled from the official, unmodified Media3 1.11.0 and FFmpeg 6.0.1 source revisions, not renamed from the old artifact. See [FFMPEG.md](FFMPEG.md) for the tested Windows rebuild and [the provenance manifest](../player/libs/media3-decoder-ffmpeg-1.11.0.properties) for exact revisions and checksum.

Both ARM64 and ARMv7 libraries built with NDK 28.2.13676358/API 25. Each ELF LOAD segment has 16384-byte alignment. Enabled decoders are `ac3,eac3,dca,mp2,mp3,truehd`; GPL, nonfree, version-3 components, and external autodetection are disabled. The build reports LGPL 2.1-or-later licensing. The AAR and final APKs contain the upstream Media3 and FFmpeg license texts.

Native instrumentation passed loading the library and querying MPEG Layer II support on the phone emulator's ARM64 translation path. It is not evidence of audible MP2 decoding on a physical ARM device or ARMv7 runtime validation.

## Verification

| Gate | Tests | Failures / Errors | Skips |
| --- | ---: | ---: | ---: |
| App JVM | 339 | 0 / 0 | 0 |
| Data JVM | 1063 | 0 / 0 | 0 |
| Domain JVM | 132 | 0 / 0 | 0 |
| Player JVM | 207 | 0 / 0 | 0 |
| API 36 phone data | 76 | 0 / 0 | 0 |
| API 36 phone native decoder | 2 | 0 / 0 | 0 |
| API 36 phone ordinary app suite | 38 | 0 / 0 | 2 TV-only |
| API 36 phone isolated quota | 1 | 0 / 0 | 0 |
| API 36 phone live playback/leaks | 2 | 0 / 0 | 0 |
| API 36 TV ordinary app suite | 38 | 0 / 0 | 1 phone-only |
| API 25 TV platform/setup/updater/player smoke | 15 | 0 / 0 | 0 |

The JVM total is 1741. Final debug and fully minified unsigned release assembly, coverage XML generation, and strict configuration-cache checks passed. The unchanged domain test task was also checked explicitly. Final app/data/player lint found no new issues; baseline ceilings remain 1560/18/9 and no baseline file changed.

Phone tests include real rapid keyboard input, search controls, player controls, updater verification, and repeated player/view cleanup with LeakCanary. TV tests include D-pad activation. API 25 coverage includes platform contracts, updater archive verification, setup, and player smoke tests; it is not the full API 25 suite.

An initial phone run exposed the Restore/tab overlap and a test attempting to scroll a fixed button. A subsequent input-dispatch timeout coincided with host memory pressure and a Play Store ANR. The same final assertions passed after stopping idle build daemons and cold-booting the emulator without wiping data. Device runs used a single worker and a 2 GiB Gradle heap; release optimization ran separately with emulators stopped.

Final local evidence is under `build/dependency-upgrade/validation/`: `jvm-final-*`, `coverage-final.xml`, `final-build.log`, `final-lint.log`, `data-phone-device`, `player-phone-device`, `app-phone-final`, `app-tv36-final`, `app-api25-final`, `app-quota-phone`, and `live-phone-final`. Earlier failed phone results remain separate in `app-phone-device`.

## Sustained Playback

The existing live-validation instrumentation captured the full screenshot sequence and hashed the center video region. It checked a title-matched Android media session, sanitized player logs, and released engines/views under LeakCanary after each channel.

| Channel | Screenshots | Interval | Full / Video Unique Hashes | Capture | Final Media Session |
| --- | ---: | --- | --- | --- | --- |
| France 24 English | 61 | 2 seconds | 61 / 61 | 121.067 seconds | PLAYING, no error |
| France 24 French | 61 | 2 seconds | 61 / 61 | 121.131 seconds | PLAYING, no error |

Both runs show HLS prepare, read progress, and first-frame success. Neither shows a fatal player error, stuck timeout, `state=ERROR`, or unintended MPEG-TS fallback. Frame progression continued through both capture windows. Native heap samples were 27,706,704 to 27,688,720 bytes (English) and 27,779,184 to 27,591,344 bytes (French). Post-release leak checks passed. These are bounded observations, not proof that no leak can occur in longer sessions or with other providers.

## APKs

The original [installation APK](../build/phone-test/StreamVault-1.0.17.1-debug-0901da4b.apk) remains unchanged, SHA-256 `9F59436404F49D5206BA3D868A203D2C5BDA8AA4DDF65131534FF4AC3CCBED0C`.

The upgraded [phone test APK](../build/phone-test/StreamVault-1.0.17.1-debug-dependency-upgrade-20260908.apk) is 51,871,372 bytes, SHA-256 `293619FA2BF6A7BA1081B1FEAC136D9A7CFB222182BC974057F525CFAC169968`. It has a verified single-signer v2 debug signature, package `com.streamvault.app.debug`, version code 19, Android 7.1+ support, and only ARM64/ARMv7 native ABIs. It shares the original debug package and version; it is a replacement test build, not a separately installed app.

The [unsigned release APK](../build/dependency-upgrade/validation/artifacts/StreamVault-1.0.17.1-dependency-upgrade-unsigned.apk) is 18,132,438 bytes, SHA-256 `CF58D2B27D838C43BF9077453B9EC5835FD3D5BE12783A47B9DE08DE0DE81FD0`. The matching R8 mapping is preserved beside it. Both APKs pass 16 KiB ZIP alignment and include the native license notices and Android OkHttp public-suffix asset. No production key was used and no release was published.

## Remaining Gates

- Official signing, release versioning, signed/minified install-and-update round trip, release checksum publication, and the existing privacy/support/rollback checklist remain release-owner gates.
- Physical ARM64/ARMv7 codec/audio checks, Cast receiver behavior, and provider-specific multi-hour playback still need validation. Emulator results cannot establish zero defects, zero leaks, or optimal performance on every device.
- Graphify refresh could not run because its CLI/global skill and generated graph are unavailable in this environment.