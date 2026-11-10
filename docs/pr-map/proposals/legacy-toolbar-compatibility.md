# Preserve saved standalone toolbar tools after native tool grouping

Branch: `nnotes/pr/legacy-toolbar-compatibility`
Base: NeDDy3z/n-notes/master (`199918c5`).

The new grouped PEN/SELECT defaults remove standalone toolbar IDs. Retain the IDs for users with explicitly saved layouts, hide them in fresh defaults, and preserve their existing tool bindings in both note and canvas toolbars. New layouts continue using native grouped controls.

This is a small fork-specific compatibility fix; it does not re-port the XNotes customization stack or replace n-notes native grouping.

Validation: JVM suite and debug Kotlin compilation passed, including two regression tests for grouped defaults and explicit legacy entries. See [validation](../validation.json). Device QA remains pending.
