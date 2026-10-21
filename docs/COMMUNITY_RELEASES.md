# Community development and releases

This is an **unofficial community build** of [XNotes](https://github.com/shardulvs/xnotes-android). The original MIT license and attribution remain intact. Releases here are not published or endorsed by the upstream maintainer.

## Branches

- `master`: stable community releases. Only merge tested integration changes here.
- `develop`: integration branch for features and fixes. Pull requests to this fork should normally target this branch.
- `feat/*` and `pr/*`: isolated work, including contributions intended for upstream. Do not rewrite these branches when integrating them.

A typical cycle is: feature branch -> `develop` -> tests and tablet validation -> `master` -> stable release. You may also tag a tested `develop` commit for a preview release without merging it into `master`.

## CI and preview artifacts

`Community CI` runs unit tests and builds a **debug** APK on pushes to `develop`/`master` and PRs targeting them. The APK is available as a workflow artifact. Debug builds are for testing, not stable distribution: runner-generated debug signing keys can change between runs.

## Signed GitHub releases

`Community Release` is triggered by a tag matching `community-v*`. It runs tests, builds a signed `community` variant, uploads an APK and SHA-256 checksum, and creates a GitHub release. Tags containing `alpha`, `beta`, `preview`, or `rc` are marked as pre-releases.

The `community` variant uses package ID `com.xnotes.community` so it can coexist with official XNotes. Keep a backup of the **same signing keystore** for all future upgrades; changing it prevents updates over an installed release.

Before creating a release tag, configure these **GitHub Actions repository secrets**:

| Secret | Value |
| --- | --- |
| `COMMUNITY_KEYSTORE_BASE64` | Base64 of your private Android signing keystore file, as a single line |
| `COMMUNITY_STORE_PASSWORD` | Keystore password |
| `COMMUNITY_KEY_ALIAS` | Signing key alias |
| `COMMUNITY_KEY_PASSWORD` | Key password |

Never commit keystores, passwords, or `keystore.properties`. Store an offline backup of the keystore and passwords. The workflow deliberately fails if secrets are missing; it will not publish an unsigned APK as a release.

Example commands after the secrets are configured and a commit has passed CI:

```sh
# Preview from develop
git switch develop
git pull --ff-only origin develop
git tag -a community-v0.9.3-preview.1 -m 'Community preview 1'
git push origin community-v0.9.3-preview.1

# Stable release: merge develop into master, validate, then tag master.
```

## Syncing upstream

The `master` branch of this fork is **not** a mirror of upstream after community changes are merged. Fetch upstream into a dedicated integration branch, resolve conflicts, test, then merge into `develop`. Never reset your community branches to upstream or force-push feature branches used by open PRs.

```sh
git remote add upstream https://github.com/shardulvs/xnotes-android.git  # once
git fetch upstream
```

## Credits

Original XNotes by Shardul Vikram Singh and contributors. This fork contains additional community modifications. See `LICENSE` for the MIT license terms.
