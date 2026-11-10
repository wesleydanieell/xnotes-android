# XNotes screen recordings

Recorded on a Redmi Pad 2 running Android 16, using isolated builds of the exact feature branches in [manifest.json](manifest.json). The demonstrations use blank test documents. Community packages and personal notes were not used.

Each animated preview links to its MP4 recording. The GIF previews run at 8 fps; the H.264 MP4 files retain the device recording and are remuxed for streaming, with metadata-only tracks removed. There are no cuts, generated scenes or playback-speed changes.

## Toolbar workspaces

The recording opens the dedicated configurator, switches to the element inspector and changes the primary toolbar from docked to floating.

[![Toolbar workspaces recording](videos/toolbar-workspaces.gif)](videos/toolbar-workspaces.mp4)

[Watch the MP4](videos/toolbar-workspaces.mp4)

## Optional Pen Group

The recording opens pen details, selects a different family and opens the hold fan.

[![Pen Group recording](videos/pen-group.gif)](videos/pen-group.mp4)

[Watch the MP4](videos/pen-group.mp4)

## Shared split tools

The recording switches the mixed note/canvas interface and changes shared tools to tools per pane.

[![Shared split tools recording](videos/shared-split-tools.gif)](videos/shared-split-tools.mp4)

[Watch the MP4](videos/shared-split-tools.mp4)

## Reproduction

Video fixtures are under `video-capture/`. The three recording scenarios passed on the tablet; summaries are under `validation/`. Feature code was unchanged. Source commits and video metadata/hashes are recorded in `manifest.json`.

With JDK 17 and the Android SDK configured, from the feature worktree set `media` to this directory. The matching isolated main APK must be installed. For the toolbar example:

```sh
adb push "$media/video-capture/start-recording.sh" /data/local/tmp/xnotes-pr-start-recording.sh
./gradlew --max-workers=2 -Pandroid.injected.build.abi=arm64-v8a -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process -I "$media/video-capture/capture.gradle" -PreviewSuffix=.prtoolbar -PreviewSources="$media/video-capture/toolbar" :app:assembleDebugAndroidTest
adb install -r -t app/build/intermediates/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.xnotes.ui.ReviewVideoTest com.xnotes.prtoolbar.test/android.test.InstrumentationTestRunner
adb pull /data/local/tmp/xnotes-pr-video-431.mp4
```

For Pen Group use `pen`, suffix `.prpen`, package `com.xnotes.prpen` and recording ID `429`. For shared tools use `shared`, `.sharedsplit`, `com.xnotes.sharedsplit` and `430`. The recorder starts after the test document is visible and stops before the activity finishes. Only the recorder PID started by the fixture receives its stop signal.

Earlier static captures and fixtures remain in the branch history and original paths for provenance. Active PR descriptions now use the recordings. The comprehensive edge/rotation/gesture matrix remains pending; the recordings demonstrate specific configurations and do not constitute performance benchmarks.
