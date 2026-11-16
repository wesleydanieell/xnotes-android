package com.xnotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import com.xnotes.R
import com.xnotes.settings.CornerStyle
import com.xnotes.ui.theme.Palette
import com.xnotes.ui.theme.XnotesTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarAnchor
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

class DocumentTabsAdvancedToolbarComposeTest {
    @get:Rule val rule = createComposeRule()

    private val workspace = ToolbarWorkspace(listOf(
        ToolbarBar("primary", "Primary toolbar", ToolbarLayout.DEFAULT),
    ))
    private val tabs = NoteTabs().open("n1.xnote", "Note One")
        .open("n2.xnote", "Note Two").open("n3.xcanvas", "Canvas Three")

    @Test fun floatingBarCanDockOnAllFourEdgesWithoutHidingEditor() {
        var config by mutableStateOf(DocumentTabsBarConfig())
        rule.setContent {
            MaterialTheme {
                Box(Modifier.size(900.dp, 500.dp).testTag("navigation-stage")) {
                    DocumentTabsHost(tabs = tabs, activeUri = "n1.xnote", expanded = false,
                        onExpandedChange = {}, onSelect = {}, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, config = config,
                        onConfigChange = { config = it }, workspace = workspace,
                        look = ToolbarLook(), colorCount = 5) {
                        Box(Modifier.testTag("document-editor-content"))
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("document-tabs-bar", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Hold to move document tabs", useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithTag("document-editor-content", useUnmergedTree = true).assertExists()
        for (edge in ToolbarPosition.entries) {
            rule.runOnIdle {
                config = DocumentTabsBarConfig(floating = false,
                    position = edge, anchor = DocumentTabsBarConfig.anchorOf(edge))
            }
            rule.waitForIdle()
            val host = rule.onNodeWithTag("navigation-stage", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            val bar = rule.onNodeWithTag("document-tabs-bar", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            assertTrue("Navigation must remain inside host when docked on $edge: $bar / $host",
                bar.left >= host.left - 3f && bar.top >= host.top - 3f &&
                    bar.right <= host.right + 3f && bar.bottom <= host.bottom + 3f)
            rule.onNodeWithTag("document-editor-content", useUnmergedTree = true).assertExists()
            assertEquals(edge, config.position)
        }
    }

    @Test fun documentTabsEditorUsesPositionMapForBothPlacementModes() {
        var paneWidth by mutableStateOf(520.dp)
        var config by mutableStateOf(DocumentTabsBarConfig())
        rule.setContent {
            MaterialTheme {
                Box(Modifier.width(paneWidth)) {
                    DocumentTabsEditor(config, "auto", { config = it }, {})
                }
            }
        }
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val positionLabel = rule.onNodeWithText(resources.getString(R.string.toolbar_config_position_and_alignment))
        val sizeLabel = rule.onNodeWithText(resources.getString(R.string.document_tabs_expanded_size))
        assertEquals(positionLabel.fetchSemanticsNode().boundsInRoot.top,
            sizeLabel.fetchSemanticsNode().boundsInRoot.top, 1f)
        rule.runOnIdle { paneWidth = 320.dp }
        assertTrue(sizeLabel.fetchSemanticsNode().boundsInRoot.top >
            positionLabel.fetchSemanticsNode().boundsInRoot.bottom)

        for (floating in listOf(true, false)) {
            rule.runOnIdle { config = config.withFloating(floating) }
            for (edge in ToolbarPosition.entries) for (alignment in ToolbarDockAlignment.entries) {
                val stop = rule.onNodeWithTag("toolbar-position-${edge.name}-${alignment.name}")
                stop.performClick()
                stop.assertIsSelected()
                rule.runOnIdle {
                    assertEquals(edge, config.position)
                    assertEquals(alignment, config.alignment())
                    assertEquals(floating, config.floating)
                }
            }
        }
    }

    @Test fun automaticVerticalBarUsesAvailableHeight() {
        var selectedUri: String? = null
        var config by mutableStateOf(DocumentTabsBarConfig().movedTo(ToolbarAnchor.CENTER_LEFT))
        rule.setContent {
            MaterialTheme {
                Box(Modifier.size(640.dp, 420.dp).testTag("tab-sizing-host")) {
                    DocumentTabsHost(tabs = tabs, activeUri = "n1.xnote", expanded = true,
                        onExpandedChange = {}, onSelect = { selectedUri = it }, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, config = config,
                        onConfigChange = {}, workspace = ToolbarWorkspace(emptyList()),
                        look = ToolbarLook(), colorCount = 5) {
                        Box(Modifier.fillMaxSize().testTag("tab-sizing-editor"))
                    }
                }
            }
        }
        val bar = rule.onNodeWithTag("document-tabs-bar", useUnmergedTree = true)
        for (floating in listOf(true, false)) {
            rule.runOnIdle {
                config = config.withFloating(floating).copy(sizing = DocumentTabsSizing.DYNAMIC, maxExtentDp = 0)
            }
            bar.assertHeightIsEqualTo(if (floating) 372.dp else 420.dp)
                .assertWidthIsEqualTo(if (floating) 44.dp else 48.dp)
            val host = rule.onNodeWithTag("tab-sizing-host").fetchSemanticsNode().boundsInRoot
            val editor = rule.onNodeWithTag("tab-sizing-editor").fetchSemanticsNode().boundsInRoot
            val reserved = (if (floating) 0f else 48f) * rule.density.density
            assertEquals(reserved, editor.left - host.left, 1f)
            rule.onNodeWithText("Note One").performTouchInput { click(center) }
            rule.runOnIdle {
                assertEquals("n1.xnote", selectedUri)
                selectedUri = null
            }
            rule.runOnIdle {
                config = config.copy(minExtentDp = 80, maxExtentDp = 140)
            }
            bar.assertHeightIsEqualTo(if (floating) 140.dp else 420.dp)
            rule.runOnIdle {
                config = config.copy(sizing = DocumentTabsSizing.MANUAL,
                    manualExtentDp = 300, maxExtentDp = 0)
            }
            bar.assertHeightIsEqualTo(if (floating) 300.dp else 420.dp)
        }
    }

    @Test fun placementHonorsAllEdgesAndAlignmentsInBothModes() {
        var config by mutableStateOf(DocumentTabsBarConfig(
            sizing = DocumentTabsSizing.MANUAL, manualExtentDp = 192,
        ))
        rule.setContent {
            MaterialTheme {
                Box(Modifier.size(640.dp, 420.dp).testTag("placement-host")) {
                    DocumentTabsHost(tabs = tabs, activeUri = "n1.xnote", expanded = true,
                        onExpandedChange = {}, onSelect = {}, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, config = config,
                        onConfigChange = { config = it }, workspace = ToolbarWorkspace(emptyList()),
                        look = ToolbarLook(), colorCount = 5) {}
                }
            }
        }

        val host = rule.onNodeWithTag("placement-host").fetchSemanticsNode().boundsInRoot
        for (floating in listOf(false, true)) for (edge in ToolbarPosition.entries) {
            val centers = ToolbarDockAlignment.entries.associateWith { alignment ->
                rule.runOnIdle { config = config.withFloating(floating).movedTo(edge, alignment) }
                val bar = rule.onNodeWithTag("document-tabs-bar", useUnmergedTree = true)
                    .fetchSemanticsNode().boundsInRoot
                val label = "floating=$floating edge=$edge alignment=$alignment"
                assertTrue("$label: $bar outside $host",
                    bar.left >= host.left && bar.top >= host.top &&
                        bar.right <= host.right && bar.bottom <= host.bottom)
                val edgeGap = when (edge) {
                    ToolbarPosition.TOP -> bar.top - host.top
                    ToolbarPosition.BOTTOM -> host.bottom - bar.bottom
                    ToolbarPosition.LEFT -> bar.left - host.left
                    ToolbarPosition.RIGHT -> host.right - bar.right
                }
                val crossSpan = if (edge.vertical) host.width else host.height
                assertTrue("$label: $bar is on the wrong edge of $host", edgeGap < crossSpan / 4)
                val thickness = if (edge.vertical) bar.width else bar.height
                val length = if (edge.vertical) bar.height else bar.width
                assertTrue("$label: expected a narrow strip, got $bar", length > 3 * thickness)
                if (!floating) {
                    val edgeLength = if (edge.vertical) host.height else host.width
                    assertEquals("$label: docked tabs must span the whole edge", edgeLength, length, 1f)
                }
                if (edge.vertical) bar.center.y else bar.center.x
            }
            if (floating) {
                assertTrue("Floating $edge must follow alignment: $centers",
                    centers.getValue(ToolbarDockAlignment.START) < centers.getValue(ToolbarDockAlignment.CENTER) &&
                        centers.getValue(ToolbarDockAlignment.CENTER) < centers.getValue(ToolbarDockAlignment.END))
            } else {
                val center = if (edge.vertical) host.center.y else host.center.x
                centers.values.forEach { assertEquals(center, it, 1f) }
            }
        }
    }

    @Test fun longPressDragChangesNavigationEdgeWithoutChangingToolbars() {
        var config by mutableStateOf(DocumentTabsBarConfig())
        rule.setContent {
            MaterialTheme {
                Box(Modifier.size(900.dp, 500.dp).testTag("navigation-stage")) {
                    DocumentTabsHost(tabs = tabs, activeUri = "n1.xnote", expanded = false,
                        onExpandedChange = {}, onSelect = {}, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, config = config,
                        onConfigChange = { config = it }, workspace = workspace,
                        look = ToolbarLook(position = ToolbarPosition.LEFT), colorCount = 5) {
                        Box(Modifier.testTag("document-editor-content"))
                    }
                }
            }
        }
        rule.waitForIdle()
        val stage = rule.onNodeWithTag("navigation-stage", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        fun moveTo(destination: Offset) {
            val navigation = rule.onNodeWithTag("toolbar-drag-surface-document-tabs-navigation",
                useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            rule.onNodeWithTag("navigation-stage", useUnmergedTree = true).performTouchInput {
                down(navigation.center - stage.topLeft)
                advanceEventTime(850)
                moveBy(destination - navigation.center)
                advanceEventTime(48)
                up()
            }
            rule.waitForIdle()
        }

        for (floating in listOf(true, false)) {
            rule.runOnIdle { config = DocumentTabsBarConfig(floating = floating) }
            moveTo(Offset(stage.right - 8f, stage.top + stage.height / 5))
            rule.runOnIdle {
                assertEquals(ToolbarPosition.RIGHT, config.position)
                assertEquals(ToolbarAnchor.TOP_RIGHT, config.anchor)
                assertEquals(ToolbarDockAlignment.START, config.alignment())
            }
            moveTo(Offset(stage.right - 8f, stage.bottom - stage.height / 5))
            rule.runOnIdle {
                assertEquals(ToolbarPosition.RIGHT, config.position)
                assertEquals(ToolbarAnchor.BOTTOM_RIGHT, config.anchor)
                assertEquals(ToolbarDockAlignment.END, config.alignment())
                assertEquals("primary", workspace.bars.single().id)
            }
        }
    }

    @Test fun floatingNavigationOutlineFollowsGlobalCornersForAllThreeModes() {
        var corners by mutableStateOf(CornerStyle.SHARP)
        rule.setContent {
            XnotesTheme(Palette.DEFAULT, corners) {
                Box(Modifier.size(560.dp, 420.dp).background(Color.Magenta)) {
                    FloatingDocumentTabs(
                        tabs = tabs, activeUri = "n1.xnote", expanded = true,
                        onExpandedChange = {}, onSelect = {}, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, vertical = true,
                        expandedWidth = 300.dp, modifier = Modifier.testTag("corner-surface"),
                    )
                }
            }
        }
        fun sample(xDp: Int, yDp: Int): Color {
            val bitmap = rule.onNodeWithTag("corner-surface", useUnmergedTree = true)
                .captureToImage()
            val x = (bitmap.width * xDp / 44).coerceIn(0, bitmap.width - 1)
            val y = (bitmap.height * yDp / 300).coerceIn(0, bitmap.height - 1)
            return bitmap.toPixelMap()[x, y]
        }
        rule.waitForIdle()
        val sharpNearCorner = sample(3, 3)
        rule.runOnIdle { corners = CornerStyle.ROUNDED }
        rule.waitForIdle()
        val roundedNearCorner = sample(3, 3)
        val roundedFartherIn = sample(6, 6)
        rule.runOnIdle { corners = CornerStyle.SOFT }
        rule.waitForIdle()
        val softFartherIn = sample(6, 6)
        assertTrue("Rounded corners must change the floating tabbar outline",
            sharpNearCorner != roundedNearCorner)
        assertTrue("Soft corners must have a larger tabbar radius than Rounded",
            roundedFartherIn != softFartherIn)
    }

    @Test fun dockedNavigationReservesSpaceWithoutDisposingEditor() {
        var canvasDisposals = 0
        var config by mutableStateOf(DocumentTabsBarConfig(floating = false))
        rule.setContent {
            MaterialTheme {
                Box(Modifier.size(640.dp, 420.dp)) {
                    DocumentTabsHost(tabs = tabs, activeUri = "n1.xnote", expanded = false,
                        onExpandedChange = {}, onSelect = {}, onClose = {},
                        onReorder = { _, _ -> }, onSplit = {}, config = config,
                        onConfigChange = {}, workspace = workspace, look = ToolbarLook(),
                        colorCount = 5) {
                        Column(Modifier.fillMaxSize()) {
                            ToolbarAround(
                                bar = { Box(Modifier.fillMaxSize().testTag("live-primary")) },
                                onCover = { _, _, _, _ -> }, workspace = workspace,
                                dockedContent = {},
                            ) {
                                DisposableEffect(Unit) { onDispose { canvasDisposals++ } }
                                Box(Modifier.fillMaxSize().testTag("live-canvas"))
                            }
                        }
                    }
                }
            }
        }
        val nav = rule.onNodeWithTag("document-tabs-bar", useUnmergedTree = true)
        val primary = rule.onNodeWithTag("live-primary", useUnmergedTree = true)
        val canvas = rule.onNodeWithTag("live-canvas", useUnmergedTree = true)
        assertTrue(nav.fetchSemanticsNode().boundsInRoot.bottom <= primary.fetchSemanticsNode().boundsInRoot.top)
        rule.runOnIdle { config = config.movedTo(ToolbarPosition.BOTTOM, ToolbarDockAlignment.CENTER) }
        assertTrue(canvas.fetchSemanticsNode().boundsInRoot.bottom <= nav.fetchSemanticsNode().boundsInRoot.top)
        canvas.assertExists()
        rule.runOnIdle { assertEquals(0, canvasDisposals) }
    }

    @Test fun previewWithIndependentNavigationDoesNotReplaceToolbarControls() {
        var config by mutableStateOf( DocumentTabsBarConfig(floating = true, anchor = ToolbarAnchor.TOP_RIGHT,
            position = ToolbarPosition.TOP))
        rule.setContent {
            MaterialTheme {
                ToolbarWorkspacePreview(
                    workspace = workspace, look = ToolbarLook(), selectedId = "primary",
                    colors = emptyList(), activeColorIndex = 0, pageNumber = 1,
                    pageCount = 1, zoomLabel = "100%", titleLabel = "Preview",
                    selectedEntry = null, onSelectBar = {}, onSelectEntry = { _, _, _ -> },
                    onMoveBar = { _, _, _ -> }, documentTabsConfig = config,
                )
            }
        }
        rule.waitForIdle()
        val primary = rule.onNodeWithTag("preview-bar-primary", useUnmergedTree = true)
        val navigation = rule.onNodeWithTag("document-tabs-bar", useUnmergedTree = true)
        val floatingPrimary = primary.fetchSemanticsNode().boundsInRoot
        navigation.assertExists()

        rule.runOnIdle { config = config.withFloating(false) }
        val dockedPrimary = primary.fetchSemanticsNode().boundsInRoot
        val dockedTabs = navigation.fetchSemanticsNode().boundsInRoot
        assertTrue("The workspace toolbar must move below docked tabs",
            dockedPrimary.top > floatingPrimary.top && dockedPrimary.top >= dockedTabs.bottom)
        rule.onNodeWithText("XNote").assertExists()
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        rule.onNodeWithContentDescription(resources.getString(R.string.document_tabs_collapse), useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithContentDescription(resources.getString(R.string.document_tabs_expand), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test fun collapsedNavigationExpandsWithoutDuplicatingToolbars() {
        var expanded by mutableStateOf(false)
        rule.setContent {
            MaterialTheme {
                DocumentTabsHost(tabs = tabs, activeUri = "n1.xnote", expanded = expanded,
                    onExpandedChange = { expanded = it }, onSelect = {}, onClose = {},
                    onReorder = { _, _ -> }, onSplit = {}, config = DocumentTabsBarConfig(),
                    onConfigChange = {}, workspace = workspace,
                    look = ToolbarLook(), colorCount = 5, modifier = Modifier.size(900.dp, 500.dp)) {
                    Box(Modifier.testTag("document-editor-content"))
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("document-tabs-bar", useUnmergedTree = true).assertExists()
        // Clicking the compact navigation expands the same independent surface.
        rule.onNodeWithTag("document-tabs-bar", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        assertTrue(expanded)
        rule.onNodeWithTag("document-tabs-bar", useUnmergedTree = true).assertExists()
    }
}
