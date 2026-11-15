package com.xnotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import com.xnotes.settings.CornerStyle
import com.xnotes.ui.theme.Palette
import com.xnotes.ui.theme.XnotesTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
                if (edge.vertical) bar.center.y else bar.center.x
            }
            assertTrue("floating=$floating edge=$edge: $centers",
                centers.getValue(ToolbarDockAlignment.START) < centers.getValue(ToolbarDockAlignment.CENTER) &&
                    centers.getValue(ToolbarDockAlignment.CENTER) < centers.getValue(ToolbarDockAlignment.END))
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
                        look = ToolbarLook(), colorCount = 5) {
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
                        expandedHeight = 300.dp, modifier = Modifier.testTag("corner-surface"),
                    )
                }
            }
        }
        fun sample(xDp: Int, yDp: Int): Color {
            val bitmap = rule.onNodeWithTag("corner-surface", useUnmergedTree = true)
                .captureToImage()
            val x = (bitmap.width * xDp / 196).coerceIn(0, bitmap.width - 1)
            val y = (bitmap.height * yDp / 300).coerceIn(0, bitmap.height - 1)
            return bitmap.toPixelMap()[x, y]
        }
        rule.waitForIdle()
        val sharp5 = sample(5, 5)
        rule.runOnIdle { corners = CornerStyle.ROUNDED }
        rule.waitForIdle()
        val rounded5 = sample(5, 5)
        val rounded11 = sample(11, 11)
        rule.runOnIdle { corners = CornerStyle.SOFT }
        rule.waitForIdle()
        val soft11 = sample(11, 11)
        assertTrue("Rounded corners must change the floating tabbar outline",
            sharp5 != rounded5)
        assertTrue("Soft corners must have a larger tabbar radius than Rounded",
            rounded11 != soft11)
    }

    @Test fun previewWithIndependentNavigationDoesNotReplaceToolbarControls() {
        val config = DocumentTabsBarConfig(floating = true, anchor = ToolbarAnchor.TOP_RIGHT,
            position = ToolbarPosition.TOP)
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
        rule.onNodeWithTag("preview-bar-primary", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("document-tabs-bar", useUnmergedTree = true).assertExists()
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
