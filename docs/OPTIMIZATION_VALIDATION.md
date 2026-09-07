# Optimization Validation

Date: 2026-09-07

Branch: `copilot/check-repo-for-improvements`

Follow-up: [INITIAL_RELEASE_REVIEW.md](INITIAL_RELEASE_REVIEW.md) records the subsequent
security and UI fixes, final 1,739-test baseline, passing device gates, and replacement
artifacts under `build/production-release/20260907-security/`. The candidate and counts
below are the earlier optimization baseline, not the final security-hardened artifact.

Status: **Verified release candidate, awaiting official signing.** All selected automated
gates pass on the tested environments. Earlier database, UI, and lifecycle failures were
diagnosed and addressed. This is not a guarantee of zero defects, zero leaks in every
workload, or validation of every provider and physical device.

Candidate: `build/production-release/20260907/StreamVault-1.0.17.1-rc-20260907-unsigned.apk`

The bundle includes `manifest.json` with hashes, R8 mapping, native symbols, and the FFmpeg
notice. The APK is unsigned and cannot be installed or published until signed with the
official project key. Its source includes uncommitted working-tree changes.

## Retained changes

- Split the 24 legacy DAOs into six domain files and shared projections, preserving their
  package names, SQL, transaction bodies, and public APIs. Updated the source-based
  playback compatibility test to read the moved DAO.
- Enabled strict Gradle configuration caching. Property files use tracked providers;
  the signing-certificate digest uses a `ValueSource`. Beta CI passes a positive UTC
  millisecond `buildTimestamp` property; local builds without it use `0L`.
- Made the lint-baseline and FFmpeg verification tasks configuration-cache compatible.
- Exported the verified FFmpeg AAR from `:player`; the artifact filename follows the
  Media3 version catalog. Removed the app's raw cross-module filesystem dependency.
- Removed 35 forced-null assertions in the targeted browse, parser, hydration, and backup
  paths, leaving 32 production assertions for further review. The XMLTV regression test
  checks invalid entries followed by valid entries in all three parser APIs.
- Narrowed XMLTV date/time exception handling. The remaining three comment-only broad
  catches found in `:data` explain why persisted reminder state permits reconciliation.
- Added player instrumentation tests for native FFmpeg loading and MPEG Layer II support.
- Fixed raw catalog imports that omitted non-null movie/series cache defaults, and added
  transactional, unambiguous legacy series identity backfill without rebinding known IDs.
- Added schema v78 with an index-only adjacent migration matching series freshness sort
  directions. Fields, foreign keys, and historical schema artifacts are unchanged.
- Fixed heap-confirmed live-audio callback retention after terminal release, detached the
  video-frame listener before player disposal, and gave shared audio sinks explicit,
  idempotent playback-thread release ownership. Release order/failure paths have unit tests.
- Added LeakCanary-backed lifecycle tests and opt-in sustained HLS capture tests. Debug
  activities and test entry points are absent from release builds.
- Corrected invalid test fixtures: missing relational parents, provider-scoped identities,
  favorite ownership drift, Stalker KEEP versus append behavior, D-pad input, and required
  Watch Next program type. Production constraints and scheduling policies were preserved.
- Added a repeatable Windows validation script with separate ordinary app and Android
  quota-exhaustion stages, tracked test arguments, and per-stage report preservation.
- Pinned R8 to `8.13.19` using its documented plugin-management override. The release
  mapping header confirms this compiler version. Kotlin metadata warnings disappeared.

AGP remains `8.10.1`, Gradle `8.12`, and Kotlin `2.2.0`. A trial of AGP `8.13.2` / Gradle
`8.13` resolved the metadata issue but also exposed 11 unrelated lint failures and changed
baseline matching, so those version edits were removed. No new lint suppressions were
added. The later v78 migration is distinct from the mechanically verified DAO split.

R8 override reference:
https://r8.googlesource.com/r8/+/refs/heads/main/README.md#replacing-r8-in-android-gradle-plugin

## Build and JVM verification

The required baseline passed on the retained configuration:

```powershell
.\gradlew.bat testDebugUnitTest :app:lintDebug :data:lintDebug :player:lintDebug verifyLintBaseline koverXmlReportCi koverHtmlReportCi
```

It also passed with `:app:assembleDebug :app:assembleRelease --no-daemon --max-workers=1`.
The single-worker fresh process avoids an observed internal Kotlin/FIR lint analyzer
crash; normal builds remain parallel and cached. No lint checks were disabled. Existing
baseline ceilings remain app 1,560, data 18, player 9. Kover XML and HTML reports were
generated.

| Module | Unit tests | Failures/errors/skips |
|---|---:|---:|
| app | 328 | 0 |
| data | 1,062 | 0 |
| domain | 132 | 0 |
| player | 204 | 0 |
| Total | 1,726 | 0 |

Focused XMLTV, Stalker API, hydration coordinator, backup manager, and audio-sink release
tests passed. `:data:lowHeapBackupAdmissionTest` passed all 63 backup tests with a 128 MiB
heap. `git diff --check` passed.

## Configuration cache and signing

```powershell
.\gradlew.bat --configuration-cache :app:assembleDebug
.\gradlew.bat --configuration-cache :app:assembleDebug
.\gradlew.bat --configuration-cache :app:assembleRelease
```

The second standalone debug invocation reported `Configuration cache entry reused` and
completed in one second, with `org.gradle.configuration-cache.problems=fail` enabled.

The signing path was tested using a disposable JKS certificate, not the production key:

```powershell
.\gradlew.bat --configuration-cache :app:assembleRelease :app:generateBetaBuildConfig -PbuildTimestamp=1788739200000
```

Both invocations passed; the second reused the cache in one second. `apksigner verify`
accepted the test-signed APK. Its certificate SHA-256 matched the generated
`OFFICIAL_SIGNING_CERT_SHA256`; beta `BUILD_TIMESTAMP_UTC` matched the supplied value.
The temporary signing properties, keystore, and test-signed APK were removed, then the
normal unsigned release was rebuilt. Official-key signing is still a release gate.

## Android device verification

| Device and suite | Tests | Failures | Skips |
|---|---:|---:|---:|
| API 36 phone, all data tests | 76 | 0 | 0 |
| API 36 phone, native FFmpeg tests | 2 | 0 | 0 |
| API 36 phone, ordinary app suite including memory tests | 30 | 0 | 2 TV-only |
| API 36 phone, isolated dataSync quota test | 1 | 0 | 0 |
| API 36 TV, ordinary app suite including both launcher tests | 30 | 0 | 0 |
| API 25 TV, all data tests | 76 | 0 | 0 |
| API 25 TV, platform contracts, D-pad focus, and player memory | 10 | 0 | 0 |
| API 36 phone, updated D-pad smoke rerun | 4 | 0 | 0 |
| API 36 phone, two-minute live sessions plus post-release leaks | 2 | 0 | 0 |

These are overlapping suites, not counts of distinct tests. The data suites include all
43 migration cases, every exported historical origin, populated multi-hop fixtures, and
foreign-key/index-direction checks. The initial DAO split was separately verified to keep
49 generated Room implementations and 76 historical schema artifacts byte-identical.

The phone emulator has an ARM64 native bridge. TV emulator app builds used `-PcompatAbi`
only for x86/x86_64 testing; the release candidate contains only ARM64 and ARMv7. No physical
device or complete intermediate-API matrix was tested.

Repeat the full ARM-capable device workflow with:

```powershell
.\tools\validate-production.ps1 -DeviceSerial emulator-5556
```

The script runs ordinary app tests separately from the quota case. The latter requires
Android's `FGS_INTRODUCE_TIME_LIMITS` override and a 5,000 ms `data_sync_fgs_timeout_duration`;
running it without that harness fails for environmental reasons. Running subsequent
service recovery in the exhausted allowance also fails. The script restores the overrides
in `finally`. A known internal FIR lint crash is avoided by isolated single-worker lint.

Preserved reports under `build/production-validation/`:

- `20260907-143920/`: final baseline, lint, data/player/app/quota stages and JSON totals.
- `api25-data/`, `api25-app/`: minimum-API database and app checks.
- `tv36-app/`: full current-TV app suite with no skips.
- `live-20260907-final/`: complete live screenshots, logs, summaries, and JUnit results.

Gradle instrumentation installs and uninstalls debug/test APKs. Use dedicated test devices;
do not run this workflow against a device whose debug-app data must be preserved.

## Live playback and memory

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.streamvault.app.player.LivePlaybackValidationTest' -PlivePlaybackValidation=true -PinstrumentationTimeoutMs=240000
```

The test uses the real engine and render view inside a non-exported debug-only host,
not a mock player. External feeds require explicit opt-in. The successful run used host
GPU rendering and landscape rotation locked on the phone emulator; earlier software-GPU
and free-rotation runs were not accepted as passing evidence.

| Channel | Screenshots | Interval | Unique screenshot/video hashes | Capture window | Final media session |
|---|---:|---:|---|---:|---|
| France 24 English | 61 | 2 s | 61 / 61 | 121.078 s | PLAYING, error=null |
| France 24 French | 61 | 2 s | 61 / 61 | 120.444 s | PLAYING, error=null |

Video-region hashes exclude unrelated UI clocks. Both captures show ongoing video through
the last frame, engine READY/isPlaying=true, HLS prepare/read/first-frame markers, no fatal
error or stuck-player timeout, and no unintended MPEG-TS fallback. Sanitized logs and all
frames are preserved. Android's typed media-session API verifies the active session.

LeakCanary 2.14 first reproduced three retaining paths: the terminal live-audio callback,
a video-frame listener retained by delayed Media3 work, and an audio-output capability
callback retaining the shared sink and engine. Each was repaired and retested. The final
two live tests passed their post-release leak assertions. Separate tests pass ten repeated
create/bind/reset/rebind/release cycles and verify terminal callback rejection.

Sampled native heaps were 27,693,392 to 27,742,448 bytes (English) and 27,822,720 to
27,648,752 bytes (French). These short-run samples are supporting observations, not proof
that memory cannot grow during longer or different workloads. No whole-app, all-provider,
multi-hour leak guarantee is made. Golden captures are local smoke checks, not an approved
cross-device visual baseline.

## Artifacts

| Artifact | Bytes | Distribution status |
|---|---:|---|
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 18,104,505 | Unsigned; not distributable |

Candidate SHA-256:
`D41325BD08F2362974EDA3E3B03E212F8CE30CFF9FD964782D40508ED57538EA`

The release APK contains both `lib/arm64-v8a/libffmpegJNI.so` and
`lib/armeabi-v7a/libffmpegJNI.so`. Notice/license entries remain packaged. Neither the AAR
nor its codec manifest was modified. No ProGuard log-stripping rule was introduced.
`zipalign -c -P 16 4` passes, and every ARM64 ELF LOAD segment has at least 0x4000 alignment.
The release manifest is non-debuggable, non-test-only, minSdk 25/targetSdk 36, with no
LeakCanary or validation component. The mapping contains the new migration and cleanup
code, and no debug-only test entry points.

## Remaining release gates and backlog

- Configure the ignored signing properties with the official key, rebuild release, verify
  its signature/BuildConfig digest, and smoke-test that signed minified artifact before
  publication. Instrumentation runs above use debug builds; they do not substitute for
  the final signed-binary check. Do not send signing passwords through chat.
- Validate physical ARM TV audio, especially audible MP2 decoding, longer soak sessions,
  provider-specific authentication/recovery, and intermediate supported API levels in CI.
- Keep log stripping deferred until its own measured size comparison justifies it.
- Remaining nullability/broad-catch review, convention plugins, themed lint burndown, and
  wider dependency evaluation remain bounded follow-ups, not claimed as completed.
- Perform Priority 3 refactors in separate PRs with characterization tests, as required
  by the original checklist. No large-class refactor was combined with these changes.
- Refresh the knowledge graph once the global Graphify skill and CLI are available.
  Neither was present in this environment, and `graphify-out` does not exist here.

No commit, push, release publication, or production deployment was performed. Test-only
SDKs/emulators were added for platform validation; the official signing key was never
read or generated.