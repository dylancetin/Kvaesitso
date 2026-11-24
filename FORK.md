# Kvaesitso fork

This is a personal fork of [Kvaesitso](https://github.com/MM2-0/Kvaesitso). The fork's own
commits sit on top of upstream `main`, and an automated job rebases them onto upstream once a week.

## What the fork changes

- **Turkish-aware contact search**: contact matching follows Turkish letter rules (such as
  `I/ı` and `İ/i`).
- **Contact results show the company** where upstream shows the phone number.
- **Side-by-side build**: the `fork` build type uses the package `de.mm20.launcher2.fork` and the
  label "Kvaesitso Fork" (with a purple icon background), so it installs next to the official app
  (`de.mm20.launcher2.release`) instead of replacing it.

## Install with Obtainium

Add the repository URL `https://github.com/dylancetin/Kvaesitso` in
[Obtainium](https://github.com/ImranR98/Obtainium), or open this link on the phone:
`obtainium://add/https://github.com/dylancetin/Kvaesitso`

Each release has a single APK, `kvaesitso-fork-<version>.apk`. Releases are normal (not
pre-releases), so the default Obtainium settings pick them up.

Version scheme: `versionName` is the upstream version plus `-fork.<YYYYMMDDHH>` (UTC build hour),
for example `1.41.0-fork.2026112418`. The git tag and release title are `v<versionName>`, and
`versionCode` is the `YYYYMMDDHH` number, so every build is newer than the last one.

## Build locally

Signed with the fork key, which can update a release installed through Obtainium:

```sh
set -a; . ~/.config/kvaesitso-fork/signing.env; set +a
./gradlew assembleDefaultFork
# -> app/app/build/outputs/apk/default/fork/app-default-fork.apk
```

`signing.env` provides `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS` and
`SIGNING_KEY_PASSWORD`.

Unsigned (debug key): run `./gradlew assembleDefaultFork` without those variables. The build falls
back to the debug signing key. Android won't install that APK over a release-signed one, so
uninstall first or use it only for testing.

`FORK_BUILD_ID=<YYYYMMDDHH>` overrides the build id. By default it is the current UTC hour.

## Automation

- `.github/workflows/fork-release.yml` runs on every push to `main`, except changes to only docs or
  Markdown, and on manual dispatch. It builds `assembleDefaultFork`, signs it with the repository
  secrets `KEYSTORE` (base64 of the keystore), `KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS` and
  `SIGNING_KEY_PASSWORD`, then publishes a GitHub release. A manual run with `publish` unchecked,
  or a run on any branch other than `main`, only uploads the APK as a workflow artifact.
- `.github/workflows/upstream-sync.yml` runs weekly (and on demand). It runs
  `git rebase upstream/main`, then force-pushes `main` with `--force-with-lease`, which starts a
  release build. If the rebase conflicts, it aborts and opens (or comments on) an issue labelled
  `upstream-sync-conflict` that lists the conflicting files and the upstream commit range.
  Resolve it locally with `git rebase upstream/main` and force-push. The job needs the `SYNC_TOKEN`
  secret: a fine-grained PAT with Contents and Workflows write access, because `GITHUB_TOKEN`
  cannot push commits that touch `.github/workflows`.

Upstream's own workflows (nightly build, docs deployment, F-Droid trigger) are disabled in this
fork. Their files are left unchanged so rebases stay clean.

## Back up the signing key

Releases are signed with the keystore in `~/.config/kvaesitso-fork/`. **Back it up, along with its
passwords.** If it is lost, no future build can update the installed app: Android rejects updates
signed with a different key. You would have to uninstall the app, losing launcher settings unless
you have a backup, and reinstall.
