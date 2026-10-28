# Codex instructions — XNotes customizable toolbars

This directory is an independent clone of the original XNotes Android repository. It is the
dedicated workspace for the toolbar project, not the XNotes Community or tabs/split-view work.
Read `docs/toolbar-implementation-guide.md` **completely**,
`docs/toolbar-task-state.md`, and `docs/toolbar-adb-test-playbook.md` before editing code.

## Repository and scope

- `upstream` = `https://github.com/shardulvs/xnotes-android.git` (read-only source).
- `origin` = `git@github.com:wesleydanieell/xnotes-android.git` (user's fork).
- Integration branch: `feat/customizable-toolbars`. It carries the existing prototype history.
- Do not edit another clone, change the user's default build, or merge unrelated PRs here.
- Keep branch-specific debug ID `com.xnotes.toolbarsdebug` so it coexists with other builds.
  Never commit the local signing key or its secrets.

## Autonomous work loop

1. Inspect Git status, user changes, `docs/toolbar-task-state.md`, the guide, and relevant code.
2. Select the first **unblocked, not-done** task in dependency order. Do not declare a task done
   based only on compilation; check every acceptance criterion and regression requirement.
3. Implement the smallest coherent change, add/adjust tests, run build and relevant checks.
4. Review diff for unintended changes, update task state and relevant architecture decisions.
5. Create a focused Conventional Commit. Use `feat(toolbar):`, `fix(toolbar):`,
   `refactor(toolbar):`, `test(toolbar):`, or `docs(toolbar):` as appropriate.
6. Repeat while user authorization, test environment and work session permit.
7. Report exactly what passed, what failed, and which task is next. Never claim an unrun
   emulator/physical-device test passed.

Do not loop indefinitely on a failing approach: after three meaningful attempts, record the
repro, diagnosis, blockers and a different proposed approach in task state. Do not add hacks.
Do not seek arbitrary permissions, bypass protection, overwrite credentials or delete data.

## Branch/PR rules

- Keep prototype commits intact; do not force-push/rewrite published history.
- For upstream PRs, extract narrowly scoped changes onto **fresh branches from
  `upstream/master`**, using cherry-pick or small patches, not the entire integration branch.
- Aim for focused PRs; avoid unrelated changes and massive >2k-line PRs. For UI migrations that
  cannot reasonably be smaller, document why and identify independently reviewable commits.
- Tests and migration support belong with the corresponding feature.
- Push only to the configured user fork when appropriate; never push to upstream.
- NEVER create, edit, comment on, or merge pull requests automatically, even if a milestone
  passes. PR submission and maintenance are exclusively manual by the user.
- Do not tag releases or delete branches without separate explicit authorization.

## Quality gates

- `git diff --check`
- `ANDROID_HOME=/home/wesley/Android/Sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`
- Verify note and canvas modes; default one-bar, multi-bar, 4 docked and 9 floating anchors.
- Manually check drawing taps, pen-down first stroke, split view, orientation, and overlaps.
- Use an Android emulator for desktop smoke tests; prefer the Redmi Pad 2 for pen-latency QA.
- Keep the currently installed XNotes applications and user notes untouched.

When external approvals, signing, hardware access, a missing design decision, or a destructive
operation block the task, stop safely and ask. This file is an execution guide, not permission
to bypass the user's authorization.
