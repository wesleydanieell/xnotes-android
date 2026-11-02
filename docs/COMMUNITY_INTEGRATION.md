# Community integration

`community/xnotes` combines the current XNotes base with the changes submitted in
PRs #285, #326, #332, #333, #358 and #359 and the stacked split-view PR.

To update, merge `upstream/master`, review conflicts, and run
`:app:testDebugUnitTest :app:assembleCommunity`. Exercise tabs, split view, stylus,
canvas loading and file interactions on a separate test document before installing.

The community variant uses `com.xnotes.community`. Updating an existing installation
requires its original signing key; do not uninstall to work around a signature mismatch.
