package com.xnotes.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import com.xnotes.core.model.Orientation
import com.xnotes.core.model.PageSize
import com.xnotes.core.model.Rgba
import com.xnotes.settings.ExplorerLayout
import com.xnotes.core.tools.InkPalette
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarDockAlignment
import com.xnotes.core.tools.ToolbarDockEdge
import com.xnotes.core.tools.ToolbarDistribution
import com.xnotes.core.tools.ToolbarEntry
import com.xnotes.core.tools.ToolbarRegion
import com.xnotes.core.tools.ToolbarGeometry
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarSection
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.core.util.NameTemplate
import com.xnotes.settings.Preferences
import com.xnotes.settings.MaterialStyle
import com.xnotes.settings.CornerStyle
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize
import com.xnotes.settings.MaterialColourMode
import com.xnotes.settings.SwatchRole
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlinx.coroutines.delay

/** One-tap filename templates offered beside the free-text field. */
private val NAME_TEMPLATE_PRESETS = listOf(
    NameTemplate.DEFAULT,
    "note_YYYY-MM-DD",
    "note_YYYY-MM-DD_HH-mm",
)
private val penButtonOptions = listOf("eraser" to R.string.tool_eraser, "pan" to R.string.tool_pan, "select" to R.string.tool_select, "none" to R.string.none)
private val tapGestureOptions = listOf(
    "none" to R.string.none,
    "undo" to R.string.undo,
    "redo" to R.string.redo,
    "toggle_pan" to R.string.toggle_pan,
    "toggle_eraser" to R.string.toggle_eraser,
    "toggle_previous" to R.string.toggle_previous,
)

private enum class ToolbarPropertyTab { BARS, DIMENSIONS, ELEMENTS }

/**
 * Preferences as a backstage pane (spec 10 §8). Edits apply live — each change is
 * pushed straight to the [Editor] (and persisted), so theme/page tweaks are seen
 * immediately, including in the surrounding backstage.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferencesPane(
    editor: Editor,
    compact: Boolean,
    sidebarOpen: Boolean,
    onShowSidebar: () -> Unit,
    onBackToHome: () -> Unit,
    onImportCodeTheme: () -> Unit = {},
    onImportFont: () -> Unit = {},
    onBackup: (export: Boolean) -> Unit = {},
    openToolbarEditor: Boolean = false,
) {
    val palette = LocalPalette.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val screenConfiguration = LocalConfiguration.current
    var prefs by remember { mutableStateOf(editor.preferences) }
    fun update(p: Preferences) {
        prefs = p
        editor.applyPreferences(p)
    }
    // Home, explorer and export settings leave the open note alone, so they skip its canvas refresh.
    fun updateHome(p: Preferences) {
        prefs = p
        editor.applyHomePreferences(p)
    }
    var namingColor by remember { mutableStateOf<Rgba?>(null) }
    namingColor?.let { c -> ColorNameDialog(editor, c) { namingColor = null } }
    // Follow out-of-pane preference changes too (the .scm import round-trips a picker).
    LaunchedEffect(editor.prefsVersion) { prefs = editor.preferences }

    val scrollState = rememberScrollState()
    val toolbarPropertiesScroll = rememberScrollState()
    val toolbarElementsScroll = rememberScrollState()
    var previewExpanded by remember { mutableStateOf(false) }
    var toolbarPropertyTab by remember { mutableStateOf(ToolbarPropertyTab.BARS) }
    var colorControlsExpanded by remember { mutableStateOf(false) }
    var dockError by remember { mutableStateOf<String?>(null) }
    var toolbarEditorOpen by remember { mutableStateOf(openToolbarEditor) }
    LaunchedEffect(openToolbarEditor) { if (openToolbarEditor) toolbarEditorOpen = true }
    LaunchedEffect(toolbarEditorOpen) { scrollState.scrollTo(0) }
    LaunchedEffect(toolbarPropertyTab) { toolbarPropertiesScroll.scrollTo(0) }
    // Which bar the toolbar section is arranging. The two are separate layouts, so the drag state
    // below always belongs to whichever is on screen.
    var canvasTab by remember { mutableStateOf(false) }
    var selectedBarId by remember { mutableStateOf<String?>(null) }
    var barMenuExpanded by remember { mutableStateOf(false) }
    var toolbarActionsExpanded by remember { mutableStateOf(false) }
    var transferDraft by remember { mutableStateOf<com.xnotes.core.tools.ToolbarTransferDraft?>(null) }
    var workspacesDialogOpen by remember { mutableStateOf(false) }
    var workspaceLibraryTab by remember { mutableStateOf(0) }
    var savedWorkspaces by remember { mutableStateOf(editor.toolbarWorkspacePresets) }
    var restorePending by remember { mutableStateOf<com.xnotes.settings.ToolbarWorkspacePreset?>(null) }
    var workspaceFileContent by remember { mutableStateOf("") }
    var workspaceFileMessage by remember { mutableStateOf<String?>(null) }
    val exportWorkspaceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(workspaceFileContent.toByteArray(Charsets.UTF_8))
                } ?: error("Não foi possível abrir o arquivo.")
            }.onSuccess { workspaceFileMessage = "Workspace exportado com sucesso." }
                .onFailure { workspaceFileMessage = "Falha ao exportar: ${it.message}" }
        }
    }
    val importWorkspaceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Não foi possível ler o arquivo.")
                require(raw.length <= 1_000_000) { "Arquivo muito grande." }
                val imported = com.xnotes.settings.ToolbarWorkspaceExchangeJson.import(raw)
                editor.importToolbarWorkspacePresets(imported)
            }.onSuccess { count ->
                savedWorkspaces = editor.toolbarWorkspacePresets
                workspaceFileMessage = if (count > 0) "$count workspace(s) importado(s)."
                    else "Limite de 20 workspaces salvos atingido."
            }.onFailure { workspaceFileMessage = "Falha ao importar: ${it.message}" }
        }
    }
    fun exportWorkspaceFile(presets: List<com.xnotes.settings.ToolbarWorkspacePreset>, filename: String) {
        workspaceFileContent = com.xnotes.settings.ToolbarWorkspaceExchangeJson.export(presets)
        exportWorkspaceLauncher.launch(filename)
    }
    fun activeWorkspacePreset() = com.xnotes.settings.ToolbarWorkspacePreset(
        id = "current-export",
        name = "Workspace atual",
        workspaces = editor.toolbarWorkspaces,
        look = prefs.toolbarLook,
        colors = editor.toolbarColors,
        colorCount = editor.toolbarColorCount,
    )
    var selectedEntry by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    fun editedWorkspace(): ToolbarWorkspace =
        if (canvasTab) editor.toolbarWorkspaces.canvas else editor.toolbarWorkspaces.note
    fun selectedBar(): ToolbarBar = editedWorkspace().bars.firstOrNull { it.id == selectedBarId }
        ?: editedWorkspace().bars.first()
    val layout = selectedBar().layout
    // Toolbar drag state, hoisted here so the floating "ghost" chip can be drawn at the pane
    // root (above and outside the scroll, so it is never clipped). Bounds are plain maps read
    // only at drag time; observable state is just the dragged item, finger, and drop slot.
    var dragItem by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragFinger by remember { mutableStateOf(Offset.Zero) }
    var dragGrab by remember { mutableStateOf(Offset.Zero) }
    var dropTarget by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var dragRegionTarget by remember { mutableStateOf<ToolbarRegion?>(null) }
    val chipBounds = remember { mutableMapOf<Pair<Int, Int>, Rect>() }
    val laneBounds = remember { mutableMapOf<Pair<Int, Int>, Rect>() }
    val sectionBounds = remember { mutableMapOf<Int, Rect>() }
    val sectionGrabBounds = remember { mutableMapOf<Int, Rect>() }
    val regionBounds = remember { mutableMapOf<ToolbarRegion, Rect>() }
    // Section-drag state (the whole card, by its grip handle); shares the finger/grab with the
    // chip drag since only one gesture runs at a time. The drop slot is an insertion index.
    var dragSection by remember { mutableStateOf<Int?>(null) }
    var sectionDropTarget by remember { mutableStateOf<ToolbarSectionTarget?>(null) }
    var paneTopLeft by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    fun editedLayout(): ToolbarLayout = selectedBar().layout
    fun applyEdited(next: ToolbarLayout) {
        val workspace = editedWorkspace()
        val bar = selectedBar()
        if (bar.id == workspace.bars.first().id) {
            if (canvasTab) editor.applyCanvasToolbarLayout(next) else editor.applyToolbarLayout(next)
        } else {
            val profiles = editor.toolbarWorkspaces
            val changed = workspace.replaceBar(bar.copy(layout = next))
            editor.applyToolbarWorkspaces(if (canvasTab) profiles.copy(canvas = changed) else profiles.copy(note = changed))
        }
    }
    fun updateSelectedBar(transform: (ToolbarBar) -> ToolbarBar) {
        val profiles = editor.toolbarWorkspaces
        val changed = editedWorkspace().replaceBar(transform(selectedBar()))
        editor.applyToolbarWorkspaces(if (canvasTab) profiles.copy(canvas = changed) else profiles.copy(note = changed))
    }
    fun configureDock(next: ToolbarGeometry, nextLook: com.xnotes.settings.ToolbarLook = prefs.toolbarLook) {
        val bar = selectedBar()
        val candidate = editedWorkspace().replaceBar(bar.copy(geometry = next.constrained()))
        val plan = toolbarDockPlan(
            candidate.onlyItems(if (canvasTab) ToolbarLayout.CANVAS_ITEMS else ToolbarLayout.NOTE_ITEMS),
            nextLook,
            screenConfiguration.screenWidthDp.dp,
            screenConfiguration.screenHeightDp.dp,
        )
        val floatingPlan = toolbarFloatingPlan(
            candidate,
            nextLook,
            screenConfiguration.screenWidthDp.dp - plan.leftInset.dp - plan.rightInset.dp,
            screenConfiguration.screenHeightDp.dp - plan.topInset.dp - plan.bottomInset.dp,
            editor.toolbarColorCount,
        )
        if (plan.rejected.isNotEmpty() || floatingPlan.rejected.isNotEmpty()) {
            dockError = "Sem espaço disponível. Reduza a barra, altere a borda ou escolha outra faixa."
            return
        }
        dockError = null
        updateSelectedBar { it.copy(geometry = next.constrained()) }
        if (nextLook != prefs.toolbarLook) updateHome(prefs.copy(toolbarLook = nextLook))
    }

    fun beginCopyToolbar() {
        val profiles = editor.toolbarWorkspaces
        if (canvasTab && profiles.shared) {
            dockError = "Desative o perfil compartilhado antes de copiar uma barra do XCanvas."
            return
        }
        val targetCanvas = !canvasTab
        val destination = if (targetCanvas) profiles.canvas else profiles.note
        val target = if (targetCanvas) com.xnotes.core.tools.ToolbarContext.CANVAS
            else com.xnotes.core.tools.ToolbarContext.NOTE
        val source = selectedBar()
        val initial = com.xnotes.core.tools.prepareToolbarTransfer(source, destination, target)
        if (initial.retainedCount == 0) {
            dockError = "Esta barra não possui elementos compatíveis com o editor de destino."
            return
        }
        // A primary toolbar inherits its docking from the global look. Make that placement
        // explicit when the copy becomes a secondary toolbar in the other context.
        val isPrimary = editedWorkspace().bars.first().id == source.id
        val geometry = if (isPrimary) {
            if (source.geometry.floating || prefs.toolbarLook.floating)
                source.geometry.copy(floating = true, dockEdge = null)
            else source.geometry.copy(
                dockEdge = source.geometry.dockEdge ?: prefs.toolbarLook.position.dockEdge(),
                floating = false,
            )
        } else source.geometry
        val candidates = listOf(geometry) + ToolbarAnchor.entries.filter { it != ToolbarAnchor.CENTER }
            .map { geometry.copy(floating = true, dockEdge = null, anchor = it, dockLane = 0) }
        val placed = candidates.firstOrNull { position ->
            val candidate = destination.addBar(initial.bar.copy(geometry = position))
            val docked = toolbarDockPlan(candidate, prefs.toolbarLook,
                screenConfiguration.screenWidthDp.dp, screenConfiguration.screenHeightDp.dp)
            val floated = toolbarFloatingPlan(candidate, prefs.toolbarLook,
                screenConfiguration.screenWidthDp.dp - docked.leftInset.dp - docked.rightInset.dp,
                screenConfiguration.screenHeightDp.dp - docked.topInset.dp - docked.bottomInset.dp,
                editor.toolbarColorCount)
            docked.rejected.isEmpty() && floated.rejected.isEmpty()
        }
        if (placed == null) {
            dockError = "Não há espaço para esta barra no destino. Reduza suas dimensões antes de copiar."
            return
        }
        dockError = null
        transferDraft = initial.copy(bar = initial.bar.copy(geometry = placed))
    }

    fun retarget() {
        val target = toolbarItemDropTarget(editedLayout(), dragFinger, chipBounds, laneBounds,
            sectionBounds, regionBounds)
        dropTarget = target?.slot
        dragRegionTarget = target?.region
    }
    fun retargetSection() {
        sectionDropTarget = toolbarSectionDropTarget(editedLayout(), dragFinger, sectionBounds, regionBounds)
    }
    fun showTab(canvas: Boolean) {
        if (canvasTab == canvas) return
        canvasTab = canvas
        selectedBarId = null
        selectedEntry = null
        // The bounds maps are keyed by item and the two bars share most items, so stale entries
        // from the other tab would aim a drop at a chip that is no longer there.
        chipBounds.clear()
        laneBounds.clear()
        sectionBounds.clear()
        sectionGrabBounds.clear()
        regionBounds.clear()
        dragItem = null
        dragSection = null
        dropTarget = null
        dragRegionTarget = null
        sectionDropTarget = null
    }
    fun showBar(id: String) {
        if (selectedBarId == id) return
        selectedBarId = id
        selectedEntry = null
        chipBounds.clear()
        laneBounds.clear()
        sectionBounds.clear()
        sectionGrabBounds.clear()
        regionBounds.clear()
        dragItem = null
        dragSection = null
        dropTarget = null
        dragRegionTarget = null
        sectionDropTarget = null
    }

    transferDraft?.let { draft ->
        val profiles = editor.toolbarWorkspaces
        val targetCanvas = !canvasTab
        val destination = if (targetCanvas) profiles.canvas else profiles.note
        ToolbarCopyDialog(
            targetLabel = if (targetCanvas) "XCanvas" else "XNotes",
            candidate = destination.addBar(draft.bar),
            draft = draft,
            look = prefs.toolbarLook,
            colors = editor.toolbarColors.take(editor.toolbarColorCount),
            activeColorIndex = editor.activeColorIndex,
            sharedCanvasWillDisable = targetCanvas && profiles.shared,
            onCopy = {
                val changed = destination.addBar(draft.bar)
                editor.applyToolbarWorkspaces(
                    if (targetCanvas) profiles.copy(canvas = changed, shared = false)
                    else profiles.copy(note = changed),
                )
                transferDraft = null
                showTab(targetCanvas)
                showBar(draft.bar.id)
            },
            onDismiss = { transferDraft = null },
        )
    }
    if (workspacesDialogOpen) {
        ToolbarPresetsDialog(
            presets = savedWorkspaces,
            templates = com.xnotes.settings.ToolbarWorkspaceTemplates.presets,
            statusMessage = workspaceFileMessage,
            selectedTab = workspaceLibraryTab,
            onSelectTab = { workspaceLibraryTab = it },
            onExportOne = { preset ->
                val basename = preset.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
                exportWorkspaceFile(listOf(preset), "$basename.xworkspaces.json")
            },
            onExportCurrent = { exportWorkspaceFile(listOf(activeWorkspacePreset()), "toolbar-atual.xworkspaces.json") },
            onExportAll = { exportWorkspaceFile(savedWorkspaces, "toolbars-salvas.xworkspaces.json") },
            onImport = { importWorkspaceLauncher.launch(arrayOf("application/json", "application/octet-stream")) },
            onUseTemplate = { template ->
                workspacesDialogOpen = false
                restorePending = template
            },
            onSave = { name ->
                editor.saveToolbarWorkspacePreset(name).also { saved ->
                    if (saved) savedWorkspaces = editor.toolbarWorkspacePresets
                }
            },
            onRestore = { preset ->
                workspacesDialogOpen = false
                restorePending = preset
            },
            onDelete = { id ->
                editor.deleteToolbarWorkspacePreset(id)
                savedWorkspaces = editor.toolbarWorkspacePresets
            },
            onDismiss = { workspacesDialogOpen = false },
        )
    }
    restorePending?.let { preset ->
        ToolbarPresetPreviewDialog(
            preset = preset,
            canvasInitially = canvasTab,
            onApply = { targetContext ->
                editor.applyToolbarWorkspacePreset(preset, targetContext)
                prefs = editor.preferences
                selectedBarId = null
                selectedEntry = null
                restorePending = null
            },
            onDismiss = {
                restorePending = null
                workspacesDialogOpen = true
            },
        )
    }

    // A tap on empty space dismisses the template field's focus; children consume their own taps.
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .onGloballyPositioned { paneTopLeft = it.boundsInRoot().topLeft },
    ) {
    Column(Modifier.fillMaxSize()) {
        // Leading button inline with the title, at a constant row height so toggling the sidebar never
        // shifts the settings below: a back arrow to Home on compact, else a hamburger when hidden.
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            if (toolbarEditorOpen) {
                Tooltip(stringResource(R.string.preferences)) {
                    IconButton(onClick = { toolbarEditorOpen = false }) {
                        Icon(XnotesIcons.prev, stringResource(R.string.preferences), tint = palette.text.toComposeColor(), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.pref_toolbar), color = palette.text.toComposeColor(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            } else {
                if (compact) {
                    Tooltip(stringResource(R.string.back_to_home)) {
                        IconButton(onClick = onBackToHome) {
                            Icon(XnotesIcons.prev, stringResource(R.string.back_to_home), tint = palette.text.toComposeColor(), modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                } else if (!sidebarOpen) {
                    Tooltip(stringResource(R.string.show_sidebar)) {
                        IconButton(onClick = onShowSidebar) {
                            Icon(XnotesIcons.menu, stringResource(R.string.show_sidebar), tint = palette.text.toComposeColor(), modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Text(stringResource(R.string.preferences), color = palette.text.toComposeColor(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { update(Preferences()) }) { Text(stringResource(R.string.reset_to_defaults), fontSize = 13.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(
            // Ending at the keyboard's edge scrolls a focused field up out from under it.
            Modifier.fillMaxSize().imePadding().verticalScroll(scrollState)
                .onGloballyPositioned {
                    if (!toolbarEditorOpen) viewport = it.boundsInRoot()
                },
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!toolbarEditorOpen) {
            SectionTitle(stringResource(R.string.pref_general))
            FieldLabel(stringResource(R.string.pref_ui_theme))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.theme_system), prefs.uiAppearance == "system") { update(prefs.copy(uiAppearance = "system")) }
                Chip(stringResource(R.string.theme_dark), prefs.uiAppearance == "dark") { update(prefs.copy(uiAppearance = "dark")) }
                Chip(stringResource(R.string.theme_light), prefs.uiAppearance == "light") { update(prefs.copy(uiAppearance = "light")) }
                Chip(stringResource(R.string.theme_oled), prefs.uiAppearance == "oled") { update(prefs.copy(uiAppearance = "oled")) }
            }
            val systemColours = prefs.materialMode == MaterialColourMode.SYSTEM
            FieldLabel(stringResource(R.string.material_tone))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.material_system_colours), systemColours) {
                    update(prefs.copy(materialMode = MaterialColourMode.SYSTEM))
                }
                Chip(stringResource(R.string.material_single_tone), prefs.materialMode == MaterialColourMode.SINGLE) {
                    update(prefs.copy(materialMode = MaterialColourMode.SINGLE))
                }
                Chip(stringResource(R.string.material_dual_tone), prefs.materialMode == MaterialColourMode.DUAL) {
                    update(prefs.copy(materialMode = MaterialColourMode.DUAL))
                }
            }
            if (!systemColours) {
                key(prefs.materialMode) { MaterialColourPicker(prefs, ::update) }
                CustomMaterialControls(prefs, ::update)
            } else if (Build.VERSION.SDK_INT < 31) {
                Text(stringResource(R.string.material_system_fallback), color = palette.textDim.toComposeColor(), fontSize = 12.sp)
            }
            FieldLabel(stringResource(R.string.pref_corners))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((style, label) in listOf(CornerStyle.SHARP to R.string.corners_sharp, CornerStyle.ROUNDED to R.string.corners_rounded, CornerStyle.SOFT to R.string.corners_soft)) {
                    Chip(stringResource(label), prefs.cornerStyle == style) { updateHome(prefs.copy(cornerStyle = style)) }
                }
            }
            CheckRow(stringResource(R.string.pref_start_fullscreen), editor.fullscreen) { editor.setFullscreenPref(it) }

            HorizontalDivider(color = palette.border.toComposeColor())
            SectionTitle(stringResource(R.string.pref_input))
            CheckRow(stringResource(R.string.pref_finger_draws), prefs.fingerDraws) { update(prefs.copy(fingerDraws = it)) }
            FieldLabel(stringResource(R.string.pref_zoom_lock_pan))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(stringResource(R.string.pan_single), prefs.zoomLockPan == "single") { update(prefs.copy(zoomLockPan = "single")) }
                Chip(stringResource(R.string.pan_two), prefs.zoomLockPan == "double") { update(prefs.copy(zoomLockPan = "double")) }
                Chip(stringResource(R.string.pan_none), prefs.zoomLockPan == "none") { update(prefs.copy(zoomLockPan = "none")) }
            }
            CheckRow(stringResource(R.string.pref_detect_shapes), prefs.detectShapes) { update(prefs.copy(detectShapes = it)) }
            FieldLabel(stringResource(R.string.pref_pen_button_hold))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                penButtonOptions.forEach { (id, label) ->
                    Chip(stringResource(label), prefs.penButtonTool == id) { update(prefs.copy(penButtonTool = id)) }
                }
            }
            if (prefs.penButtonTool == "eraser" || prefs.penButtonTool == "pan") {
                CheckRow(stringResource(R.string.pref_pen_button_hover), prefs.penButtonHover) {
                    update(prefs.copy(penButtonHover = it))
                }
            }
            val tapOptions = tapGestureOptions.map { (id, res) -> id to stringResource(res) }
            FieldLabel(stringResource(R.string.pref_two_finger_tap))
            OptionDropdown(tapOptions, prefs.twoFingerTap) { update(prefs.copy(twoFingerTap = it)) }
            FieldLabel(stringResource(R.string.pref_three_finger_tap))
            OptionDropdown(tapOptions, prefs.threeFingerTap) { update(prefs.copy(threeFingerTap = it)) }
            FieldLabel(stringResource(R.string.pref_stylus_double_tap))
            OptionDropdown(tapOptions, prefs.stylusDoubleTap) { update(prefs.copy(stylusDoubleTap = it)) }
            FieldLabel(stringResource(R.string.pref_stylus_button_tap))
            OptionDropdown(tapOptions, prefs.stylusButtonTap) { update(prefs.copy(stylusButtonTap = it)) }
            FieldLabel(stringResource(R.string.pref_redmi_buttons))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.pen_button_1), color = palette.textDim.toComposeColor(), fontSize = 12.sp)
                    OptionDropdown(tapOptions, prefs.stylusButton1Tap) { update(prefs.copy(stylusButton1Tap = it)) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.pen_button_2), color = palette.textDim.toComposeColor(), fontSize = 12.sp)
                    OptionDropdown(tapOptions, prefs.stylusButton2Tap) { update(prefs.copy(stylusButton2Tap = it)) }
                }
            }
            CheckRow(stringResource(R.string.pref_volume_keys_turn_pages), prefs.volumeKeysTurnPages) { update(prefs.copy(volumeKeysTurnPages = it)) }

            HorizontalDivider(color = palette.border.toComposeColor())
            SectionTitle(stringResource(R.string.pref_new_notes))
            FieldLabel(stringResource(R.string.pref_filename_template))
            Text(
                stringResource(R.string.pref_filename_template_help),
                color = palette.textDim.toComposeColor(),
                fontSize = 12.sp,
            )
            OutlinedTextField(
                value = prefs.newNoteNameTemplate,
                onValueChange = { update(prefs.copy(newNoteNameTemplate = it)) },
                singleLine = true,
                placeholder = { Text(NameTemplate.DEFAULT) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.width(280.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NAME_TEMPLATE_PRESETS.forEach { t ->
                    Chip(t, prefs.newNoteNameTemplate == t) { update(prefs.copy(newNoteNameTemplate = t)) }
                }
            }
            Text(
                stringResource(R.string.pref_next_note_named, "${editor.newNoteStem(emptySet())}.xnote"),
                color = palette.textDim.toComposeColor(),
                fontSize = 12.sp,
            )

            HorizontalDivider(color = palette.border.toComposeColor())
            SectionTitle(stringResource(R.string.pref_page))
            FieldLabel(stringResource(R.string.pref_default_page_size))
            SizeDropdown(prefs.defaultPageSize) { it?.let { size -> update(prefs.copy(defaultPageSize = size)) } }
            if (prefs.defaultPageSize == PageSize.CUSTOM) {
                // A custom page is taken as typed, so the orientation chips have nothing to say
                // about it and are left out rather than shown doing nothing.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MillimetreField(stringResource(R.string.width_mm), prefs.customPageWidthMm) {
                        update(prefs.copy(customPageWidthMm = it))
                    }
                    MillimetreField(stringResource(R.string.height_mm), prefs.customPageHeightMm) {
                        update(prefs.copy(customPageHeightMm = it))
                    }
                }
            } else {
                FieldLabel(stringResource(R.string.pref_orientation))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.orientation_portrait), prefs.defaultPageOrientation == Orientation.PORTRAIT) {
                        update(prefs.copy(defaultPageOrientation = Orientation.PORTRAIT))
                    }
                    Chip(stringResource(R.string.orientation_landscape), prefs.defaultPageOrientation == Orientation.LANDSCAPE) {
                        update(prefs.copy(defaultPageOrientation = Orientation.LANDSCAPE))
                    }
                }
            }
            CheckRow(stringResource(R.string.pref_hide_page_borders), prefs.hidePageBorders) {
                update(prefs.copy(hidePageBorders = it))
            }
            FieldLabel(stringResource(R.string.pref_side_margin_px, prefs.sideMargin.toInt()))
            Slider(
                value = prefs.sideMargin.toFloat(),
                onValueChange = { update(prefs.copy(sideMargin = it.toDouble())) },
                valueRange = 0f..64f,
                modifier = Modifier.width(280.dp),
            )
            FieldLabel(stringResource(R.string.pref_page_colour))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SwatchCircles(editor, SwatchRole.PAPER, prefs.pageColor) { update(prefs.copy(pageColor = it)) }
            }
            CheckRow(stringResource(R.string.pref_page_colour_follows_theme), prefs.pageColor == null) {
                update(prefs.copy(pageColor = if (it) null else editor.swatches(SwatchRole.PAPER).first()))
            }

            HorizontalDivider(color = palette.border.toComposeColor())
            SectionTitle(stringResource(R.string.pref_home_explorer))
            Text(
                stringResource(R.string.pref_home_explorer_help),
                color = palette.textDim.toComposeColor(),
                fontSize = 12.sp,
            )
            FieldLabel(stringResource(R.string.pref_default_layout))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExplorerLayout.entries.forEach { l -> Chip(stringResource(l.labelRes), editor.explorerView.layout == l) { editor.setDefaultLayout(l) } }
            }
            FieldLabel(stringResource(R.string.pref_switcher_layouts))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExplorerLayout.entries.forEach { l ->
                    val on = l in prefs.switcherLayouts
                    ExplorerChip(stringResource(l.labelRes), on, icon = if (on) XnotesIcons.check else XnotesIcons.plus) {
                        val next = if (on) prefs.switcherLayouts - l else prefs.switcherLayouts + l
                        if (next.isNotEmpty()) updateHome(prefs.copy(switcherLayouts = ExplorerLayout.entries.filter { it in next }))
                    }
                }
            }
            FieldLabel(stringResource(R.string.pref_sidebar_sections))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                @Composable
                fun section(label: String, on: Boolean, flip: () -> Preferences) =
                    ExplorerChip(label, on, icon = if (on) XnotesIcons.check else XnotesIcons.plus) { updateHome(flip()) }
                section(stringResource(R.string.recent), prefs.sidebarRecent) { prefs.copy(sidebarRecent = !prefs.sidebarRecent) }
                section(stringResource(R.string.pinned), prefs.sidebarPinned) { prefs.copy(sidebarPinned = !prefs.sidebarPinned) }
                section(stringResource(R.string.toolbar_colours), prefs.sidebarColours) { prefs.copy(sidebarColours = !prefs.sidebarColours) }
                if (prefs.trashDays != 0) section(stringResource(R.string.trash), prefs.sidebarTrash) { prefs.copy(sidebarTrash = !prefs.sidebarTrash) }
            }
            FieldLabel(stringResource(R.string.pref_home_opens_to))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.top_folder), prefs.homeOpensTo == "top") { updateHome(prefs.copy(homeOpensTo = "top")) }
                Chip(stringResource(R.string.last_folder), prefs.homeOpensTo == "last") { updateHome(prefs.copy(homeOpensTo = "last")) }
                Chip(stringResource(R.string.recent_and_pinned), prefs.homeOpensTo == "shelves") { updateHome(prefs.copy(homeOpensTo = "shelves")) }
            }
            val words = rememberExplorerWords()
            FieldLabel(stringResource(R.string.pref_dates))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(words.daysAgo(2), prefs.dateStyle == "relative") { updateHome(prefs.copy(dateStyle = "relative")) }
                Chip("${words.shortWeekday(java.time.DayOfWeek.WEDNESDAY)} ${clockText(words, java.time.LocalTime.of(17, 30), true)}", prefs.dateStyle == "day") { updateHome(prefs.copy(dateStyle = "day")) }
                Chip(words.dayMonthYear(16, java.time.Month.SEPTEMBER, 2026), prefs.dateStyle == "date") { updateHome(prefs.copy(dateStyle = "date")) }
            }
            FieldLabel(stringResource(R.string.pref_tapping_file))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.tap_opens), !prefs.tapPreviews) { updateHome(prefs.copy(tapPreviews = false)) }
                Chip(stringResource(R.string.tap_previews), prefs.tapPreviews) { updateHome(prefs.copy(tapPreviews = true)) }
            }
            FieldLabel(stringResource(R.string.pref_keep_deleted))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.off), prefs.trashDays == 0) { updateHome(prefs.copy(trashDays = 0)) }
                Chip(pluralStringResource(R.plurals.days_count, 7, 7), prefs.trashDays == 7) { updateHome(prefs.copy(trashDays = 7)) }
                Chip(pluralStringResource(R.plurals.days_count, 30, 30), prefs.trashDays == 30) { updateHome(prefs.copy(trashDays = 30)) }
                Chip(stringResource(R.string.until_emptied), prefs.trashDays == Preferences.TRASH_FOREVER) { updateHome(prefs.copy(trashDays = Preferences.TRASH_FOREVER)) }
            }
            if (editor.browseRoot != null) {
                FieldLabel(stringResource(R.string.pref_colour_names))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    editor.colorNames.entries.sortedBy { it.value.lowercase() }.forEach { (c, name) ->
                        Row(
                            Modifier.clip(MaterialTheme.shapes.small).background(palette.surface.toComposeColor())
                                .border(1.dp, palette.border.toComposeColor(), MaterialTheme.shapes.small)
                                .clickable { namingColor = c }.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(codeTint(c, palette)))
                            Text(name, color = palette.text.toComposeColor(), fontSize = 14.sp, maxLines = 1)
                        }
                    }
                    var picking by remember { mutableStateOf(false) }
                    Box {
                        ExplorerChip(stringResource(R.string.name_a_colour), false, icon = XnotesIcons.plus) { picking = true }
                        DropdownMenu(expanded = picking, onDismissRequest = { picking = false }) {
                            ColorCodeMenuContent { c -> picking = false; if (c != null) namingColor = c }
                        }
                    }
                }
            }
            CheckRow(stringResource(R.string.pref_per_folder_views), prefs.perFolderViews) { updateHome(prefs.copy(perFolderViews = it)) }
            CheckRow(stringResource(R.string.pref_show_folder_counts), prefs.showFolderCounts) { updateHome(prefs.copy(showFolderCounts = it)) }
            CheckRow(stringResource(R.string.pref_show_extensions), prefs.showExtensions) { updateHome(prefs.copy(showExtensions = it)) }

            HorizontalDivider(color = palette.border.toComposeColor())
            SectionTitle(stringResource(R.string.pref_pdf_export))
            CheckRow(stringResource(R.string.pref_pdf_heading_bookmarks), prefs.pdfHeadingBookmarks) {
                updateHome(prefs.copy(pdfHeadingBookmarks = it))
            }

            HorizontalDivider(color = palette.border.toComposeColor())
            SectionTitle(stringResource(R.string.pref_performance))
            FieldLabel(stringResource(R.string.pref_max_cache_px, prefs.maxCacheResolution))
            Slider(
                value = prefs.maxCacheResolution.toFloat(),
                onValueChange = { update(prefs.copy(maxCacheResolution = Math.round(it))) },
                valueRange = 1024f..4096f,
                steps = 5,
                modifier = Modifier.width(280.dp),
            )
            Text(
                stringResource(R.string.pref_cache_help),
                color = palette.textDim.toComposeColor(),
                fontSize = 12.sp,
            )
            CheckRow(stringResource(R.string.pref_disable_front_buffering), prefs.disableFrontBuffering) {
                update(prefs.copy(disableFrontBuffering = it))
            }
            Text(
                stringResource(R.string.pref_front_buffering_help),
                color = palette.textDim.toComposeColor(),
                fontSize = 12.sp,
            )

            if (editor.treeSitterAvailable) {
                HorizontalDivider(color = palette.border.toComposeColor())
                SectionTitle(stringResource(R.string.pref_code_highlighting))

                val langOptions = editor.scmLanguages().map { it to it }
                FieldLabel(stringResource(R.string.pref_default_code_language))
                Text(
                    stringResource(R.string.pref_code_language_help),
                    color = palette.textDim.toComposeColor(),
                    fontSize = 12.sp,
                )
                OptionDropdown(listOf("plain" to stringResource(R.string.code_plain)) + langOptions, prefs.defaultCodeLanguage) {
                    update(prefs.copy(defaultCodeLanguage = it))
                }

                FieldLabel(stringResource(R.string.pref_code_theme))
                Text(
                    stringResource(R.string.pref_code_theme_help),
                    color = palette.textDim.toComposeColor(),
                    fontSize = 12.sp,
                )
                if (editor.hasCustomCodeTheme) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            prefs.codeThemeName ?: stringResource(R.string.custom_theme),
                            color = palette.text.toComposeColor(),
                            fontSize = 13.sp,
                        )
                        TextButton(onClick = { editor.resetCodeTheme() }) { Text(stringResource(R.string.reset), fontSize = 13.sp) }
                    }
                }
                TextButton(onClick = { onImportCodeTheme() }) { Text(stringResource(R.string.import_theme), fontSize = 13.sp) }
            }

            HorizontalDivider(color = palette.border.toComposeColor())
            SectionTitle(stringResource(R.string.pref_fonts))
            Text(
                stringResource(R.string.pref_fonts_help),
                color = palette.textDim.toComposeColor(),
                fontSize = 12.sp,
            )
            TextButton(onClick = { onImportFont() }) { Text(stringResource(R.string.import_font), fontSize = 13.sp) }
            for (font in editor.customFonts) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        font.label,
                        color = palette.text.toComposeColor(),
                        fontSize = 13.sp,
                        style = TextStyle(fontFamily = font.face.toComposeFamily()),
                    )
                    if (font.mono) {
                        Text(stringResource(R.string.mono_suffix), color = palette.accent.toComposeColor(), fontSize = 11.sp)
                    }
                    TextButton(onClick = { editor.removeCustomFont(font.face) }) { Text(stringResource(R.string.remove), fontSize = 12.sp) }
                }
            }

            }
            if (!toolbarEditorOpen) {
                HorizontalDivider(color = palette.border.toComposeColor())
                SectionTitle(stringResource(R.string.pref_toolbar))
                Text(
                    stringResource(R.string.toolbar_workspace_intro),
                    color = palette.textDim.toComposeColor(), fontSize = 13.sp,
                )
                TextButton(onClick = { toolbarEditorOpen = true }) { Text(stringResource(R.string.toolbar_configure)) }
            } else {
            val barLabel = stringResource(R.string.pref_toolbar)
            val workspace = editedWorkspace()
            val currentBar = selectedBar()
            val toolbarActions: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = {
                    toolbarActionsExpanded = false
                    val old = editedWorkspace()
                    val id = generateSequence(1) { it + 1 }.map { "bar-$it" }
                        .first { key -> old.bars.none { it.id == key } }
                    val defaults = if (canvasTab) ToolbarLayout.CANVAS_DEFAULT else ToolbarLayout.DEFAULT
                    val compact = ToolbarLayout(defaults.sections.map { section ->
                        section.copy(entries = section.entries.map { entry ->
                            entry.copy(visible = entry.item == ToolbarItem.COLORS)
                        })
                    })
                    val bar = ToolbarBar(
                        id = id,
                        name = "$barLabel ${old.bars.size + 1}",
                        layout = compact,
                        geometry = ToolbarGeometry(anchor = ToolbarAnchor.BOTTOM_RIGHT, floating = true),
                    )
                    val profiles = editor.toolbarWorkspaces
                    val changed = old.addBar(bar)
                    val dockPlan = toolbarDockPlan(
                        changed, prefs.toolbarLook,
                        screenConfiguration.screenWidthDp.dp, screenConfiguration.screenHeightDp.dp,
                    )
                    val floated = toolbarFloatingPlan(
                        changed, prefs.toolbarLook,
                        screenConfiguration.screenWidthDp.dp - dockPlan.leftInset.dp - dockPlan.rightInset.dp,
                        screenConfiguration.screenHeightDp.dp - dockPlan.topInset.dp - dockPlan.bottomInset.dp,
                        editor.toolbarColorCount,
                    )
                    if (id in floated.rejected) {
                        dockError = "Limite de barras alcançado nas faixas atuais. Libere espaço antes de adicionar."
                    } else {
                        dockError = null
                        editor.applyToolbarWorkspaces(
                            if (canvasTab) profiles.copy(canvas = changed) else profiles.copy(note = changed),
                        )
                        showBar(id)
                    }
                }) { Text(stringResource(R.string.add) + " " + barLabel) }
                TextButton(onClick = {
                    toolbarActionsExpanded = false
                    val old = editedWorkspace()
                    val bar = selectedBar()
                    val id = generateSequence(1) { it + 1 }.map { "bar-$it" }
                        .first { key -> old.bars.none { it.id == key } }
                    val changed = old.duplicateBar(bar.id, id, "$barLabel ${old.bars.size + 1}")
                    val profiles = editor.toolbarWorkspaces
                    val plan = toolbarDockPlan(
                        changed, prefs.toolbarLook,
                        screenConfiguration.screenWidthDp.dp, screenConfiguration.screenHeightDp.dp,
                    )
                    val floated = toolbarFloatingPlan(
                        changed, prefs.toolbarLook,
                        screenConfiguration.screenWidthDp.dp - plan.leftInset.dp - plan.rightInset.dp,
                        screenConfiguration.screenHeightDp.dp - plan.topInset.dp - plan.bottomInset.dp,
                        editor.toolbarColorCount,
                    )
                    if (plan.rejected.isNotEmpty() || floated.rejected.isNotEmpty()) {
                        dockError = "A cópia não cabe nas faixas disponíveis. Ajuste o tamanho ou a organização."
                    } else {
                        dockError = null
                        editor.applyToolbarWorkspaces(
                            if (canvasTab) profiles.copy(canvas = changed) else profiles.copy(note = changed),
                        )
                        showBar(id)
                    }
                }) { Text(stringResource(R.string.duplicate)) }
                if (workspace.bars.size > 1 && currentBar.id != workspace.bars.first().id) {
                    TextButton(onClick = {
                        toolbarActionsExpanded = false
                        val old = editedWorkspace()
                        val changed = old.removeBar(selectedBar().id)
                        val profiles = editor.toolbarWorkspaces
                        editor.applyToolbarWorkspaces(
                            if (canvasTab) profiles.copy(canvas = changed) else profiles.copy(note = changed),
                        )
                        showBar(changed.bars.first().id)
                    }) { Text(stringResource(R.string.remove)) }
                }
            }
            TextButton(onClick = { toolbarActionsExpanded = false; beginCopyToolbar() }) {
                Text("Copiar para " + if (canvasTab) "XNotes" else "XCanvas")
            }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Chip("xnote", selected = !canvasTab) { showTab(false) }
                Chip("xcanvas", selected = canvasTab) { showTab(true) }
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(onClick = { barMenuExpanded = true },
                        modifier = Modifier.widthIn(max = 120.dp)) {
                        Text("${currentBar.name} ▾", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(
                        expanded = barMenuExpanded,
                        onDismissRequest = { barMenuExpanded = false },
                    ) {
                        workspace.bars.forEachIndexed { index, bar ->
                            DropdownMenuItem(
                                text = { Text("${index + 1}. ${bar.name}") },
                                onClick = {
                                    showBar(bar.id)
                                    barMenuExpanded = false
                                },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reset)) },
                            onClick = {
                                applyEdited(if (canvasTab) ToolbarLayout.CANVAS_DEFAULT else ToolbarLayout.DEFAULT)
                                barMenuExpanded = false
                            },
                        )
                    }
                }
                TextButton(onClick = {
                    savedWorkspaces = editor.toolbarWorkspacePresets
                    workspaceFileMessage = null
                    workspacesDialogOpen = true
                }) { Text("Workspaces ▾", maxLines = 1) }
                Box {
                    IconButton(onClick = { toolbarActionsExpanded = true }) {
                        Icon(XnotesIcons.more, "Criar, duplicar ou copiar barra")
                    }
                    DropdownMenu(
                        expanded = toolbarActionsExpanded,
                        onDismissRequest = { toolbarActionsExpanded = false },
                    ) {
                        Column(Modifier.widthIn(min = 290.dp).padding(8.dp)) {
                            toolbarActions()
                        }
                    }
                }
            }
            val entryPosition = selectedEntry
            val activeEntry = entryPosition?.let { (section, index) ->
                currentBar.layout.sections.getOrNull(section)?.entries?.getOrNull(index)
            }
            val barSize = if (currentBar.id == workspace.bars.first().id) prefs.toolbarLook.size
                else when (currentBar.geometry.scale) {
                    com.xnotes.core.tools.ToolbarScale.COMPACT -> ToolbarSize.COMPACT
                    com.xnotes.core.tools.ToolbarScale.REGULAR -> ToolbarSize.REGULAR
                    com.xnotes.core.tools.ToolbarScale.COMFORTABLE -> ToolbarSize.COMFORTABLE
                }
            val defaultEntrySize = resolvedBarMetrics(barSize, currentBar.geometry).button.value.toInt()
            val preview: @Composable () -> Unit = {
                ToolbarWorkspacePreview(
                    workspace = workspace.onlyItems(if (canvasTab) ToolbarLayout.CANVAS_ITEMS else ToolbarLayout.NOTE_ITEMS),
                    look = prefs.toolbarLook,
                    selectedId = currentBar.id,
                    selectedEntry = selectedEntry,
                    colors = editor.toolbarColors.take(editor.toolbarColorCount),
                    activeColorIndex = editor.activeColorIndex,
                    pageNumber = editor.pageIndex + 1,
                    pageCount = editor.pageCount,
                    zoomLabel = "${editor.zoomPercent}%",
                    titleLabel = editor.title,
                    onSelectBar = { id ->
                        showBar(id)
                        selectedEntry = null
                        toolbarPropertyTab = ToolbarPropertyTab.BARS
                    },
                    onSelectEntry = { id, section, index ->
                        showBar(id)
                        selectedEntry = section to index
                        toolbarPropertyTab = ToolbarPropertyTab.ELEMENTS
                    },
                    onMoveBar = { id, anchor ->
                        editor.moveToolbarBar(
                            if (canvasTab) com.xnotes.core.tools.ToolbarContext.CANVAS else com.xnotes.core.tools.ToolbarContext.NOTE,
                            id, anchor,
                        )
                    },
                )
            }
            val propertyTabs: @Composable () -> Unit = {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Chip("Barras", toolbarPropertyTab == ToolbarPropertyTab.BARS) {
                        selectedEntry = null
                        toolbarPropertyTab = ToolbarPropertyTab.BARS
                    }
                    Chip("Aparência", toolbarPropertyTab == ToolbarPropertyTab.DIMENSIONS) {
                        selectedEntry = null
                        toolbarPropertyTab = ToolbarPropertyTab.DIMENSIONS
                    }
                    Chip("Elementos", toolbarPropertyTab == ToolbarPropertyTab.ELEMENTS) {
                        toolbarPropertyTab = ToolbarPropertyTab.ELEMENTS
                    }
                 }
            }
            val inspector: @Composable () -> Unit = {
                Column(
                    Modifier.fillMaxWidth()
                        .heightIn(max = if (toolbarPropertyTab == ToolbarPropertyTab.ELEMENTS) 380.dp else 500.dp)
                        .onGloballyPositioned { viewport = it.boundsInRoot() }
                        .verticalScroll(if (toolbarPropertyTab == ToolbarPropertyTab.ELEMENTS)
                            toolbarElementsScroll else toolbarPropertiesScroll),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (toolbarPropertyTab == ToolbarPropertyTab.ELEMENTS) {
                        if (activeEntry != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tamanho do elemento", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { selectedEntry = null }) { Text("Fechar") }
                            }
                            ToolbarEntrySizeControls(
                                entry = activeEntry,
                                defaultSizeDp = defaultEntrySize,
                                onChange = { size ->
                                    applyEdited(editedLayout().setEntrySize(entryPosition.first, entryPosition.second, size))
                                },
                            )
                        }
                    }
                    if (toolbarPropertyTab == ToolbarPropertyTab.DIMENSIONS) {
                        Text("Tamanho e espaçamento", style = MaterialTheme.typography.titleSmall)
                        ToolbarGeometryControls(
                            geometry = currentBar.geometry,
                            onChange = { next -> configureDock(next) },
                        )
                        FieldLabel(stringResource(R.string.pref_toolbar_size))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for ((scale, label) in listOf(
                                com.xnotes.core.tools.ToolbarScale.COMPACT to R.string.toolbar_size_compact,
                                com.xnotes.core.tools.ToolbarScale.REGULAR to R.string.toolbar_size_regular,
                                com.xnotes.core.tools.ToolbarScale.COMFORTABLE to R.string.toolbar_size_comfortable,
                            )) {
                                Chip(stringResource(label), currentBar.geometry.scale == scale) {
                                    val primary = currentBar.id == workspace.bars.first().id
                                    val size = when (scale) {
                                        com.xnotes.core.tools.ToolbarScale.COMPACT -> ToolbarSize.COMPACT
                                        com.xnotes.core.tools.ToolbarScale.REGULAR -> ToolbarSize.REGULAR
                                        com.xnotes.core.tools.ToolbarScale.COMFORTABLE -> ToolbarSize.COMFORTABLE
                                    }
                                    configureDock(currentBar.geometry.copy(scale = scale),
                                        if (primary) prefs.toolbarLook.copy(size = size) else prefs.toolbarLook)
                                }
                            }
                        }
                    }
                    if (toolbarPropertyTab == ToolbarPropertyTab.BARS) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
            OutlinedTextField(
                value = currentBar.name,
                onValueChange = { name -> updateSelectedBar { it.copy(name = name.take(48)) } },
                label = { Text(stringResource(R.string.toolbar_name)) },
                singleLine = true,
                modifier = Modifier.widthIn(max = 280.dp),
            )
            Chip(stringResource(R.string.toolbar_movable), currentBar.geometry.movable) {
                updateSelectedBar { bar -> bar.copy(geometry = bar.geometry.copy(movable = !bar.geometry.movable)) }
            }
            }
            if (currentBar.id != workspace.bars.first().id) {
                val barIndex = workspace.bars.indexOfFirst { it.id == currentBar.id }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Chip(stringResource(R.string.toolbar_visible), currentBar.visible) {
                        updateSelectedBar { it.copy(visible = !it.visible) }
                    }
                    if (barIndex > 1) {
                        TextButton(onClick = {
                            val profiles = editor.toolbarWorkspaces
                            val updated = editedWorkspace().moveBar(currentBar.id, barIndex - 1)
                            editor.applyToolbarWorkspaces(
                                if (canvasTab) profiles.copy(canvas = updated) else profiles.copy(note = updated),
                            )
                        }) { Text(stringResource(R.string.toolbar_move_up)) }
                    }
                    if (barIndex in 1 until workspace.bars.lastIndex) {
                        TextButton(onClick = {
                            val profiles = editor.toolbarWorkspaces
                            val updated = editedWorkspace().moveBar(currentBar.id, barIndex + 1)
                            editor.applyToolbarWorkspaces(
                                if (canvasTab) profiles.copy(canvas = updated) else profiles.copy(note = updated),
                            )
                        }) { Text(stringResource(R.string.toolbar_move_down)) }
                    }
                }
                val secondaryDocked = currentBar.geometry.dockEdge != null && !currentBar.geometry.floating
                FieldLabel("Posicionamento")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Encaixada", secondaryDocked) {
                        val geo = currentBar.geometry
                        configureDock(geo.copy(
                            floating = false,
                            dockEdge = geo.dockEdge ?: ToolbarDockEdge.TOP,
                            dockLane = if (geo.dockEdge == null) 1 else geo.dockLane,
                            widthDp = geo.widthDp ?: 200,
                            heightDp = geo.heightDp,
                        ))
                    }
                    Chip("Flutuante", !secondaryDocked) {
                        configureDock(currentBar.geometry.copy(floating = true, dockEdge = null))
                    }
                }
                if (secondaryDocked) {
                    FieldLabel("Borda")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for ((edge, label) in listOf(
                            ToolbarDockEdge.TOP to "Superior", ToolbarDockEdge.BOTTOM to "Inferior",
                            ToolbarDockEdge.LEFT to "Esquerda", ToolbarDockEdge.RIGHT to "Direita",
                        )) {
                            Chip(label, currentBar.geometry.dockEdge == edge) {
                                val geo = currentBar.geometry
                                val vertical = edge == ToolbarDockEdge.LEFT || edge == ToolbarDockEdge.RIGHT
                                configureDock(geo.copy(
                                    dockEdge = edge,
                                    widthDp = if (vertical && geo.widthDp == 200) null else geo.widthDp ?: if (vertical) null else 200,
                                    heightDp = if (!vertical && geo.heightDp == 200) null else geo.heightDp ?: if (vertical) 200 else null,
                                ))
                            }
                        }
                    }
                } else {
                    FieldLabel(stringResource(R.string.pref_toolbar_position))
                    ToolbarAnchorGrid(
                        selected = currentBar.geometry.anchor,
                        onSelect = { anchor ->
                            updateSelectedBar { bar -> bar.copy(geometry = bar.geometry.copy(anchor = anchor)) }
                        },
                    )
                }
            } else {
                FieldLabel(stringResource(R.string.toolbar_primary_appearance))
                val look = prefs.toolbarLook
                val floatingPrimary = currentBar.geometry.floating || look.floating
                val dockAnchor = when (look.position) {
                    ToolbarPosition.TOP -> ToolbarAnchor.TOP_CENTER
                    ToolbarPosition.BOTTOM -> ToolbarAnchor.BOTTOM_CENTER
                    ToolbarPosition.LEFT -> ToolbarAnchor.CENTER_LEFT
                    ToolbarPosition.RIGHT -> ToolbarAnchor.CENTER_RIGHT
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.toolbar_docked), !floatingPrimary) {
                        configureDock(currentBar.geometry.copy(floating = false, anchor = dockAnchor), look.copy(floating = false))
                    }
                    Chip(stringResource(R.string.toolbar_floating), floatingPrimary) {
                        configureDock(currentBar.geometry.copy(floating = true, anchor = if (currentBar.geometry.floating) currentBar.geometry.anchor else dockAnchor), look.copy(floating = true))
                    }
                }
                FieldLabel(stringResource(R.string.pref_toolbar_position))
                ToolbarAnchorGrid(
                    selected = if (floatingPrimary) currentBar.geometry.anchor else dockAnchor,
                    docked = !floatingPrimary,
                    onSelect = { anchor ->
                        if (!floatingPrimary) {
                            val position = when (anchor) {
                                ToolbarAnchor.TOP_CENTER -> ToolbarPosition.TOP
                                ToolbarAnchor.BOTTOM_CENTER -> ToolbarPosition.BOTTOM
                                ToolbarAnchor.CENTER_LEFT -> ToolbarPosition.LEFT
                                ToolbarAnchor.CENTER_RIGHT -> ToolbarPosition.RIGHT
                                else -> look.position
                            }
                            configureDock(currentBar.geometry.copy(anchor = anchor, floating = false), look.copy(floating = false, position = position))
                        } else {
                            configureDock(currentBar.geometry.copy(anchor = anchor, floating = true))
                        }
                    },
                )
            }
            val isPrimaryDock = currentBar.id == workspace.bars.first().id &&
                !currentBar.geometry.floating && !prefs.toolbarLook.floating
            val isSecondaryDock = currentBar.id != workspace.bars.first().id &&
                currentBar.geometry.dockEdge != null && !currentBar.geometry.floating
            val floatingBar = !isPrimaryDock && !isSecondaryDock
            FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FieldLabel(if (floatingBar) "Faixa flutuante preferida · máximo 3"
                        else "Faixa de encaixe · máximo 3 por borda")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (lane in 0..2) {
                    Chip("Faixa ${lane + 1}", currentBar.geometry.dockLane == lane) {
                        configureDock(currentBar.geometry.copy(dockLane = lane))
                    }
                }
            }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FieldLabel("Alinhamento na faixa")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((align, label) in listOf(
                    ToolbarDockAlignment.START to "Início",
                    ToolbarDockAlignment.CENTER to "Centro",
                    ToolbarDockAlignment.END to "Fim",
                )) {
                    Chip(label, (if (floatingBar) toolbarFloatingAlignment(currentBar)
                        else currentBar.geometry.dockAlignment) == align) {
                        val anchor = if (floatingBar) when (currentBar.geometry.anchor.floatingEdge()) {
                            ToolbarDockEdge.TOP -> when (align) {
                                ToolbarDockAlignment.START -> ToolbarAnchor.TOP_LEFT
                                ToolbarDockAlignment.CENTER -> ToolbarAnchor.TOP_CENTER
                                ToolbarDockAlignment.END -> ToolbarAnchor.TOP_RIGHT
                            }
                            ToolbarDockEdge.BOTTOM -> when (align) {
                                ToolbarDockAlignment.START -> ToolbarAnchor.BOTTOM_LEFT
                                ToolbarDockAlignment.CENTER -> ToolbarAnchor.BOTTOM_CENTER
                                ToolbarDockAlignment.END -> ToolbarAnchor.BOTTOM_RIGHT
                            }
                            ToolbarDockEdge.LEFT -> ToolbarAnchor.CENTER_LEFT
                            ToolbarDockEdge.RIGHT -> ToolbarAnchor.CENTER_RIGHT
                        } else currentBar.geometry.anchor
                        configureDock(currentBar.geometry.copy(dockAlignment = align, anchor = anchor))
                    }
                }
            }
                }
            }
            val displayDockPlan = toolbarDockPlan(
                workspace, prefs.toolbarLook,
                screenConfiguration.screenWidthDp.dp, screenConfiguration.screenHeightDp.dp,
            )
            val displayedPlacement = if (floatingBar) toolbarFloatingPlan(
                workspace, prefs.toolbarLook,
                screenConfiguration.screenWidthDp.dp - displayDockPlan.leftInset.dp - displayDockPlan.rightInset.dp,
                screenConfiguration.screenHeightDp.dp - displayDockPlan.topInset.dp - displayDockPlan.bottomInset.dp,
                editor.toolbarColorCount,
            ).placement(currentBar.id) else displayDockPlan.placement(currentBar.id)
            if (displayedPlacement != null && displayedPlacement.lane != currentBar.geometry.dockLane) {
                Text("Redistribuída automaticamente para a Faixa ${displayedPlacement.lane + 1}.",
                    color = palette.accent.toComposeColor(), style = MaterialTheme.typography.bodySmall)
            }
            dockError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                    if (toolbarPropertyTab == ToolbarPropertyTab.ELEMENTS) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Paleta de cores", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { colorControlsExpanded = !colorControlsExpanded }) {
                                Text(if (colorControlsExpanded) "Recolher" else "Configurar")
                            }
                        }
                        if (colorControlsExpanded) {
                            // Palette is shared; only the number of blocks belongs to this bar.
                            FieldLabel(stringResource(R.string.pref_toolbar_colours_n, editor.toolbarColorCount))
                            Slider(
                                value = editor.toolbarColorCount.toFloat(),
                                onValueChange = {
                                    val total = Math.round(it)
                                    editor.applyToolbarColorCount(total)
                                    if (editedLayout().colorBlockCount > total)
                                        applyEdited(editedLayout().setColorBlocks(total))
                                },
                                valueRange = 1f..InkPalette.MAX_SWATCHES.toFloat(),
                                steps = InkPalette.MAX_SWATCHES - 2,
                                modifier = Modifier.width(280.dp),
                            )
                            FieldLabel("Distribuir cores em ${layout.colorBlockCount} blocos")
                            if (editor.toolbarColorCount > 1 && layout.colorBlockCount > 0) {
                                Slider(
                                    value = layout.colorBlockCount.coerceIn(1, editor.toolbarColorCount).toFloat(),
                                    onValueChange = { applyEdited(editedLayout().setColorBlocks(Math.round(it))) },
                                    valueRange = 1f..editor.toolbarColorCount.toFloat(),
                                    steps = (editor.toolbarColorCount - 2).coerceAtLeast(0),
                                    modifier = Modifier.width(280.dp),
                                )
                            }
                        }
                    }
                    if (toolbarPropertyTab == ToolbarPropertyTab.ELEMENTS) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.toolbar_distribution), color = palette.textDim.toComposeColor(), fontSize = 12.sp)
                Chip(stringResource(R.string.toolbar_continuous), layout.distribution == ToolbarDistribution.CONTINUOUS) {
                    selectedEntry = null
                    applyEdited(editedLayout().setDistribution(ToolbarDistribution.CONTINUOUS))
                }
                Chip(stringResource(R.string.toolbar_regions), layout.distribution == ToolbarDistribution.REGIONS) {
                    selectedEntry = null
                    applyEdited(editedLayout().setDistribution(ToolbarDistribution.REGIONS))
                }
            }
            if (layout.distribution == ToolbarDistribution.REGIONS &&
                layout.sections.any { it.region != ToolbarRegion.START }) {
                TextButton(onClick = {
                    selectedEntry = null
                    applyEdited(editedLayout().gatherSectionsAtStart())
                }) { Text("Reunir sections no início", fontSize = 12.sp) }
            }
            val unifiedPenEnabled = layout.sections.any { section ->
                section.entries.any { entry -> entry.item == ToolbarItem.UNIFIED_PEN && entry.visible }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = unifiedPenEnabled,
                    onCheckedChange = { enabled ->
                        val current = editedLayout()
                        applyEdited(if (enabled) current.withUnifiedPenVisible() else current.withUnifiedPenHidden())
                    },
                )
                Text(stringResource(R.string.toolbar_unified_pen), color = palette.text.toComposeColor(), fontSize = 13.sp)
            }
            ToolbarCustomizerBody(
                layout = layout,
                availableItems = if (canvasTab) ToolbarLayout.CANVAS_ITEMS else ToolbarLayout.NOTE_ITEMS,
                dragItem = dragItem,
                selectedEntry = selectedEntry,
                dropTarget = dropTarget,
                dragRegionTarget = dragRegionTarget,
                dragSection = dragSection,
                sectionDropTarget = sectionDropTarget,
                chipBounds = chipBounds,
                laneBounds = laneBounds,
                sectionBounds = sectionBounds,
                sectionGrabBounds = sectionGrabBounds,
                regionBounds = regionBounds,
                onToggle = { s, i -> applyEdited(editedLayout().toggleVisible(s, i)) },
                onSelect = { s, i -> selectedEntry = s to i },
                onRemoveItem = { s, i ->
                    applyEdited(editedLayout().removeItem(s, i))
                    selectedEntry = null
                },
                onAddItem = { s, lane, item -> applyEdited(editedLayout().addItem(s, lane, item)) },
                onLanesChange = { s, lanes -> applyEdited(editedLayout().setSectionLanes(s, lanes)) },
                onRegionChange = { s, region -> applyEdited(editedLayout().setSectionRegion(s, region)) },
                onDelete = { s -> applyEdited(editedLayout().removeSection(s)) },
                onAdd = { lanes, region -> applyEdited(editedLayout().addSection(lanes, region)) },
                onDragStart = { section, index, local ->
                    val key = section to index
                    dragItem = key
                    // The floating copy now has the same dimensions as the source row.
                    dragGrab = local
                    dragFinger = (chipBounds[key]?.topLeft ?: Offset.Zero) + local
                    retarget()
                },
                onDrag = { delta -> dragFinger += delta; retarget() },
                onDrop = {
                    val target = dropTarget
                    val region = dragRegionTarget
                    val moving = dragItem
                    if (moving != null) {
                        val current = editedLayout()
                        val next = when {
                            target != null -> current.moveItem(moving.first, moving.second,
                                target.first, target.third, target.second)
                            region != null -> current.moveItemToNewRegionSection(moving.first, moving.second, region)
                            else -> current
                        }
                        if (next != current) {
                            applyEdited(next)
                            selectedEntry = null
                        }
                    }
                    dragItem = null
                    dropTarget = null
                    dragRegionTarget = null
                },
                onCancel = {
                    dragItem = null
                    dropTarget = null
                    dragRegionTarget = null
                },
                onSectionDragStart = { sec, local ->
                    dragSection = sec
                    val grab = sectionGrabBounds[sec]?.topLeft ?: sectionBounds[sec]?.topLeft ?: Offset.Zero
                    val card = sectionBounds[sec]?.topLeft ?: Offset.Zero
                    dragFinger = grab + local
                    dragGrab = dragFinger - card
                    retargetSection()
                },
                onSectionDrag = { delta -> dragFinger += delta; retargetSection() },
                onSectionDrop = {
                    val from = dragSection
                    val target = sectionDropTarget
                    if (from != null && target != null) {
                        val cur = editedLayout()
                        if (cur.distribution == ToolbarDistribution.REGIONS) {
                            applyEdited(cur.moveSectionToRegion(from, target.region, target.beforeSection))
                        } else {
                            val insertAt = target.beforeSection ?: cur.sections.size
                            val to = (if (insertAt > from) insertAt - 1 else insertAt).coerceIn(0, cur.sections.lastIndex)
                            if (to != from) applyEdited(cur.moveSection(from, to))
                        }
                    }
                    dragSection = null
                    sectionDropTarget = null
                },
                onSectionCancel = {
                    dragSection = null
                    sectionDropTarget = null
                },
            )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FieldLabel(stringResource(R.string.toolbar_preview))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { previewExpanded = !previewExpanded }) {
                    Text(if (previewExpanded) "Reduzir" else "Ampliar")
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.widthIn(max = if (previewExpanded) 680.dp else 390.dp)) { preview() }
            }
            propertyTabs()
            inspector()
            }

            if (!toolbarEditorOpen) {
            HorizontalDivider(color = palette.border.toComposeColor())
            SectionTitle(stringResource(R.string.pref_backup))
            Text(stringResource(R.string.pref_backup_help), color = palette.textDim.toComposeColor(), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onBackup(true) }) { Text(stringResource(R.string.export_settings), fontSize = 13.sp) }
                TextButton(onClick = { onBackup(false) }) { Text(stringResource(R.string.import_settings), fontSize = 13.sp) }
            }
            Spacer(Modifier.size(8.dp))
            }
        }
    }
        // The dragged chip/section's floating copy: a pane-root sibling so the scroll never clips it.
        val gx = (dragFinger.x - dragGrab.x - paneTopLeft.x).toInt()
        val gy = (dragFinger.y - dragGrab.y - paneTopLeft.y).toInt()
        val ghostEntry = dragItem?.let { (sec, idx) ->
            editedLayout().sections.getOrNull(sec)?.entries?.getOrNull(idx)
        }
        val ghostSecIdx = dragSection
        if (ghostEntry != null) {
            val widthPx = dragItem?.let { chipBounds[it]?.width }
            if (widthPx != null) {
                Box(Modifier.offset { IntOffset(gx, gy) }) {
                    ToolbarDragGhost(ghostEntry.item, ghostEntry.visible, widthPx)
                }
            }
        } else if (ghostSecIdx != null) {
            val sectionG = editedLayout().sections.getOrNull(ghostSecIdx)
            val widthG = sectionBounds[ghostSecIdx]?.width
            if (sectionG != null && widthG != null) {
                Box(Modifier.offset { IntOffset(gx, gy) }) { SectionCardGhost(sectionG, widthG) }
            }
        }
    }

    // While dragging near a vertical edge of the scroll viewport, ease the list along so items
    // off-screen can be reached without lifting the finger.
    LaunchedEffect(dragItem != null || dragSection != null) {
        while (dragItem != null || dragSection != null) {
            val band = 90f
            val y = dragFinger.y
            val delta = when {
                y < viewport.top + band -> -((viewport.top + band - y) / band) * 14f
                y > viewport.bottom - band -> ((y - (viewport.bottom - band)) / band) * 14f
                else -> 0f
            }
            if (delta != 0f) {
                if (toolbarEditorOpen && toolbarPropertyTab == ToolbarPropertyTab.ELEMENTS)
                    toolbarElementsScroll.scrollBy(delta)
                else if (toolbarEditorOpen) toolbarPropertiesScroll.scrollBy(delta)
                else scrollState.scrollBy(delta)
                if (dragItem != null) retarget() else retargetSection()
            }
            delay(16L)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = LocalPalette.current.text.toComposeColor(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = LocalPalette.current.accent.toComposeColor(), fontSize = 13.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomMaterialControls(prefs: Preferences, update: (Preferences) -> Unit) {
    val palette = LocalPalette.current
    val styles = listOf(
        MaterialStyle.TONAL_SPOT to R.string.material_style_soft,
        MaterialStyle.VIBRANT to R.string.material_style_vivid,
        MaterialStyle.FIDELITY to R.string.material_style_original,
        MaterialStyle.EXPRESSIVE to R.string.material_style_playful,
        MaterialStyle.FRUIT_SALAD to R.string.material_style_fresh,
        MaterialStyle.RAINBOW to R.string.material_style_clean,
        MaterialStyle.NEUTRAL to R.string.material_style_muted,
        MaterialStyle.MONOCHROME to R.string.material_style_grayscale,
    )
    FieldLabel(stringResource(R.string.material_style))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        styles.forEach { (style, label) ->
            Chip(stringResource(label), prefs.materialStyle == style) {
                update(prefs.copy(materialStyle = style))
            }
        }
    }
    val description = when (prefs.materialStyle) {
        MaterialStyle.TONAL_SPOT -> R.string.material_style_soft_description
        MaterialStyle.VIBRANT -> R.string.material_style_vivid_description
        MaterialStyle.FIDELITY -> R.string.material_style_original_description
        MaterialStyle.EXPRESSIVE -> R.string.material_style_playful_description
        MaterialStyle.FRUIT_SALAD -> R.string.material_style_fresh_description
        MaterialStyle.RAINBOW -> R.string.material_style_clean_description
        MaterialStyle.NEUTRAL -> R.string.material_style_muted_description
        MaterialStyle.MONOCHROME -> R.string.material_style_grayscale_description
    }
    Text(stringResource(description), color = palette.textDim.toComposeColor(), fontSize = 12.sp)
    val percent = Math.round(prefs.materialContrast * 100).toInt()
    FieldLabel(stringResource(R.string.material_contrast, if (percent > 0) "+$percent%" else "$percent%"))
    Slider(
        value = prefs.materialContrast.toFloat(),
        onValueChange = { update(prefs.copy(materialContrast = Math.round(it * 10) / 10.0)) },
        valueRange = -1f..1f,
        steps = 19,
        modifier = Modifier.width(280.dp),
    )
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) palette.selectionBackground.toComposeColor() else palette.surface.toComposeColor())
            .border(1.dp, if (selected) palette.accent.toComposeColor() else palette.border.toComposeColor(), MaterialTheme.shapes.small)
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            color = if (selected) palette.selectionForeground.toComposeColor() else palette.text.toComposeColor(),
            fontSize = 14.sp,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun ColorDot(color: Color, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val label = Rgba.toHex(Rgba.fromArgb(color.toArgb()))
    Box(
        Modifier
            .size(30.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics {
                this.selected = selected
                contentDescription = label
            }
            .then(if (selected) Modifier.border(2.dp, palette.accent.toComposeColor(), CircleShape) else Modifier)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, palette.border.toComposeColor(), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
    )
}

/** A spectrum wheel signals that this dot opens the full picker. */
private val spectrumBrush = Brush.sweepGradient(
    listOf(
        Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
        Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000),
    ),
)

/**
 * A spectrum dot that opens [grid], a popup of colour swatches — shared by the accent and
 * page-colour rows. Until a colour outside the row's presets is chosen the dot shows the
 * spectrum wheel; once one is, it fills with that colour and reads as selected.
 */
@Composable
internal fun ColorPickerDot(
    current: Rgba?,
    custom: Boolean,
    onPick: (Rgba) -> Unit,
    dismissOnPick: Boolean = true,
    enabled: Boolean = true,
    grid: @Composable (onDismiss: () -> Unit, onPick: (Rgba) -> Unit) -> Unit,
) {
    val palette = LocalPalette.current
    val label = stringResource(R.string.material_custom_colour)
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(30.dp)
                .alpha(if (enabled) 1f else 0.4f)
                .semantics { contentDescription = label }
                .then(if (custom) Modifier.border(2.dp, palette.accent.toComposeColor(), CircleShape) else Modifier)
                .padding(4.dp)
                .clip(CircleShape)
                .then(if (custom && current != null) Modifier.background(current.toComposeColor()) else Modifier.background(spectrumBrush))
                .border(1.dp, palette.border.toComposeColor(), CircleShape)
                .clickable(enabled = enabled) { open = true },
        )
        // A live picker (e.g. the page/ink popup) edits across several taps, so it stays open until a
        // tap outside; a one-shot grid (the accent swatches) closes the moment a colour is chosen.
        if (open) grid({ open = false }, { onPick(it); if (dismissOnPick) open = false })
    }
}

/** Where each [SwatchRole]'s circles are kept: the settings file, shared by every row of that role. */
interface SwatchStore {
    fun swatches(role: SwatchRole): List<Rgba>
    fun setSwatch(role: SwatchRole, index: Int, color: Rgba)
}

/**
 * A role's five colour circles. A tap applies one; a tap on the one already [selected] opens the
 * picker and recolours that circle, as the toolbar's pen swatches do. Emits the circles straight
 * into the caller's row, so a Default chip can sit before them.
 */
@Composable
internal fun SwatchCircles(store: SwatchStore, role: SwatchRole, selected: Rgba?, onPick: (Rgba) -> Unit) {
    var swatches by remember(role) { mutableStateOf(store.swatches(role)) }
    var editing by remember { mutableStateOf<Int?>(null) }
    swatches.forEachIndexed { i, c ->
        Box {
            ColorDot(c.toComposeColor(), selected == c) { if (selected == c) editing = i else onPick(c) }
            if (editing == i) PageColorGridPopup(c, onDismiss = { editing = null }) { picked ->
                swatches = swatches.toMutableList().also { it[i] = picked }
                store.setSwatch(role, i, picked)
                onPick(picked)
            }
        }
    }
}

/** One tappable colour cell in a picker grid. */
/**
 * The colour-code picker shown inside a note/folder's overflow menu: a None row to clear the colour,
 * then the picker's full matrix (pale tints through near-black plus the greyscale row). [onPick] is
 * called with null for None. Rendered directly inside the menu's own dropdown column.
 */
@Composable
internal fun ColorCodeMenuContent(onPick: (Rgba?) -> Unit) {
    val palette = LocalPalette.current
    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.clip(MaterialTheme.shapes.extraSmall).clickable { onPick(null) }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(20.dp).clip(RoundedCornerShape(2.dp))
                    .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(2.dp)),
            )
            Text(stringResource(R.string.none), color = palette.text.toComposeColor(), fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
        }
        FullSwatchGrid { onPick(it) }
    }
}

/**
 * Page/pattern colour picker: the shared [ColorPickerPopup] (Swatches + Spectrum tabs, full
 * 13×13 range from pale tint to near-black plus a greyscale row, and HEX/RGB fields), so paper-like
 * and muted page backgrounds are reachable, not just the saturated ones. It keeps no recents list.
 */
@Composable
internal fun PageColorGridPopup(initial: Rgba?, onDismiss: () -> Unit, onPick: (Rgba) -> Unit) {
    ColorPickerPopup(initial = initial, recents = emptyList(), onDismiss = onDismiss, onPick = onPick)
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onChange(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(2.dp))
        Text(label, color = LocalPalette.current.text.toComposeColor(), fontSize = 14.sp)
    }
}

/**
 * One side of a custom page, in millimetres. The field keeps whatever is typed so a number can be
 * cleared and retyped; only a value that parses inside the settable range reaches the preference.
 */
@Composable
private fun MillimetreField(label: String, value: Double, onChange: (Double) -> Unit) {
    var text by remember { mutableStateOf(formatMm(value)) }
    // Adopt an outside change (Reset to defaults) without ever rewriting what is being typed.
    LaunchedEffect(value) { if (text.trim().toDoubleOrNull() != value) text = formatMm(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            parseMm(raw)?.let(onChange)
        },
        singleLine = true,
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        modifier = Modifier.width(136.dp),
    )
}

/** Stable 3×3 placement grid; docked primary bars only accept the four edges. */
@Composable
private fun ToolbarAnchorGrid(
    selected: ToolbarAnchor,
    docked: Boolean = false,
    onSelect: (ToolbarAnchor) -> Unit,
) {
    val palette = LocalPalette.current
    val symbols = listOf("↖", "↑", "↗", "←", "", "→", "↙", "↓", "↘")
    val dockable = setOf(
        ToolbarAnchor.TOP_CENTER, ToolbarAnchor.CENTER_LEFT,
        ToolbarAnchor.CENTER_RIGHT, ToolbarAnchor.BOTTOM_CENTER,
    )
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for (row in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (column in 0..2) {
                    val index = row * 3 + column
                    if (index == 4) {
                        Spacer(Modifier.size(48.dp))
                        continue
                    }
                    val anchor = ToolbarAnchor.entries[index]
                    val enabled = !docked || anchor in dockable
                    val active = selected == anchor && enabled
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (active) palette.accent.toComposeColor().copy(alpha = 0.18f)
                                else palette.panel.toComposeColor(),
                            )
                            .border(
                                if (active) 2.dp else 1.dp,
                                (if (active) palette.accent else palette.border).toComposeColor(),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable(enabled = enabled) { onSelect(anchor) }
                            .semantics {
                                contentDescription = anchor.name.replace('_', ' ').lowercase()
                                this.selected = active
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            symbols[index],
                            color = (if (!enabled) palette.textDim else if (active) palette.accent else palette.text).toComposeColor()
                                .copy(alpha = if (enabled) 1f else 0.35f),
                            fontSize = 21.sp,
                        )
                    }
                }
            }
        }
        if (docked) {
            Text(
                stringResource(R.string.toolbar_docked_positions_hint),
                color = palette.textDim.toComposeColor(),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun OptionDropdown(options: List<Pair<String, String>>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val palette = LocalPalette.current
    val label = options.firstOrNull { it.first == selectedId }?.second ?: options.first().second
    Box {
        Box(
            Modifier
                .clip(MaterialTheme.shapes.small)
                .border(1.dp, palette.border.toComposeColor(), MaterialTheme.shapes.small)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(label, color = palette.text.toComposeColor(), fontSize = 14.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, lbl) ->
                DropdownMenuItem(text = { Text(lbl) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}
