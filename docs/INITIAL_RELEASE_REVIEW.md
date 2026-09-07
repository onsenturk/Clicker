# Initial Release Review

Date: 2026-09-07

This review supplements [OPTIMIZATION_VALIDATION.md](OPTIMIZATION_VALIDATION.md).
The earlier unsigned candidate predates these security and UI changes and must not be
used as the final release artifact. Official signing and a smoke test of the signed,
minified binary remain mandatory before distribution.

## Security Findings Addressed

- Removed the browser-based QR pairing server and its setup entry point with the
  owner's approval. This eliminates its unencrypted credential transfer and unmanaged
  accepted sockets rather than leaving a dormant unsafe listener. On-device Xtream,
  Stalker, M3U URL/file, backup import, and Jellyfin Quick Connect remain available.
- Replaced the remaining raw playback request-path log with the existing URL sanitizer.
  Regression tests cover path credentials, URL user information, query tokens, and
  authentication header values in the actual request-shape message.
- Made updater SHA-256 verification mandatory before downloading or installing an
  update. The trusted version/checksum pair is stored with the download, not taken
  from a later release check, and is excluded from portable backups.
- Added APK package-name, signing-identity, forward signer-lineage, and downgrade checks.
  Unverified or incompatible packages are rejected before requesting installation.
- Installation shares a verified app-private copy, not the mutable external download.
  Copying and hashing are bounded to 256 MiB, incomplete copies are removed, and the
  verified cache retains at most three artifacts. Android's package installer remains
  the final authority for installation and signature compatibility.

Older downloaded updates without bound integrity metadata must be downloaded again.
A release without a valid SHA-256 digest is deliberately not installable through the
in-app updater. This does not replace release-key custody or GitHub account security.

## Usability Review

Native Android screenshots and accessibility trees were inspected on the API 36 phone
emulator, including the real first-run, provider setup, imported public M3U library,
and search screens. This was a developer workflow review, not a study with end users.

Implemented improvements:

- Source selection scrolls vertically in the wide setup layout and horizontally in
  the narrow layout. Jellyfin and Restore are no longer stranded below a short viewport.
- Provider input shells have accessible labels and are exercised through touch on
  phones and D-pad activation on TV. Tests verify complete rapid URL entry.
- Short phone screens use compact search controls, omitting the duplicate status panel
  and introductory copy. The tested control block fits within 190 dp while retaining
  search input and content-type filters. TV keeps its existing layout.

Observed behavior:

- Manual import of the public French M3U playlist completed through the normal form.
- Searching that provider for `France` returned 15 live results.
- The compact search exposes result cards in the first phone viewport. The real
  channel menu successfully toggled from Add to Favorites to Remove from Favorites
  and back; the dialog stays open while its action label updates.
- Advanced setup options are collapsed by default; existing non-default edit settings
  can still expand them. The earlier expanded screenshot was not a new-default defect.
- The top navigation scrolls, but the clipped final item is not an obvious cue on a
  phone. A compact primary navigation plus an overflow menu would be easier to scan.
- The app remains landscape-first and TV-oriented. Portrait phone navigation, fewer
  top-level choices, and more visible favourite actions are recommended follow-ups.

An asleep, discharging emulator initially prevented both activity-hosted and isolated
Compose tests from seeing the UI. After restoring emulator AC power and waking it,
the unchanged isolated tests passed. The retained test uses the production setup screen
and real dependencies with explicit view-model cleanup. Fast ADB text injection once
dropped characters; the instrumentation rapid-input test passed, so no speculative
text-field state rewrite was made.

## Verification

- Full unit baseline: 1,739 tests, zero failures (app 339, data 1,062, domain 132, player 206).
- Focused API 36 phone run: eight setup, compact-search, and real-APK verification tests,
  zero failures/skips.
- Current TV run: 37 ordinary app tests passed before adding the phone-only compact
  search test, including launcher, player-memory, setup, and APK-verification coverage.
- Five real-APK tests exercise valid installed APKs, tampered downloads, other packages,
  malformed APKs, private-copy mutation, and FileProvider output.
- CI shell-helper tests passed.
- Local Gitleaks 8.30.1 scanned the changed, non-ignored files with no secrets found.
  The scanner binary was verified against its published SHA-256. GitHub MCP secret
  scanning was unavailable because Advanced Security is not enabled for this repository.

The final production workflow passed in `build/production-validation/20260907-173639/`:

| Gate | Result |
|---|---|
| Unit tests | 1,739 passed, zero failures |
| Debug lint and baseline ratchet | No new issues, no baseline increase |
| Coverage and unsigned release build | Passed |
| API 36 database instrumentation | 76 passed |
| API 36 native-player instrumentation | 2 passed |
| API 36 phone app instrumentation | 38 cases, zero failures, 2 TV-only skips |
| API 36 isolated foreground-service quota | 1 passed; overrides restored |
| API 25 update security and setup | 7 passed, zero skips |
| Final ARM release ZIP alignment | 16 KiB alignment passed |

The final app run includes the new update, setup, compact-search, and player lifecycle
tests. The two live two-minute captures documented in OPTIMIZATION_VALIDATION.md are
earlier evidence for the unchanged player-retention fixes, not a new soak of this APK.
All 312 obsolete pairing resource definitions were removed from default and translated
resources after lint identified the orphaned strings. The update metadata's required
runtime-only backup classification was also added and its regression test passes.
The staged check removed inherited trailing spaces from VOD SQL strings. All five
affected movie/episode DAO tests passed during this cleanup; both APKs are rebuilt
after the final formatting pass.

Preserved final artifacts under `build/production-release/20260907-security/`:

| Artifact | Distribution status |
|---|---|
| StreamVault-1.0.17.1-security-debug.apk | Locally signed debug build, testing only |
| StreamVault-1.0.17.1-security-unsigned.apk | Unsigned release, official signing required |

The bundle's generated `manifest.json` records each final artifact's size and SHA-256,
alongside the committed source revision. Recompute those hashes before distribution.

The debug APK is for testing only. The release APK is unsigned, version name `1.0.17.1`,
version code `19`, and database version `78`. Release version numbering was not changed.
Screenshots and full reports are retained locally under `build/production-validation/`;
ignored builds, scanner binaries, test data, and signing properties are not committed.
Graphify refresh remains unavailable: this environment has no graph output, global
Graphify skill, or CLI. No production release has been published by this work.

## Release Priorities

### Required Before Distribution

1. Configure the official signing key outside source control, build the intended
   release version, verify its certificate and digest, and smoke-test that exact APK.
   No production key has been generated or substituted by this work.
2. Test a physical target phone/ARM TV with the intended provider: login, import, search,
   favourite/unfavourite, channel switching, background/resume, audio, and a multi-hour
   playback soak. Include audible MP2 on hardware and one real signed-update round trip.
3. Confirm release version numbering, published SHA-256 metadata, rollback strategy,
   support contact, privacy disclosures, and the redistribution/licensing notices.
4. Keep the plaintext pairing feature disabled. Reintroduce it only with authenticated
   encrypted transport, bounded requests/connections, explicit cancellation, and tests.

### Recommended Next Implementations

1. Simplify phone navigation: primary destinations with an obvious overflow, clearer
   access to favourites, and a portrait-friendly layout while preserving TV D-pad flow.
2. Add visible per-item favourite actions for touch users rather than relying on
   discovery of long-press menus; validate with first-time users before expanding scope.
3. Extend the Stalker-style explicit transport policy to legacy HTTP-capable providers
   and remove unused trust-all TLS paths after compatibility characterization. HTTP
   provider traffic remains unencrypted in this release; prefer HTTPS providers.
4. Automate signed-binary upgrade/migration and longer playback/memory tests on physical
   target devices, plus the intermediate supported Android API matrix.
5. Add dependency advisory monitoring/security CI and continue bounded nullability and
   lint cleanup. Keep the large-class refactors in separate characterized changes.

Passing these tests does not prove zero leaks, zero vulnerabilities, or compatibility
with every provider and device. The earlier three player-retention fixes have passing
LeakCanary evidence; this review does not claim a whole-app, multi-hour memory guarantee.