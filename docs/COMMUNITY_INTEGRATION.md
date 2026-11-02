# Community integration

`community/xnotes` combines the latest reviewed XNotes base with this fork's features.
The customizable toolbar workspace is a prototype; gesture and split-screen QA is pending.

To update: fetch `upstream/master`, merge it into this branch, resolve conflicts without
removing community features, and run `:app:testDebugUnitTest :app:assembleCommunity`.
Review the diff and exercise notes, canvases, tabs, split view and toolbar gestures on a
separate test document before installing. Push only after validation.

The community variant uses `com.xnotes.community`. Updating an existing installation
requires its original signing key; never uninstall it to work around a signature mismatch.
