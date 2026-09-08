# StreamVault by Onur

**Based on StreamVault**, originally developed by **David Nashash (Davidona)**.
This is an independent, non-commercial fork maintained by **Onur (onsenturk)**,
not an official release by or endorsement from the original developer.

- Fork source: <https://github.com/onsenturk/StreamVault-IPTV>
- Original project: <https://github.com/Davidona/StreamVault-IPTV>
- Original developer's support page: <https://ko-fi.com/davidona>
- Governing license: [StreamVault Source-Available License (Non-Commercial)](../LICENSE), retained unchanged.

The license requires visible in-app and documentation attribution, accessible
upstream repository and donation links, derivative identification, and publicly
available corresponding source under the same license when distributing changes.
Commercial use needs the copyright holder's prior written permission. A fork does
not transfer ownership of the original source or permit removal of these notices.

## Application Identity

| Variant | Installed package | Label |
| --- | --- | --- |
| Release | `com.onsenturk.streamvault` | StreamVault by Onur |
| Debug | `com.onsenturk.streamvault.debug` | StreamVault by Onur Debug |
| Beta | `com.onsenturk.streamvault.beta` | StreamVault by Onur |

The Kotlin namespace remains `com.streamvault.app`. It names source classes and is
not the package used by Android to distinguish installed apps. Launcher components,
FileProvider authorities, updater package checks, and diagnostics use the new
application ID. Plugin API action names and backup formats remain compatible.

App labels fall back to the same non-translated brand across the 26 supported
locales. About shows this fork's maintainer/repository first, then the original
developer, exact upstream repository, existing author profile, and donation link.
Release-page templates carry the same attribution and a source link for the build.

## Updates And Data

Stable and beta feeds point to `onsenturk/StreamVault-IPTV`. The existing
`StreamVault.apk` and `StreamVault-beta.apk` asset aliases are retained for the
release workflows; filenames do not determine Android publisher identity.
Downloaded updates still need a matching checksum, package, certificate, and
acceptable version. Do not disable those checks to install an upstream build.

The fork can coexist with the original packages, including the earlier debug
APKs produced in this workspace. It starts with its own database, preferences,
credentials, and app-private storage. Export a backup from the old app, restore it
in the fork, and verify the imported providers before uninstalling anything.
Private downloaded files and recordings are not automatically transferred, and
some providers may require signing in again. Backups can contain credentials;
keep them private and never commit them.

Google Drive sign-in requires OAuth Android clients for the fork's actual
package/certificate combinations. See [Google Drive setup](GOOGLE_DRIVE_SETUP.md).
No OAuth account, Play Console entry, or GitHub release is created by these code
changes.

## Signing And Android Trust

GitHub ownership and attribution strings do not control Android's installer trust.
A fork can cause an update-install conflict if it shares a package name with an
app signed by a different key. The separate package avoids that conflict, but does
not bypass Play Protect, unknown-source permission, or a managed-device policy.

Debug builds use this machine's Android debug certificate. Production requires
the maintainer's own permanent signing key and secure offline backup of it. The
existing build reads an ignored `keystore.properties` with `storeFile`,
`storePassword`, `keyAlias`, and `keyPassword`; do not place those values in chat
or version control. Key creation/import and release distribution are separate
owner-approved actions, not part of the rebrand.

The in-app build verification compares the fork package with its configured
release certificate. It does not certify the app to Android or Google. A missing
release signing setup continues to show verification unavailable, and release
assembly remains unsigned rather than substituting a debug key.

For installation failures, distinguish unknown-source permission, a Play Protect
scan/warning, an existing-package signature conflict, and administrator policy.
Keep Play Protect enabled. A permanent signing key alone does not guarantee the
warning disappears; Google Play internal testing is a supported distribution
route for testing under the maintainer's developer account.

## Local Validation (2026-09-08)

Validation was performed before commit or push, on top of the dependency upgrades.
No keys, remote repositories, OAuth accounts, or release publications were created
or changed during validation. The original license and both previously provided APKs are unchanged.

- App JVM suite: 341 tests, zero failures or errors.
- API 36 phone app suite: 40 cases, zero failures or errors, two expected TV-only skips.
- API 36 TV identity and player smoke suite: six tests, zero failures, errors, or skips.
- The fork and `com.streamvault.app.debug` installed together successfully on the
	dedicated phone emulator. The fork launcher resolved correctly, and its
	FileProvider authority belongs to the new, non-exported package provider.
- Device tests verified the brand and preserved upstream links in all 26 app
	locales, and activated the fork repository, original repository, and donation
	links using touch on phone and D-pad on TV. About screenshots were reviewed.
- Normal ARM-only debug and fully minified unsigned release assembly passed.
	App lint reported no new issues; baseline files and ceilings are unchanged.
	Both APKs pass 16 KiB ZIP alignment, contain both ARM decoder libraries and
	native license notices, and show the correct name in all 87 packaged label
	configurations. The debug APK's single-signer v2 signature verifies.
- Smoke workflow/diagnostic helper tests and PowerShell syntax checks passed.
	Existing editor warnings about unconfigured release-signing secrets remain;
	production signing is still an owner setup step.

Evidence is preserved under `build/fork-identity-validation/`, including
`app-jvm`, `phone-final`, `tv`, `final-build.log`, and `final-lint.log`. The earlier
multi-channel live soak belongs to the preceding dependency upgrade described
in [DEPENDENCY_UPGRADE_VALIDATION.md](DEPENDENCY_UPGRADE_VALIDATION.md); it was
not repeated for this identity-only change. No physical phone was modified.
The task's test emulators were stopped. Graphify refresh remains unavailable
because the global skill/CLI and graph output are absent in this environment.

[Branded debug test APK](../build/phone-test/StreamVault-by-Onur-1.0.17.1-debug-20260908.apk):
51,873,752 bytes; SHA-256
`5146E30F320CBB34D7E47112DC5A4B2D413E61761DA91F2368D234E0C725A2EC`.

[Unsigned release APK](../build/fork-identity-validation/artifacts/StreamVault-by-Onur-1.0.17.1-unsigned.apk):
18,118,446 bytes; SHA-256
`2285605E0490C201A00BDE76950760352F2168495C63D637832F473ECB458A0F`.
The matching R8 mapping is stored alongside it. The release APK is not
installable until signed. The debug APK is independently installable but may
still trigger Android source/trust warnings; the exact warning on the user's
phone has not been provided, so that device-specific issue is not confirmed fixed.