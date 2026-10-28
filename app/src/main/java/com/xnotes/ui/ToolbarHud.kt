package com.xnotes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

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
    var filter by remember { mutableStateOf("") }
    val bars = remember {
        mutableStateListOf(
            HudBar("Barra principal", editor.toolbarLayout.sections.flatMap { it.visibleEntries }.map { it.item.id }.take(8).toMutableList()),
            HudBar("Ferramentas rápidas", mutableListOf("pen", "colors", "eraser", "undo")),
        )
    }
    val catalog = remember {
        listOf(
            "pen" to "Caneta", "colors" to "Cores", "eraser" to "Borracha", "select" to "Selecionar",
            "shape" to "Forma", "text" to "Texto", "image" to "Imagem", "undo" to "Desfazer",
            "redo" to "Refazer", "spacer" to "Espaçador", "divider" to "Separador",
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
            onClose = onClose,
        )
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (catalogOpen) {
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
            } else {
                HudRail("Catálogo", XnotesIcons.layers) { catalogOpen = true }
            }

            HudViewport(
                bars = bars,
                selectedBar = selectedBar,
                selectedItem = selectedItem,
                onSelectBar = { selectedBar = it; selectedItem = null },
                onSelectItem = { selectedItem = it },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )

            if (propertiesOpen) {
                HudProperties(
                    bar = bars[selectedBar],
                    selectedItem = selectedItem,
                    onCollapse = { propertiesOpen = false },
                )
            } else {
                HudRail("Propriedades", XnotesIcons.sliders) { propertiesOpen = true }
            }
        }
    }
}

private data class HudBar(val name: String, val items: MutableList<String>)

@Composable
private fun HudTopBar(profile: String, onProfile: () -> Unit, onClose: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().background(palette.panel.toComposeColor()).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HudIconButton(XnotesIcons.prev, "Voltar", onClose)
        Text("Editor de barras", color = palette.text.toComposeColor(), fontSize = 18.sp)
        Spacer(Modifier.width(16.dp))
        HudChip(profile, selected = true, onClick = onProfile)
        HudChip("Compartilhado", selected = false, onClick = {})
        Spacer(Modifier.weight(1f))
        HudIconButton(XnotesIcons.undo, "Desfazer", onClick = {})
        HudIconButton(XnotesIcons.redo, "Refazer", onClick = {})
        TextButton(onClick = onClose) { Text("Cancelar") }
        TextButton(onClick = onClose) { Text("Aplicar") }
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
        Modifier.width(220.dp).fillMaxHeight().background(palette.panel.toComposeColor()).padding(10.dp),
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
    modifier: Modifier,
) {
    val palette = LocalPalette.current
    Box(modifier.background(palette.paper.toComposeColor()).padding(20.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Pré-visualização da nota (fundo não interativo)", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(palette.bg.toComposeColor())
                    .border(1.dp, palette.paperBorder.toComposeColor(), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    bars.forEachIndexed { index, bar ->
                        HudBarPreview(
                            bar = bar,
                            selected = selectedBar == index,
                            selectedItem = selectedItem,
                            onSelectBar = { onSelectBar(index) },
                            onSelectItem = onSelectItem,
                        )
                    }
                }
            }
            Text("Toque no espaço vazio para selecionar a barra; toque em um item para selecionar a instância.", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
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
            HudItemIcon(item, selected = selected && selectedItem == item) { onSelectItem(item) }
        }
    }
}

@Composable
private fun HudProperties(bar: HudBar, selectedItem: String?, onCollapse: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier.width(240.dp).fillMaxHeight().background(palette.panel.toComposeColor()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HudPanelTitle("Propriedades", XnotesIcons.sliders, onCollapse)
        Text(if (selectedItem == null) "Barra" else "Instância: $selectedItem", color = palette.text.toComposeColor(), fontSize = 15.sp)
        HudProperty("Nome", if (selectedItem == null) bar.name else selectedItem)
        HudProperty("Layout", "Row · encaixe automático")
        HudProperty("Padding", "8 dp")
        HudProperty("Gap", "6 dp")
        HudProperty("Aparência", "Regular · bordas arredondadas")
        Text("As propriedades exibidas são apenas protótipo M1; ainda não persistem alterações.", color = palette.textDim.toComposeColor(), fontSize = 12.sp)
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
private fun HudItemIcon(id: String, selected: Boolean, onClick: () -> Unit = {}) {
    val palette = LocalPalette.current
    val label = id.replace('_', ' ').replaceFirstChar { it.uppercase() }
    Box(
        Modifier.size(42.dp).clip(MaterialTheme.shapes.small)
            .background(if (selected) palette.selectionBackground.toComposeColor() else palette.surface.toComposeColor())
            .border(1.dp, if (selected) palette.accent.toComposeColor() else palette.border.toComposeColor(), MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.take(3), color = if (selected) palette.selectionForeground.toComposeColor() else palette.text.toComposeColor(), fontSize = 11.sp)
    }
}
