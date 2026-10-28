package com.xnotes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize

/**
 * M1 HUD editor prototype. Its background is deliberately a painted, non-document surface: the
 * only interactive objects are the toolbar previews and the editor chrome around them.
 * Persistence and the slot engine arrive in later milestones.
 */
@Composable
fun ToolbarHudScreen(
    editor: Editor,
    onClose: () -> Unit,
) {
    val palette = LocalPalette.current
    var catalogOpen by remember { mutableStateOf(true) }
    var propertiesOpen by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf("xnote") }
    var selectedBar by remember { mutableStateOf(0) }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var draggedItem by remember { mutableStateOf<String?>(null) }
    var movingBar by remember { mutableStateOf<Int?>(null) }
    var barDropTarget by remember { mutableStateOf<HudAnchor?>(null) }
    var editPanelX by remember { mutableStateOf(0f) }
    var editPanelY by remember { mutableStateOf(0f) }
    var editPanelDragging by remember { mutableStateOf(false) }
    var editPanelExpanded by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    val bars = remember {
        mutableStateListOf(
            HudBar(
                name = "Barra principal",
                items = editor.toolbarLayout.sections.flatMap { it.visibleEntries }.map { it.item.id }.take(8).toMutableList(),
                anchor = editor.toolbarLook.position.toHudAnchor(),
                presentation = if (editor.toolbarLook.floating) HudPresentation.FLOATING else HudPresentation.DOCKED,
                size = editor.toolbarLook.size.toHudBarSize(),
            ),
            HudBar("Ferramentas rápidas", mutableListOf("pen", "colors", "eraser", "undo"), anchor = HudAnchor.BOTTOM_START),
        )
    }
    val catalog = remember {
        listOf(
            "pen" to "Caneta", "colors" to "Cores", "eraser" to "Borracha", "select" to "Selecionar",
            "shape" to "Forma", "text" to "Texto", "image" to "Imagem", "undo" to "Desfazer",
            "redo" to "Refazer", "group" to "Grupo", "grid" to "Grid 2×2", "spacer" to "Espaçador", "divider" to "Separador",
        )
    }
    val filteredCatalog = catalog.filter { (id, label) ->
        filter.isBlank() || id.contains(filter, ignoreCase = true) || label.contains(filter, ignoreCase = true)
    }

    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(palette.bg.toComposeColor())) {
        HudTopBar(
            profile = profile,
            onProfile = { profile = if (profile == "xnote") "xcanvas" else "xnote" },
            onAddBar = {
                bars.add(
                    HudBar(
                        name = "Barra ${bars.size + 1}",
                        items = mutableListOf(),
                        anchor = HudAnchor.TOP_CENTER,
                        fillWidth = true,
                        size = HudBarSize.REGULAR,
                    ),
                )
                selectedBar = bars.lastIndex
                selectedItem = null
            },
            onOpenCatalog = { catalogOpen = true },
            panelOffsetX = editPanelX,
            panelOffsetY = editPanelY,
            panelDragging = editPanelDragging,
            onPanelDragStart = { editPanelDragging = true },
            onPanelDrag = { dx, dy ->
                editPanelX = (editPanelX + dx).coerceIn(-700f, 700f)
                editPanelY = (editPanelY + dy).coerceIn(-20f, 620f)
            },
            onPanelDragEnd = { editPanelDragging = false },
            expanded = editPanelExpanded,
            onToggleExpanded = { editPanelExpanded = !editPanelExpanded },
            selectedBar = bars[selectedBar],
            selectedItem = selectedItem,
            onChangeBar = { bars[selectedBar] = it },
            onRemoveItem = { item ->
                val items = bars[selectedBar].items.toMutableList()
                items.remove(item)
                bars[selectedBar] = bars[selectedBar].copy(items = items)
                selectedItem = null
            },
            onClose = onClose,
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            HudViewport(
                bars = bars,
                selectedBar = selectedBar,
                selectedItem = selectedItem,
                onSelectBar = { selectedBar = it; selectedItem = null },
                onSelectItem = { selectedItem = it },
                onMoveItem = { from, to ->
                    val items = bars[selectedBar].items.toMutableList()
                    if (from in items.indices && to in items.indices && from != to) {
                        items.add(to, items.removeAt(from))
                        bars[selectedBar] = bars[selectedBar].copy(items = items)
                    }
                },
                onDragItem = { draggedItem = it },
                movingBar = movingBar,
                barDropTarget = barDropTarget,
                onBarDragStart = { index -> selectedBar = index; selectedItem = null; movingBar = index },
                onBarDrag = { index, dx, dy ->
                    val bar = bars[index]
                    val nextX = (bar.offsetXDp + dx).coerceIn(-260f, 260f)
                    val nextY = (bar.offsetYDp + dy).coerceIn(-220f, 220f)
                    bars[index] = bar.copy(offsetXDp = nextX, offsetYDp = nextY)
                    barDropTarget = hudAnchorFor(bar.anchor, nextX, nextY)
                },
                onBarDrop = { index ->
                    val bar = bars[index]
                    bars[index] = bar.copy(anchor = hudAnchorFor(bar.anchor, bar.offsetXDp, bar.offsetYDp), offsetXDp = 0f, offsetYDp = 0f)
                    movingBar = null
                    barDropTarget = null
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (catalogOpen) {
                HudFloatingPanel(Modifier.align(Alignment.TopCenter).padding(top = 12.dp)) {
                    HudCatalog(
                        items = filteredCatalog,
                        filter = filter,
                        onFilter = { filter = it },
                        onAdd = { id ->
                            bars[selectedBar] = bars[selectedBar].copy(items = (bars[selectedBar].items + id).toMutableList())
                            selectedItem = id
                        },
                        onCollapse = { catalogOpen = false },
                    )
                }
            }
            if (propertiesOpen && !editPanelExpanded) {
                HudFloatingPanel(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    HudProperties(
                        bar = bars[selectedBar],
                        selectedItem = selectedItem,
                        onChangeBar = { bars[selectedBar] = it },
                        onRemoveItem = { item ->
                            val items = bars[selectedBar].items.toMutableList()
                            items.remove(item)
                            bars[selectedBar] = bars[selectedBar].copy(items = items)
                            selectedItem = null
                        },
                        onCollapse = { propertiesOpen = false },
                    )
                }
            } else {
                HudRail("Propriedades", XnotesIcons.sliders) { propertiesOpen = true }
            }
        }
    }
}

private data class HudBar(
    val name: String,
    val items: MutableList<String>,
    val paddingDp: Float = 8f,
    val gapDp: Float = 6f,
    val anchor: HudAnchor = HudAnchor.TOP_CENTER,
    val offsetXDp: Float = 0f,
    val offsetYDp: Float = 0f,
    val presentation: HudPresentation = HudPresentation.DOCKED,
    val size: HudBarSize = HudBarSize.REGULAR,
    val fillWidth: Boolean = false,
)

private enum class HudAnchor { TOP_START, TOP_CENTER, TOP_END, CENTER_START, CENTER, CENTER_END, BOTTOM_START, BOTTOM_CENTER, BOTTOM_END }
private enum class HudPresentation { DOCKED, FLOATING }
private enum class HudBarSize(val scale: Float) { COMPACT(0.82f), REGULAR(1f), COMFORTABLE(1.18f) }

private fun hudAnchorFor(origin: HudAnchor, offsetX: Float, offsetY: Float): HudAnchor {
    val (baseX, baseY) = when (origin) {
        HudAnchor.TOP_START -> -300f to -220f
        HudAnchor.TOP_CENTER -> 0f to -220f
        HudAnchor.TOP_END -> 300f to -220f
        HudAnchor.CENTER_START -> -300f to 0f
        HudAnchor.CENTER -> 0f to 0f
        HudAnchor.CENTER_END -> 300f to 0f
        HudAnchor.BOTTOM_START -> -300f to 220f
        HudAnchor.BOTTOM_CENTER -> 0f to 220f
        HudAnchor.BOTTOM_END -> 300f to 220f
    }
    val x = baseX + offsetX
    val y = baseY + offsetY
    val column = when { x < -150f -> 0; x > 150f -> 2; else -> 1 }
    val row = when { y < -110f -> 0; y > 110f -> 2; else -> 1 }
    return HudAnchor.entries[row * 3 + column]
}

private fun ToolbarPosition.toHudAnchor(): HudAnchor = when (this) {
    ToolbarPosition.TOP -> HudAnchor.TOP_CENTER
    ToolbarPosition.BOTTOM -> HudAnchor.BOTTOM_CENTER
    ToolbarPosition.LEFT -> HudAnchor.CENTER_START
    ToolbarPosition.RIGHT -> HudAnchor.CENTER_END
}

private fun ToolbarSize.toHudBarSize(): HudBarSize = when (this) {
    ToolbarSize.COMPACT -> HudBarSize.COMPACT
    ToolbarSize.REGULAR -> HudBarSize.REGULAR
    ToolbarSize.COMFORTABLE -> HudBarSize.COMFORTABLE
}

@Composable
private fun HudTopBar(
    profile: String,
    onProfile: () -> Unit,
    onAddBar: () -> Unit,
    onOpenCatalog: () -> Unit,
    panelOffsetX: Float,
    panelOffsetY: Float,
    panelDragging: Boolean,
    onPanelDragStart: () -> Unit,
    onPanelDrag: (Float, Float) -> Unit,
    onPanelDragEnd: () -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedBar: HudBar,
    selectedItem: String?,
    onChangeBar: (HudBar) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current.density
    val panelScale by animateFloatAsState(if (panelDragging) 1.04f else 1f, spring(), label = "editPanelScale")
    Row(
        Modifier.fillMaxWidth().background(palette.panel.toComposeColor()).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HudIconButton(XnotesIcons.prev, "Voltar", onClose)
        Text("Editor de barras", color = palette.text.toComposeColor(), fontSize = 18.sp)
        Spacer(Modifier.width(16.dp))
        HudFloatingPanel(
            Modifier.padding(horizontal = 4.dp).offset(x = panelOffsetX.dp, y = panelOffsetY.dp)
                .graphicsLayer { scaleX = panelScale; scaleY = panelScale }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onPanelDragStart() },
                        onDragCancel = onPanelDragEnd,
                        onDragEnd = onPanelDragEnd,
                        onDrag = { change, amount -> change.consume(); onPanelDrag(amount.x / density, amount.y / density) },
                    )
                },
        ) {
            Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(XnotesIcons.grip, "Arrastar painel", Modifier.size(18.dp), tint = palette.textDim.toComposeColor())
                HudChip(profile, selected = true, onClick = onProfile)
                HudChip("Compartilhado", selected = false, onClick = {})
                HudChip("+ Componente", selected = false, onClick = onOpenCatalog)
                HudChip("+ Barra", selected = false, onClick = onAddBar)
                HudIconButton(XnotesIcons.undo, "Desfazer", onClick = {})
                HudIconButton(XnotesIcons.redo, "Refazer", onClick = {})
                HudChip(if (expanded) "Recolher propriedades" else "Editar seleção", selected = expanded, onClick = onToggleExpanded)
              }
              if (expanded) {
                  HudProperties(
                      bar = selectedBar,
                      selectedItem = selectedItem,
                      onChangeBar = onChangeBar,
                      onRemoveItem = onRemoveItem,
                      onCollapse = onToggleExpanded,
                  )
              }
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onClose) { Text("Cancelar") }
        TextButton(onClick = onClose) { Text("Aplicar") }
    }
}

@Composable
private fun HudFloatingPanel(modifier: Modifier, content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    Box(
        modifier.shadow(10.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
            .background(palette.panel.toComposeColor().copy(alpha = 0.94f))
            .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(14.dp)),
    ) { content() }
}

@Composable
private fun HudAddButton(modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(modifier.size(52.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(palette.accent.toComposeColor()).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(XnotesIcons.plus, "Adicionar componente", Modifier.size(25.dp), tint = palette.selectionForeground.toComposeColor())
    }
}

@Composable
private fun HudCatalog(
    items: List<Pair<String, String>>,
    filter: String,
    onFilter: (String) -> Unit,
    onAdd: (String) -> Unit,
    onCollapse: () -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        Modifier.width(260.dp).heightIn(max = 520.dp).background(palette.panel.toComposeColor()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HudPanelTitle("Catálogo", XnotesIcons.layers, onCollapse)
        OutlinedTextField(
            value = filter,
            onValueChange = onFilter,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Pesquisar") },
            leadingIcon = { Icon(XnotesIcons.search, null, Modifier.size(18.dp)) },
        )
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { (id, label) ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { onAdd(id) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HudItemIcon(id, selected = false)
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = palette.text.toComposeColor(), fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(XnotesIcons.plus, "Adicionar", Modifier.size(16.dp), tint = palette.textDim.toComposeColor())
                }
            }
        }
    }
}

@Composable
private fun HudViewport(
    bars: List<HudBar>,
    selectedBar: Int,
    selectedItem: String?,
    onSelectBar: (Int) -> Unit,
    onSelectItem: (String) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onDragItem: (String?) -> Unit,
    movingBar: Int?,
    barDropTarget: HudAnchor?,
    onBarDragStart: (Int) -> Unit,
    onBarDrag: (Int, Float, Float) -> Unit,
    onBarDrop: (Int) -> Unit,
    modifier: Modifier,
) {
    val palette = LocalPalette.current
    var zoom by remember { mutableStateOf(1f) }

    Column(modifier.background(palette.bg.toComposeColor()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nota infinita · ajuste em escala real", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("${(zoom * 100).toInt()}%", color = palette.text.toComposeColor(), fontSize = 12.sp)
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp))
                .background(palette.paper.toComposeColor()),
        ) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                    }
                    .drawBehind {
                        val rule = palette.paperBorder.toComposeColor().copy(alpha = 0.20f)
                        val step = 44.dp.toPx()
                        var y = 0f
                        while (y < size.height) {
                            drawLine(rule, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f)
                            y += step
                        }
                        var x = 0f
                        while (x < size.width) {
                            drawLine(rule.copy(alpha = 0.09f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f)
                            x += step
                        }
                    },
            ) {
                if (movingBar != null && barDropTarget != null) HudAnchorHints(barDropTarget)
                Column(Modifier.padding(58.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Nota sem bordas", color = palette.text.toComposeColor().copy(alpha = 0.40f), fontSize = 18.sp)
                    Text("Esta área é uma referência visual da nota final; somente as barras recebem edição.", color = palette.textDim.toComposeColor().copy(alpha = 0.48f), fontSize = 13.sp)
                    Spacer(Modifier.height(110.dp))
                    repeat(5) { line ->
                        Box(Modifier.fillMaxWidth(if (line == 2) 0.62f else 0.82f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(palette.textDim.toComposeColor().copy(alpha = 0.08f)))
                    }
                }
                bars.forEachIndexed { index, bar ->
                    val stackIndex = bars.take(index).count { it.anchor == bar.anchor }
                    HudComposedBar(
                        bar = bar,
                        selected = selectedBar == index,
                        selectedItem = selectedItem,
                        onSelectBar = { onSelectBar(index) },
                        onSelectItem = onSelectItem,
                        onMoveItem = onMoveItem,
                        onDragItem = onDragItem,
                        dragging = movingBar == index,
                        onBarDragStart = { onBarDragStart(index) },
                        onBarDrag = { dx, dy -> onBarDrag(index, dx, dy) },
                        onBarDrop = { onBarDrop(index) },
                        modifier = hudAnchorModifier(bar.anchor, stackIndex)
                            .offset(x = bar.offsetXDp.dp, y = bar.offsetYDp.dp)
                            .then(if (bar.fillWidth) Modifier.fillMaxWidth().padding(horizontal = 28.dp) else Modifier),
                    )
                }
            }
            HudFloatingPanel(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                Row(Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HudIconButton(XnotesIcons.zoomOut, "Reduzir", onClick = { zoom = (zoom - 0.05f).coerceAtLeast(0.35f) })
                    HudChip("${(zoom * 100).toInt()}%", selected = false, onClick = { zoom = 1f })
                    HudIconButton(XnotesIcons.zoomIn, "Ampliar", onClick = { zoom = (zoom + 0.05f).coerceAtMost(1.5f) })
                    HudIconButton(XnotesIcons.fit, "Escala real", onClick = { zoom = 1f })
                }
            }
        }
        Text("A nota é apenas uma simulação visual. Toque no contorno da barra para selecioná-la; toque no widget para selecionar sua instância.", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
    }
}

private fun BoxScope.hudAnchorModifier(anchor: HudAnchor, stackIndex: Int): Modifier = when (anchor) {
    HudAnchor.TOP_START -> Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = (28 + stackIndex * 68).dp)
    HudAnchor.TOP_CENTER -> Modifier.align(Alignment.TopCenter).padding(top = (28 + stackIndex * 68).dp)
    HudAnchor.TOP_END -> Modifier.align(Alignment.TopEnd).padding(end = 28.dp, top = (28 + stackIndex * 68).dp)
    HudAnchor.CENTER_START -> Modifier.align(Alignment.CenterStart).padding(start = 28.dp)
    HudAnchor.CENTER -> Modifier.align(Alignment.Center)
    HudAnchor.CENTER_END -> Modifier.align(Alignment.CenterEnd).padding(end = 28.dp)
    HudAnchor.BOTTOM_START -> Modifier.align(Alignment.BottomStart).padding(start = 28.dp, bottom = (28 + stackIndex * 68).dp)
    HudAnchor.BOTTOM_CENTER -> Modifier.align(Alignment.BottomCenter).padding(bottom = (28 + stackIndex * 68).dp)
    HudAnchor.BOTTOM_END -> Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = (28 + stackIndex * 68).dp)
}

@Composable
private fun HudAnchorHints(target: HudAnchor) {
    val palette = LocalPalette.current
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        HudAnchorHint(Modifier.align(Alignment.TopStart), HudAnchor.TOP_START, target)
        HudAnchorHint(Modifier.align(Alignment.TopCenter), HudAnchor.TOP_CENTER, target)
        HudAnchorHint(Modifier.align(Alignment.TopEnd), HudAnchor.TOP_END, target)
        HudAnchorHint(Modifier.align(Alignment.CenterStart), HudAnchor.CENTER_START, target)
        HudAnchorHint(Modifier.align(Alignment.Center), HudAnchor.CENTER, target)
        HudAnchorHint(Modifier.align(Alignment.CenterEnd), HudAnchor.CENTER_END, target)
        HudAnchorHint(Modifier.align(Alignment.BottomStart), HudAnchor.BOTTOM_START, target)
        HudAnchorHint(Modifier.align(Alignment.BottomCenter), HudAnchor.BOTTOM_CENTER, target)
        HudAnchorHint(Modifier.align(Alignment.BottomEnd), HudAnchor.BOTTOM_END, target)
        Text("Soltar para encaixar · ${target.name.lowercase().replace('_', ' ')}", color = palette.accent.toComposeColor(), fontSize = 11.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
    }
}

@Composable
private fun HudAnchorHint(modifier: Modifier, anchor: HudAnchor, target: HudAnchor) {
    val palette = LocalPalette.current
    Box(modifier.size(if (anchor == target) 40.dp else 22.dp).clip(RoundedCornerShape(6.dp)).background(if (anchor == target) palette.accent.toComposeColor().copy(alpha = 0.25f) else palette.textDim.toComposeColor().copy(alpha = 0.10f)).border(1.dp, if (anchor == target) palette.accent.toComposeColor() else palette.border.toComposeColor(), RoundedCornerShape(6.dp)))
}

@Composable
private fun HudComposedBar(
    bar: HudBar,
    selected: Boolean,
    selectedItem: String?,
    onSelectBar: () -> Unit,
    onSelectItem: (String) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onDragItem: (String?) -> Unit,
    dragging: Boolean,
    onBarDragStart: () -> Unit,
    onBarDrag: (Float, Float) -> Unit,
    onBarDrop: () -> Unit,
    modifier: Modifier,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current.density
    val elevation by animateDpAsState(
        if (dragging || bar.presentation == HudPresentation.FLOATING) 8.dp else if (selected) 3.dp else 1.dp,
        spring(),
        label = "barElevation",
    )
    val interactionScale by animateFloatAsState(if (dragging) 1.04f else if (selected) 1.015f else 1f, spring(), label = "barScale")
    Box(
        modifier.shadow(elevation, RoundedCornerShape(12.dp)).graphicsLayer {
            scaleX = bar.size.scale * interactionScale
            scaleY = bar.size.scale * interactionScale
            alpha = if (dragging) 0.78f else 1f
        }
            .pointerInput(bar.name) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onBarDragStart() },
                    onDragCancel = onBarDrop,
                    onDragEnd = onBarDrop,
                    onDrag = { change, amount -> change.consume(); onBarDrag(amount.x / density, amount.y / density) },
                )
            }
            .clip(RoundedCornerShape(12.dp)).background(palette.panel.toComposeColor().copy(alpha = 0.97f))
            .border(2.dp, if (selected) palette.accent.toComposeColor() else palette.border.toComposeColor(), RoundedCornerShape(14.dp))
            .clickable(onClick = onSelectBar).padding(bar.paddingDp.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(bar.name, color = palette.textDim.toComposeColor(), fontSize = 8.sp, modifier = Modifier.padding(start = 2.dp))
            if (bar.items.isEmpty()) {
                Text(
                    "Barra vazia · use + Componente para adicionar",
                    color = palette.textDim.toComposeColor(),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(bar.gapDp.dp)) {
                    HudWidgetCell("↤", "Principal", selected && selectedItem == "home", onClick = { onSelectItem("home") }, tall = true)
                    HudColorGrid(selected && selectedItem == "colors", onClick = { onSelectItem("colors") })
                    HudDivider()
                    HudToolGrid(selected && selectedItem == "tools", onClick = { onSelectItem("tools") })
                    HudWidgetCell("↶", "Desfazer", selected && selectedItem == "undo", onClick = { onSelectItem("undo") }, tall = true)
                    HudWidgetCell("↷", "Refazer", selected && selectedItem == "redo", onClick = { onSelectItem("redo") }, tall = true)
                    if (bar.items.size > 8) HudWidgetCell("+${bar.items.size - 8}", "Mais", selected && selectedItem == "more", onClick = { onSelectItem("more") })
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    bar.items.forEachIndexed { index, item ->
                        HudActualSlot(
                            item = item,
                            selected = selected && selectedItem == item,
                            onClick = { onSelectItem(item) },
                            onMove = { delta -> onMoveItem(index, (index + delta).coerceIn(0, bar.items.lastIndex)) },
                            onDrag = onDragItem,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HudActualSlot(
    item: String,
    selected: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onDrag: (String?) -> Unit,
) {
    var totalDrag by remember(item) { mutableStateOf(0f) }
    val scale by animateFloatAsState(if (totalDrag != 0f) 1.10f else if (selected) 1.05f else 1f, spring(), label = "slotScale")
    val direction = when {
        totalDrag > 24f -> 1
        totalDrag < -24f -> -1
        else -> 0
    }
    Box {
        HudItemIcon(
            id = item,
            selected = selected,
            onClick = onClick,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.pointerInput(item) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { totalDrag = 0f; onDrag(item) },
                    onDragCancel = { totalDrag = 0f; onDrag(null) },
                    onDragEnd = {
                        if (direction != 0) onMove(direction)
                        totalDrag = 0f
                        onDrag(null)
                    },
                    onDrag = { change, amount -> change.consume(); totalDrag += amount.x },
                )
            },
        )
        if (direction != 0) {
            val palette = LocalPalette.current
            Box(Modifier.align(if (direction > 0) Alignment.CenterEnd else Alignment.CenterStart).width(4.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(palette.accent.toComposeColor()))
        }
    }
}

@Composable
private fun HudColorGrid(selected: Boolean, onClick: () -> Unit) {
    val colors = listOf(Color(0xffe45756), Color(0xffffb627), Color(0xff5bc0eb), Color(0xff6cc551), Color(0xff9b5de5), Color(0xfff15bb5), Color(0xff00bbf9), Color(0xff555b6e))
    Column(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        colors.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { color ->
                    Box(Modifier.size(22.dp).clip(CircleShape).background(color).border(if (selected) 2.dp else 1.dp, if (selected) Color.White else Color.Transparent, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun HudToolGrid(selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { HudWidgetCell("✎", "Caneta", selected, onClick); HudWidgetCell("⌁", "Forma", selected, onClick) }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { HudWidgetCell("T", "Texto", selected, onClick); HudWidgetCell("⌖", "Selecionar", selected, onClick) }
    }
}

@Composable
private fun HudDivider() {
    val palette = LocalPalette.current
    Box(Modifier.width(1.dp).height(42.dp).background(palette.border.toComposeColor()))
}

@Composable
private fun HudWidgetCell(symbol: String, label: String, selected: Boolean, onClick: () -> Unit, tall: Boolean = false) {
    val palette = LocalPalette.current
    Box(
        Modifier.width(if (tall) 38.dp else 27.dp).height(if (tall) 42.dp else 21.dp)
            .clip(RoundedCornerShape(5.dp)).background(if (selected) palette.selectionBackground.toComposeColor() else palette.surface.toComposeColor())
            .border(1.dp, if (selected) palette.accent.toComposeColor() else palette.border.toComposeColor(), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(symbol, color = palette.text.toComposeColor(), fontSize = if (tall) 14.sp else 10.sp)
            if (tall) Text(label.take(7), color = palette.textDim.toComposeColor(), fontSize = 6.sp)
        }
    }
}

@Composable
private fun HudBarPreview(
    bar: HudBar,
    selected: Boolean,
    selectedItem: String?,
    onSelectBar: () -> Unit,
    onSelectItem: (String) -> Unit,
) {
    val palette = LocalPalette.current
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(palette.panel.toComposeColor())
            .border(2.dp, if (selected) palette.accent.toComposeColor() else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelectBar).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(bar.name, color = palette.textDim.toComposeColor(), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
        bar.items.forEach { item ->
            HudItemIcon(item, selected = selected && selectedItem == item, onClick = { onSelectItem(item) })
        }
    }
}

@Composable
private fun HudProperties(
    bar: HudBar,
    selectedItem: String?,
    onChangeBar: (HudBar) -> Unit,
    onRemoveItem: (String) -> Unit,
    onCollapse: () -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        Modifier.width(240.dp).heightIn(max = 480.dp).verticalScroll(rememberScrollState())
            .background(palette.panel.toComposeColor()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HudPanelTitle("Propriedades", XnotesIcons.sliders, onCollapse)
        Text(if (selectedItem == null) "Barra" else "Instância: $selectedItem", color = palette.text.toComposeColor(), fontSize = 15.sp)
        if (selectedItem == null) {
            OutlinedTextField(
                value = bar.name,
                onValueChange = { onChangeBar(bar.copy(name = it.take(32))) },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Padding · ${bar.paddingDp.toInt()} dp", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
            Slider(
                value = bar.paddingDp,
                onValueChange = { onChangeBar(bar.copy(paddingDp = it)) },
                valueRange = 0f..24f,
            )
            Text("Gap · ${bar.gapDp.toInt()} dp", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
            Slider(
                value = bar.gapDp,
                onValueChange = { onChangeBar(bar.copy(gapDp = it)) },
                valueRange = 0f..20f,
            )
            Text("Apresentação", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HudChip("Docked", bar.presentation == HudPresentation.DOCKED) {
                    onChangeBar(bar.copy(presentation = HudPresentation.DOCKED))
                }
                HudChip("Floating", bar.presentation == HudPresentation.FLOATING) {
                    onChangeBar(bar.copy(presentation = HudPresentation.FLOATING))
                }
            }
            Text("Tamanho", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HudChip("Compact", bar.size == HudBarSize.COMPACT) { onChangeBar(bar.copy(size = HudBarSize.COMPACT)) }
                HudChip("Regular", bar.size == HudBarSize.REGULAR) { onChangeBar(bar.copy(size = HudBarSize.REGULAR)) }
                HudChip("Confortável", bar.size == HudBarSize.COMFORTABLE) { onChangeBar(bar.copy(size = HudBarSize.COMFORTABLE)) }
            }
            Text("Posição", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HudChip("Topo", bar.anchor in setOf(HudAnchor.TOP_START, HudAnchor.TOP_CENTER, HudAnchor.TOP_END)) {
                    onChangeBar(bar.copy(anchor = HudAnchor.TOP_CENTER, offsetXDp = 0f, offsetYDp = 0f))
                }
                HudChip("Base", bar.anchor in setOf(HudAnchor.BOTTOM_START, HudAnchor.BOTTOM_CENTER, HudAnchor.BOTTOM_END)) {
                    onChangeBar(bar.copy(anchor = HudAnchor.BOTTOM_CENTER, offsetXDp = 0f, offsetYDp = 0f))
                }
                HudChip("Lado", bar.anchor in setOf(HudAnchor.CENTER_START, HudAnchor.CENTER_END)) {
                    onChangeBar(bar.copy(anchor = HudAnchor.CENTER_START, offsetXDp = 0f, offsetYDp = 0f))
                }
            }
            HudProperty("Largura", if (bar.fillWidth) "Tela inteira" else "Conteúdo")
            Text("Estas alterações são do rascunho visual; persistência e Aplicar entram no motor M2/M5.", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
        } else {
            HudProperty("Tipo", selectedItem)
            HudProperty("Instância", "ID independente · seleção contextual")
            TextButton(onClick = { onRemoveItem(selectedItem) }) { Text("Remover da barra") }
            Text("Selecione a barra para editar seu nome e espaçamento.", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun HudProperty(label: String, value: String) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = palette.textDim.toComposeColor(), fontSize = 11.sp)
        Text(value, color = palette.text.toComposeColor(), fontSize = 13.sp)
    }
}

@Composable
private fun HudPanelTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onCollapse: () -> Unit) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = palette.text.toComposeColor())
        Spacer(Modifier.width(8.dp))
        Text(title, color = palette.text.toComposeColor(), fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        HudIconButton(XnotesIcons.close, "Recolher", onCollapse)
    }
}

@Composable
private fun HudRail(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier.width(48.dp).fillMaxHeight().background(palette.panel.toComposeColor()).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = palette.text.toComposeColor())
        Text("+", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
    }
}

@Composable
private fun HudIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(Modifier.size(38.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, Modifier.size(20.dp), tint = palette.text.toComposeColor())
    }
}

@Composable
private fun HudChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Text(
        label,
        color = if (selected) palette.selectionForeground.toComposeColor() else palette.text.toComposeColor(),
        fontSize = 12.sp,
        modifier = Modifier.clip(MaterialTheme.shapes.small)
            .background(if (selected) palette.selectionBackground.toComposeColor() else palette.surface.toComposeColor())
            .border(1.dp, if (selected) palette.accent.toComposeColor() else palette.border.toComposeColor(), MaterialTheme.shapes.small)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun HudItemIcon(
    id: String,
    selected: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val label = id.replace('_', ' ').replaceFirstChar { it.uppercase() }
    Box(
        modifier.size(42.dp).clip(MaterialTheme.shapes.small)
            .background(if (selected) palette.selectionBackground.toComposeColor() else palette.surface.toComposeColor())
            .border(1.dp, if (selected) palette.accent.toComposeColor() else palette.border.toComposeColor(), MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val image = hudItemIcon(id)
        if (image != null) {
            Icon(image, label, Modifier.size(20.dp), tint = if (selected) palette.selectionForeground.toComposeColor() else palette.text.toComposeColor())
        } else {
            Text(label.take(3), color = if (selected) palette.selectionForeground.toComposeColor() else palette.text.toComposeColor(), fontSize = 11.sp)
        }
    }
}

@Composable
private fun hudItemIcon(id: String): ImageVector? = when (id) {
    "pen" -> ImageVector.vectorResource(R.drawable.ic_stroke_regular)
    "colors" -> XnotesIcons.palette
    "eraser" -> XnotesIcons.eraser
    "select" -> XnotesIcons.select
    "shape" -> XnotesIcons.shape
    "text" -> XnotesIcons.text
    "image" -> XnotesIcons.image
    "undo" -> XnotesIcons.undo
    "redo" -> XnotesIcons.redo
    "tape" -> XnotesIcons.tape
    "group" -> XnotesIcons.layers
    "grid" -> XnotesIcons.layoutGrid
    "divider" -> XnotesIcons.minus
    "spacer" -> XnotesIcons.more
    else -> null
}
