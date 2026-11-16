package com.xnotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarBar
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

    private fun workspace(config: DocumentTabsBarConfig, count: Int, lengthDp: Int = 280) = ToolbarWorkspace(
        List(count) { lane ->
            val placement = documentTabsToolbar(config.copy(dockLane = lane)).geometry
            ToolbarBar("tools-$lane", "Tools $lane", ToolbarLayout.DEFAULT,
                geometry = placement.withLengthDp(lengthDp, config.position.vertical))
        },
    )

    private fun bounds(tag: String): Rect = rule.onNodeWithTag(tag, useUnmergedTree = true)
        .fetchSemanticsNode().boundsInRoot

    @Test fun expandedNavigationStaysUsableAcrossOccupiedEdges() {
        var config by mutableStateOf(DocumentTabsBarConfig())
        var peerCount by mutableStateOf(0)
        var compact by mutableStateOf(false)
        rule.setContent {
            MaterialTheme {
                Box(Modifier.size(if (compact) 340.dp else 580.dp,
                    if (compact) 240.dp else 380.dp).testTag("tabs-host")) {
                    DocumentTabsHost(tabs = documents, activeUri = "one.xnote",
                        expanded = true, onExpandedChange = {}, onSelect = {}, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, config = config,
                        onConfigChange = { config = it }, workspace = workspace(config, peerCount),
                        look = ToolbarLook(), colorCount = 5, shareDockLanes = true) {}
                }
            }
        }

        val failures = mutableListOf<String>()
        for (small in listOf(false, true)) for (floating in listOf(false, true))
            for (edge in ToolbarPosition.entries) for (lane in 0..2)
                for (alignment in ToolbarDockAlignment.entries) for (peers in listOf(0, 1, 3)) {
                    rule.runOnIdle {
                        compact = small
                        peerCount = peers
                        config = DocumentTabsBarConfig(floating = floating, dockLane = lane)
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
                        "$problem small=$small floating=$floating edge=$edge lane=${lane + 1} " +
                            "alignment=$alignment peers=$peers bar=$bar host=$host",
                    )
                }
        assertTrue("432 placements, ${failures.size} failures:\n" +
            failures.groupBy { it.substringBefore(" small=") }.entries.joinToString("\n") { (reason, cases) ->
                "$reason: ${cases.size}\n" + cases.take(4).joinToString("\n")
            }, failures.isEmpty())
    }

    @Test fun floatingNavigationKeepsTheEdgeMarginBesideFullLengthDocks() {
        var edge by mutableStateOf(ToolbarPosition.TOP)
        var marginPx = 0f
        rule.setContent {
            marginPx = with(LocalDensity.current) { 8.dp.toPx() }
            MaterialTheme {
                val config = DocumentTabsBarConfig(floating = true)
                    .movedTo(edge, ToolbarDockAlignment.CENTER)
                Box(Modifier.size(580.dp, 380.dp).testTag("edge-host")) {
                    DocumentTabsHost(
                        tabs = documents, activeUri = "one.xnote", expanded = true,
                        onExpandedChange = {}, onSelect = {}, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, config = config,
                        onConfigChange = {}, workspace = workspace(config.withFloating(false), 2, 1200),
                        look = ToolbarLook(), colorCount = 5, shareDockLanes = true,
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
            assertEquals("floating margin at $position", marginPx, gap, 1.5f)
        }
    }

    @Test fun splitEditorReservesTheExpandedFloatingStripForPaneControls() {
        var expanded by mutableStateOf(true)
        rule.setContent {
            MaterialTheme {
                val config = DocumentTabsBarConfig(floating = true)
                    .movedTo(ToolbarPosition.BOTTOM, ToolbarDockAlignment.CENTER)
                DocumentTabsHost(
                    tabs = documents, activeUri = "one.xnote", expanded = expanded,
                    onExpandedChange = { expanded = it }, onSelect = {}, onClose = {},
                    onReorder = { _, _ -> }, onSplit = {}, config = config,
                    onConfigChange = {}, workspace = workspace(config.withFloating(false), 1, 1200),
                    look = ToolbarLook(), colorCount = 5, reserveExpandedFloatingSpace = true,
                ) {
                    Box(Modifier.fillMaxSize().testTag("split-pane-content"))
                }
            }
        }
        val expandedPane = bounds("split-pane-content")
        assertTrue(expandedPane.bottom <= bounds("document-tabs-bar").top + 1f)
        rule.runOnIdle { expanded = false }
        assertTrue(bounds("split-pane-content").bottom > expandedPane.bottom)
    }

    @Test fun workspacePreviewReordersNavigationWithOneOrThreeToolbars() = exerciseReordering(preview = true)

    @Test fun liveEditorReordersNavigationWithOneOrThreeToolbars() = exerciseReordering(preview = false)

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
                look = ToolbarLook(), colorCount = 5, shareDockLanes = true) {
                Column(Modifier.fillMaxSize()) {
                    ToolbarAround(
                        bar = { toolContent(workspace.bars.first()) },
                        onCover = { _, _, _, _ -> }, suppressBar = config.floating,
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

    private fun exerciseReordering(preview: Boolean) {
        var config by mutableStateOf(DocumentTabsBarConfig(floating = true))
        var peerCount by mutableStateOf(1)
        rule.setContent {
            MaterialTheme {
                val bars = workspace(config.copy(dockLane = 0), peerCount,
                    lengthDp = if (peerCount == 3) 1200 else 280)
                if (!preview) LiveWorkspace(config, bars) { config = it }
                else ToolbarWorkspacePreview(
                    workspace = bars, look = ToolbarLook(),
                    selectedId = "tools-0", colors = emptyList(), activeColorIndex = 0,
                    pageNumber = 1, pageCount = 1, zoomLabel = "100%", titleLabel = "Preview",
                    selectedEntry = null, onSelectBar = {}, onSelectEntry = { _, _, _ -> },
                    onMoveBar = { _, _, _ -> }, documentTabsConfig = config,
                    onMoveDocumentTabs = { config = it },
                )
            }
        }
        val failures = mutableListOf<String>()
        val stageTag = if (preview) "toolbar-preview-stage" else "live-document-workspace"
        val toolTag = if (preview) "preview-bar-tools-0" else "live-toolbar-slot-tools-0"
        val stage = rule.onNodeWithTag(stageTag)
        fun drag(from: Offset, to: Offset) {
            val origin = bounds(stageTag).topLeft
            stage.performTouchInput {
                down(from - origin)
                advanceEventTime(850)
                moveBy(to - from)
                advanceEventTime(48)
                up()
            }
            rule.waitForIdle()
        }
        for (floating in listOf(false, true)) for (peers in listOf(1, 3)) for (edge in ToolbarPosition.entries) {
            rule.runOnIdle {
                peerCount = peers
                config = DocumentTabsBarConfig(floating = floating,
                    sizing = DocumentTabsSizing.MANUAL, manualExtentDp = 280).movedTo(edge, ToolbarDockAlignment.CENTER)
            }
            val tabs = bounds("document-tabs-bar").center
            val tools = bounds(toolTag).center
            drag(tabs, tools)
            val movedAfterTools = rule.runOnIdle { config.afterToolbarId == "tools-0" }
            if (!movedAfterTools) rule.runOnIdle { config = config.copy(afterToolbarId = "tools-0") }
            drag(bounds(toolTag).center, bounds("document-tabs-bar").center)
            val restored = rule.runOnIdle { config.afterToolbarId == null }
            if (!preview) rule.onNodeWithTag("live-document-content").assertExists()
            if (!movedAfterTools || !restored) failures.add(
                "floating=$floating peers=$peers edge=$edge: " +
                    "tabs over tools=$movedAfterTools, tools over tabs=$restored",
            )
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
