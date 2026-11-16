package com.xnotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarGeometry
import com.xnotes.core.tools.ToolbarEntry
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarSection
import com.xnotes.core.tools.ToolbarDockAlignment
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.core.workspace.NoteTabs
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.DocumentTabsSizing
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DocumentTabsPlacementRegressionTest {
    @get:Rule val rule = createComposeRule()

    private val documents = NoteTabs().open("one.xnote", "One")
        .open("two.xcanvas", "Two").open("three.xnote", "Three")

    private fun workspace(config: DocumentTabsBarConfig, count: Int, lengthDp: Int = 280, rows: Int = 1) = ToolbarWorkspace(
        List(count) { lane ->
            val placement = ToolbarGeometry(anchor = DocumentTabsBarConfig.anchorOf(config.position),
                floating = config.floating, dockEdge = config.position.dockEdge(), dockLane = lane)
            val layout = ToolbarLayout(listOf(ToolbarSection(
                List(rows) { row -> ToolbarEntry(ToolbarItem.PEN, lane = row) }, lanes = rows,
            )))
            ToolbarBar("tools-$lane", "Tools $lane", layout,
                geometry = placement.withLengthDp(lengthDp, config.position.vertical))
        },
    )

    private fun bounds(tag: String): Rect = rule.onNodeWithTag(tag, useUnmergedTree = true)
        .fetchSemanticsNode().boundsInRoot

    @Test fun expandedNavigationStaysUsableAcrossOccupiedEdges() {
        var config by mutableStateOf(DocumentTabsBarConfig())
        var peerCount by mutableStateOf(0)
        var rows by mutableStateOf(1)
        var compact by mutableStateOf(false)
        rule.setContent {
            MaterialTheme {
                Box(Modifier.size(if (compact) 340.dp else 580.dp,
                    if (compact) 240.dp else 380.dp).testTag("tabs-host")) {
                    DocumentTabsHost(tabs = documents, activeUri = "one.xnote",
                        expanded = true, onExpandedChange = {}, onSelect = {}, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, config = config,
                        onConfigChange = { config = it }, workspace = workspace(config, peerCount, rows = rows),
                        look = ToolbarLook(), colorCount = 5) {}
                }
            }
        }

        val failures = mutableListOf<String>()
        for (small in listOf(false, true)) for (floating in listOf(false, true))
            for (edge in ToolbarPosition.entries)
                for (alignment in ToolbarDockAlignment.entries) for (peers in listOf(0, 1, 3))
                    for (toolbarRows in 1..3) {
                    rule.runOnIdle {
                        compact = small
                        peerCount = peers
                        rows = toolbarRows
                        config = DocumentTabsBarConfig(floating = floating)
                            .movedTo(edge, alignment)
                    }
                    val host = bounds("tabs-host")
                    val bar = bounds("document-tabs-bar")
                    val length = if (edge.vertical) bar.height else bar.width
                    val depth = if (edge.vertical) bar.width else bar.height
                    val edgeLength = if (edge.vertical) host.height else host.width
                    val crossLength = if (edge.vertical) host.width else host.height
                    val gap = when (edge) {
                        ToolbarPosition.TOP -> bar.top - host.top
                        ToolbarPosition.BOTTOM -> host.bottom - bar.bottom
                        ToolbarPosition.LEFT -> bar.left - host.left
                        ToolbarPosition.RIGHT -> host.right - bar.right
                    }
                    val problem = when {
                        bar.left < host.left || bar.top < host.top ||
                            bar.right > host.right || bar.bottom > host.bottom -> "clipped"
                        depth <= 0 || length <= 2 * depth -> "collapsed"
                        !floating && length < edgeLength - 1 -> "incomplete dock"
                        gap >= crossLength / 2 -> "past the middle of host"
                        else -> null
                    }
                    if (problem != null) failures.add(
                        "$problem small=$small floating=$floating edge=$edge " +
                            "alignment=$alignment peers=$peers rows=$toolbarRows bar=$bar host=$host",
                    )
                }
        assertTrue("432 placements, ${failures.size} failures:\n" +
            failures.groupBy { it.substringBefore(" small=") }.entries.joinToString("\n") { (reason, cases) ->
                "$reason: ${cases.size}\n" + cases.take(4).joinToString("\n")
            }, failures.isEmpty())
    }

    @Test fun floatingNavigationClearsFullLengthDocksOnEveryEdge() {
        var edge by mutableStateOf(ToolbarPosition.TOP)
        rule.setContent {
            MaterialTheme {
                val config = DocumentTabsBarConfig(floating = true)
                    .movedTo(edge, ToolbarDockAlignment.CENTER)
                Box(Modifier.size(580.dp, 380.dp).testTag("edge-host")) {
                    DocumentTabsHost(
                        tabs = documents, activeUri = "one.xnote", expanded = true,
                        onExpandedChange = {}, onSelect = {}, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, config = config,
                        onConfigChange = {}, workspace = workspace(config.withFloating(false), 2, 1200),
                        look = ToolbarLook(), colorCount = 5,
                    ) {}
                }
            }
        }
        for (position in ToolbarPosition.entries) {
            rule.runOnIdle { edge = position }
            val host = bounds("edge-host")
            val bar = bounds("document-tabs-bar")
            val gap = when (position) {
                ToolbarPosition.TOP -> bar.top - host.top
                ToolbarPosition.BOTTOM -> host.bottom - bar.bottom
                ToolbarPosition.LEFT -> bar.left - host.left
                ToolbarPosition.RIGHT -> host.right - bar.right
            }
            val length = if (position.vertical) host.width else host.height
            assertTrue("floating clearance at $position: $gap", gap in 85f..length / 2f)
        }
    }

    @Test fun floatingTabsKeepBothSplitPanesStationaryAndUseTheFullWidthForCenter() {
        var expanded by mutableStateOf(false)
        var edge by mutableStateOf(ToolbarPosition.TOP)
        rule.setContent {
            MaterialTheme {
                val config = DocumentTabsBarConfig(floating = true)
                    .movedTo(edge, ToolbarDockAlignment.CENTER)
                Box(Modifier.fillMaxSize().testTag("split-host")) {
                    DocumentTabsHost(
                        tabs = documents, activeUri = "one.xnote", expanded = expanded,
                        onExpandedChange = { expanded = it }, onSelect = {}, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, config = config,
                        onConfigChange = {}, workspace = workspace(config.withFloating(false), 1, 1200),
                        look = ToolbarLook(), colorCount = 5,
                        splitToolbars = DocumentTabsSplitToolbars(
                            first = workspace(config.withFloating(false), 1, 1200),
                            second = ToolbarWorkspace(emptyList()), sideBySide = true, ratio = 0.35f,
                        ),
                    ) {
                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(0.35f).fillMaxHeight().testTag("split-left"))
                            Box(Modifier.weight(0.65f).fillMaxHeight().testTag("split-right"))
                        }
                    }
                }
            }
        }
        val left = bounds("split-left")
        val right = bounds("split-right")
        for (position in listOf(ToolbarPosition.TOP, ToolbarPosition.BOTTOM)) {
            for (isExpanded in listOf(false, true)) {
                rule.runOnIdle { edge = position; expanded = isExpanded }
                assertEquals(left, bounds("split-left"))
                assertEquals(right, bounds("split-right"))
                assertEquals(bounds("split-host").center.x, bounds("document-tabs-bar").center.x, 1.5f)
            }
        }
    }

    @Test fun floatingDropRecognizesTheSecondPaneToolbar() {
        var config by mutableStateOf(DocumentTabsBarConfig(floating = true))
        val empty = ToolbarWorkspace(emptyList())
        val bottomDock = workspace(DocumentTabsBarConfig(floating = false)
            .movedTo(ToolbarPosition.BOTTOM, ToolbarDockAlignment.CENTER), 1, 1200)
        rule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize().testTag("split-stage")) {
                    DocumentTabsHost(
                        tabs = documents, activeUri = "one.xnote", expanded = true,
                        onExpandedChange = {}, onSelect = {}, onClose = {}, onReorder = { _, _ -> },
                        onSplit = {}, config = config, onConfigChange = { config = it },
                        workspace = empty, look = ToolbarLook(), colorCount = 5,
                        splitToolbars = DocumentTabsSplitToolbars(empty, bottomDock, false, 0.35f),
                    ) {}
                }
            }
        }
        val viewport = bounds("split-stage")
        val origin = bounds("document-tabs-bar").center - viewport.topLeft
        val stage = rule.onNodeWithTag("split-stage")
        stage.performTouchInput {
            down(origin)
            advanceEventTime(850)
            moveBy(Offset(viewport.width / 2, viewport.height * 0.85f) - origin)
        }
        rule.onNodeWithTag("document-tabs-drop-outline").assertDoesNotExist()
        stage.performTouchInput { up() }
        assertEquals(ToolbarPosition.TOP, rule.runOnIdle { config.position })
    }

    @Test fun movingAndReorderingToolbarsInSplitKeepsTheOtherPaneAndTabsStable() {
        val config = DocumentTabsBarConfig(floating = false)
        val bottom = config.movedTo(ToolbarPosition.BOTTOM, ToolbarDockAlignment.CENTER)
        val primary = workspace(bottom, 1, 132, rows = 1).bars.single().copy(id = "split-moving")
        val peer = workspace(bottom, 1, 132, rows = 2).bars.single().copy(id = "split-target")
        var first by mutableStateOf(ToolbarWorkspace(listOf(primary, peer)))
        val secondBar = workspace(config, 1, 132, rows = 3).bars.single().copy(id = "split-other")
        val second = ToolbarWorkspace(listOf(secondBar))
        rule.setContent {
            MaterialTheme {
                DocumentTabsHost(
                    tabs = documents, activeUri = "one.xnote", expanded = true,
                    onExpandedChange = {}, onSelect = {}, onClose = {}, onReorder = { _, _ -> },
                    onSplit = {}, config = config, onConfigChange = {},
                    workspace = first, look = ToolbarLook(), colorCount = 5,
                    splitToolbars = DocumentTabsSplitToolbars(first, second, true, 0.5f),
                ) {
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            ToolbarAround(
                                bar = { Box(Modifier.size(132.dp, 52.dp)) },
                                onCover = { _, _, _, _ -> }, workspace = first,
                                dockedContent = { Box(Modifier.size(132.dp, 52.dp)) },
                                onMoveBar = { id, anchor, alignment ->
                                    first = first.copy(bars = first.bars.map { bar ->
                                        if (bar.id != id) bar else bar.copy(geometry = bar.geometry
                                            .movedTo(anchor, floating = false, fromEdge = bar.geometry.dockEdge)
                                            .copy(dockAlignment = alignment))
                                    })
                                },
                                onReorderBar = { from, to -> first = first.reorderVisualBar(from, to) },
                            ) { _ -> Box(Modifier.fillMaxSize()) }
                        }
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            ToolbarAround(
                                bar = { Box(Modifier.size(132.dp, 52.dp)) },
                                onCover = { _, _, _, _ -> }, workspace = second,
                                dockedContent = { Box(Modifier.size(132.dp, 52.dp)) },
                                onMoveBar = { _, _, _ -> },
                            ) { _ -> Box(Modifier.fillMaxSize()) }
                        }
                    }
                }
            }
        }
        val moving = rule.onNodeWithTag("toolbar-drag-surface-split-moving", useUnmergedTree = true)
        val start = moving.fetchSemanticsNode().boundsInRoot.center
        val target = bounds("live-toolbar-slot-split-target").center
        val other = bounds("live-toolbar-slot-split-other")
        val tabs = bounds("document-tabs-bar")
        moving.performTouchInput {
            down(center)
            advanceEventTime(790)
            moveBy(target - start)
            advanceEventTime(48)
        }
        val indicator = rule.onNodeWithTag("toolbar-drop-indicator-split-moving", useUnmergedTree = true)
        indicator.assertExists()
        assertEquals(other, bounds("live-toolbar-slot-split-other"))
        assertEquals(tabs, bounds("document-tabs-bar"))
        moving.performTouchInput { up() }
        assertEquals(listOf("split-target", "split-moving"), rule.runOnIdle {
            first.bars.sortedBy { it.geometry.visualOrder }.map { it.id }
        })
        indicator.assertDoesNotExist()
        assertEquals(other, bounds("live-toolbar-slot-split-other"))
        assertEquals(tabs, bounds("document-tabs-bar"))

        val afterReorder = moving.fetchSemanticsNode().boundsInRoot.center
        moving.performTouchInput {
            down(center)
            advanceEventTime(790)
            moveBy(Offset(5f, (tabs.bottom + other.bottom + afterReorder.y) / 3f) - afterReorder)
            advanceEventTime(48)
            up()
        }
        assertEquals(ToolbarPosition.LEFT.dockEdge(), rule.runOnIdle {
            first.bars.first { it.id == "split-moving" }.geometry.dockEdge
        })
        assertEquals(other, bounds("live-toolbar-slot-split-other"))
        assertEquals(tabs, bounds("document-tabs-bar"))
    }

    @Test fun workspacePreviewSeparatesTabsFromMovedAndResizedToolbar() =
        exerciseToolbarCollision(preview = true)

    @Test fun liveEditorSeparatesTabsFromMovedAndResizedToolbar() =
        exerciseToolbarCollision(preview = false)

    private fun exerciseToolbarCollision(preview: Boolean) {
        val tabs = DocumentTabsBarConfig(floating = true)
        var edge by mutableStateOf(ToolbarPosition.BOTTOM)
        var length by mutableStateOf(132)
        rule.setContent {
            MaterialTheme {
                val dock = tabs.withFloating(false).movedTo(edge, ToolbarDockAlignment.CENTER)
                val bars = workspace(dock, 1, lengthDp = length, rows = 2)
                if (!preview) LiveWorkspace(tabs, bars) {}
                else ToolbarWorkspacePreview(
                    workspace = bars, look = ToolbarLook(), selectedId = "tools-0",
                    colors = emptyList(), activeColorIndex = 0, pageNumber = 1,
                    pageCount = 1, zoomLabel = "100%", titleLabel = "Preview",
                    selectedEntry = null, onSelectBar = {}, onSelectEntry = { _, _, _ -> },
                    onMoveBar = { _, _, _ -> }, documentTabsConfig = tabs,
                )
            }
        }
        val toolbarTag = if (preview) "preview-bar-tools-0" else "live-toolbar-slot-tools-0"
        val initial = bounds("document-tabs-bar")
        rule.runOnIdle { edge = ToolbarPosition.TOP }
        assertTrue(!bounds("document-tabs-bar").overlaps(bounds(toolbarTag)))
        rule.runOnIdle { length = 1200 }
        assertTrue(!bounds("document-tabs-bar").overlaps(bounds(toolbarTag)))
        rule.runOnIdle { edge = ToolbarPosition.BOTTOM }
        assertEquals(initial, bounds("document-tabs-bar"))
    }

    @Test fun workspacePreviewShowsOutlineOnlyDuringDrag() = exerciseDrag(preview = true)

    @Test fun liveEditorShowsOutlineOnlyDuringDrag() = exerciseDrag(preview = false)

    @Composable
    private fun LiveWorkspace(
        config: DocumentTabsBarConfig,
        workspace: ToolbarWorkspace,
        onConfigChange: (DocumentTabsBarConfig) -> Unit,
    ) {
        val toolContent: @Composable (ToolbarBar) -> Unit = { bar ->
            ToolbarFrame(armed = null, layout = bar.layout, geometry = bar.geometry) {
                ToolbarSections(bar.layout) { _, section ->
                    ToolbarSectionGrid(section) { Box(Modifier.size(42.dp)) }
                }
            }
        }
        Box(Modifier.fillMaxSize().testTag("live-document-workspace")) {
            DocumentTabsHost(tabs = documents, activeUri = "one.xnote",
                expanded = true, onExpandedChange = {}, onSelect = {}, onClose = {},
                onReorder = { _, _ -> }, onSplit = {}, config = config,
                onConfigChange = onConfigChange, workspace = workspace,
                look = ToolbarLook(), colorCount = 5) {
                Column(Modifier.fillMaxSize()) {
                    ToolbarAround(
                        bar = { toolContent(workspace.bars.first()) },
                        onCover = { _, _, _, _ -> }, suppressBar = workspace.bars.first().geometry.floating,
                        workspace = workspace, dockedContent = toolContent,
                        onMoveBar = { _, _, _ -> }, onReorderBar = { _, _ -> },
                    ) { floatingBar ->
                        Box(Modifier.fillMaxSize().testTag("live-document-content")) {
                            floatingBar()
                            WorkspaceFloatingBars(workspace, onMoveBar = { _, _, _ -> },
                                primaryContent = { toolContent(workspace.bars.first()) },
                                content = toolContent, onReorderBar = { _, _ -> })
                        }
                    }
                }
            }
        }
    }

    private fun exerciseDrag(preview: Boolean) {
        var config by mutableStateOf(DocumentTabsBarConfig(floating = false))
        var workspaceConfig by mutableStateOf(DocumentTabsBarConfig(floating = false))
        rule.setContent {
            MaterialTheme {
                val bars = workspace(workspaceConfig, 1, lengthDp = 1200)
                if (!preview) LiveWorkspace(config, bars) { config = it }
                else ToolbarWorkspacePreview(
                    workspace = bars, look = ToolbarLook(), selectedId = "tools-0",
                    colors = emptyList(), activeColorIndex = 0, pageNumber = 1,
                    pageCount = 1, zoomLabel = "100%", titleLabel = "Preview",
                    selectedEntry = null, onSelectBar = {}, onSelectEntry = { _, _, _ -> },
                    onMoveBar = { _, _, _ -> }, documentTabsConfig = config,
                    onMoveDocumentTabs = { config = it },
                )
            }
        }
        val stageTag = if (preview) "toolbar-preview-stage" else "live-document-workspace"
        val stage = rule.onNodeWithTag(stageTag)
        val outline = rule.onNodeWithTag("document-tabs-drop-outline")
        val dragSurface = "toolbar-drag-surface-document-tabs-navigation"
        fun startDrag() {
            val viewport = bounds(stageTag)
            val origin = bounds("document-tabs-bar").center - viewport.topLeft
            stage.performTouchInput {
                down(origin)
                advanceEventTime(850)
                moveBy(Offset(viewport.width / 2, viewport.height * 0.75f) - origin)
            }
        }
        outline.assertDoesNotExist()
        val original = bounds("document-tabs-bar")
        val before = stage.captureToImage().toPixelMap()
        startDrag()
        outline.assertExists()
        val dragged = bounds(dragSurface)
        assertTrue(dragged.top > original.bottom)
        assertTrue(dragged.bottom <= bounds(stageTag).bottom)
        assertEquals(ToolbarPosition.TOP, rule.runOnIdle { config.position })
        val target = bounds("document-tabs-drop-outline")
        val pixel = target.center - bounds(stageTag).topLeft
        val during = stage.captureToImage().toPixelMap()
        assertEquals(before[pixel.x.toInt(), pixel.y.toInt()], during[pixel.x.toInt(), pixel.y.toInt()])
        assertEquals(original, bounds("document-tabs-bar"))
        stage.performTouchInput { cancel() }
        outline.assertDoesNotExist()
        assertEquals(original, bounds("document-tabs-bar"))

        startDrag()
        outline.assertExists()
        val beforeSettle = bounds(dragSurface)
        rule.mainClock.autoAdvance = false
        try {
            stage.performTouchInput { up() }
            rule.mainClock.advanceTimeBy(100)
            outline.assertDoesNotExist()
            val settling = bounds(dragSurface)
            assertTrue(settling.top > beforeSettle.top && settling.top < target.top)
        } finally {
            rule.mainClock.autoAdvance = true
        }
        assertEquals(ToolbarPosition.BOTTOM, rule.runOnIdle { config.position })
        assertEquals(target, bounds("document-tabs-bar"))
        assertEquals(target, bounds(dragSurface))

        val editorBounds = bounds(stageTag)
        stage.performTouchInput {
            down(target.center - editorBounds.topLeft)
            advanceEventTime(850)
            moveBy(Offset(editorBounds.left + 4f, editorBounds.center.y) - target.center)
        }
        val verticalTarget = bounds("document-tabs-drop-outline")
        val beforeMorph = bounds(dragSurface)
        rule.mainClock.autoAdvance = false
        try {
            stage.performTouchInput { up() }
            rule.mainClock.advanceTimeBy(100)
            val intermediate = bounds(dragSurface)
            assertTrue(intermediate.width < beforeMorph.width && intermediate.width > verticalTarget.width)
            assertTrue(intermediate.height > beforeMorph.height && intermediate.height < verticalTarget.height)
            assertEquals(ToolbarPosition.BOTTOM, rule.runOnIdle { config.position })
            outline.assertDoesNotExist()
        } finally {
            rule.mainClock.autoAdvance = true
        }
        assertEquals(ToolbarPosition.LEFT, rule.runOnIdle { config.position })
        assertEquals(verticalTarget, bounds(dragSurface))

        rule.runOnIdle {
            config = DocumentTabsBarConfig(floating = true)
            workspaceConfig = workspaceConfig.movedTo(ToolbarPosition.BOTTOM, ToolbarDockAlignment.CENTER)
        }
        val floating = bounds("document-tabs-bar")
        startDrag()
        outline.assertDoesNotExist()
        stage.performTouchInput { up() }
        assertEquals(ToolbarPosition.TOP, rule.runOnIdle { config.position })
        assertEquals(floating, bounds("document-tabs-bar"))
    }
}