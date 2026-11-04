# Build Variants & Signing Policy

## Purpose

Define a clear build strategy for development/testing and production release, aligned with Google Play package registration and key ownership requirements.

## Why we keep a `dev` flavor

`dev` is used for development and QA testing:
- Uses a non-production package suffix (`.dev`).
- Can be installed alongside production builds on the same device.
- Reduces risk of accidentally testing/releasing with the real package identity.

## Why `prod` must use the real release key

`prod` is the production track:

- Uses the real package name ( ex : `com.anrstudio.template`).
- Both `prodDebug` and `prodRelease` must be signed with the real release key owned by the team.
- Ensures package ownership continuity and valid update chain for real users.

## Google Play policy alignment

For package registration/verification, Google Play requires proof of private key ownership, and for existing package names the uploaded APK must be signed with the corresponding private key.

Reference:
- [Registering Android package names - Play Console Help](https://support.google.com/googleplay/android-developer/answer/16761053)

## Team convention in this project

- `dev*` variants: for development and testing only.
- `prod*` variants: for real package-name flows.
- `prodDebug` and `prodRelease` are both signed with the real release key.
- Public distribution artifacts must come from `prodRelease`.
