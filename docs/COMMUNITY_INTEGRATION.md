# Community integration

`community/n-notes` combines the reviewed n-notes base with this fork's features.
The customizable toolbar workspace and text rendering adaptations need device QA.

To update: fetch `NeDDy3z/n-notes` master, merge it into this branch, resolve conflicts
without removing fork features, then run `:app:testDebugUnitTest :app:assembleCommunity`.
Review the diff and exercise notes, canvases, tabs, split readers, sync and toolbar
gestures on a separate test document before installing. Push only after validation.

The community variant uses `com.neddy.nnotes.community`, separate from n-notes.
Updating an existing installation requires its original signing key; never uninstall
it to work around a signature mismatch.
