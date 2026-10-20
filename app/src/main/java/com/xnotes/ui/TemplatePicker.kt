package com.xnotes.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import com.xnotes.core.geometry.Rect
import com.xnotes.core.model.PageStyle
import com.xnotes.core.model.Rgba
import com.xnotes.core.template.Template
import com.xnotes.core.template.TemplatePage
import com.xnotes.core.template.TemplatePainter
import com.xnotes.core.template.TemplateLayer
import com.xnotes.core.template.TemplateOutput
import com.xnotes.core.template.TPrim
import com.xnotes.core.template.TemplateValues
import com.xnotes.platform.AndroidRenderer
import com.xnotes.platform.CustomAssetFolders
import com.xnotes.platform.TemplateLibrary
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.max

/** Bumped when the template library changes, so open pickers list it again. */
internal object TemplateLibraryUi {
    var version by mutableIntStateOf(0)
}

private val THUMB_W = 102.dp
private val TILE_W = 108.dp

/**
 * A row of template previews drawn at the current page's proportions, the [selected] one outlined,
 * ending in an Import tile. A long press on an imported template offers to remove it, and on one
 * only this note carries, to add it to the library.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TemplateStrip(
    entries: List<TemplateLibrary.Entry>,
    selected: String?,
    onClear: () -> Unit,
    pageMm: Pair<Double, Double>,
    showClear: Boolean = true,
    ink: Rgba,
    accent: Rgba,
    paper: Rgba,
    selectedStyle: PageStyle? = null,
    onSelect: (String) -> Unit,
    onImport: () -> Unit,
    onRemove: (String) -> Unit,
    onKeep: (String) -> Unit,
) {
    val palette = LocalPalette.current
    val aspect = (pageMm.second / pageMm.first).coerceIn(0.5, 2.0)
    val thumbH = THUMB_W * aspect.toFloat()
    val wPx = thumbPx(THUMB_W)
    // Not a LazyRow: the popup measures its content's intrinsic size, which lazy lists refuse.
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showClear && selected != null && selected != com.xnotes.core.model.PageTemplates.NONE) {
            val active = entries.firstOrNull { it.key == selected }
                ?: TemplateLibrary.entry(selected)
            Column(
                Modifier.width(TILE_W).clip(MaterialTheme.shapes.extraSmall)
                    .combinedClickable(onClick = onClear).padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width(TILE_W).height(36.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.none), color = palette.text.toComposeColor(), fontSize = 11.sp)
                }
                Box(
                    Modifier.size(THUMB_W, thumbH)
                        .border(1.dp, palette.accent.toComposeColor(), RoundedCornerShape(3.dp))
                        .clip(RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (active != null) {
                        val thumb = rememberTemplateThumb(
                            active.template,
                            TemplateThumbs.spec(active.key, pageMm, ink, accent, paper,
                                TemplateValues.DEFAULTS, wPx),
                            keep = true,
                        )
                        if (thumb != null) {
                            Image(thumb, contentDescription = null,
                                modifier = Modifier.size(THUMB_W, thumbH).blur(5.dp))
                        }
                    }
                    Box(Modifier.matchParentSize().background(palette.surface.toComposeColor().copy(alpha = 0.42f)))
                    Text("×", color = palette.text.toComposeColor(), fontSize = 27.sp)
                }
                Spacer(Modifier.height(3.dp))
            }
        }
        for (e in entries) key(e.key) {
            var menu by remember { mutableStateOf(false) }
            Column(
                Modifier
                    .width(TILE_W)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .combinedClickable(
                        onClick = { onSelect(e.key) },
                        onLongClick = {
                            if (e.source == TemplateLibrary.Source.IMPORTED || e.source == TemplateLibrary.Source.NOTE) menu = true
                        },
                    )
                    .padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Reserve two lines for every title so longer names never shift previews.
                Box(Modifier.width(TILE_W).height(36.dp), contentAlignment = Alignment.Center) {
                    Text(
                        e.template.name,
                        color = palette.text.toComposeColor(),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
                val sel = e.key == selected
                Box(
                    Modifier
                        .size(THUMB_W, thumbH)
                        .border(if (sel) 2.dp else 1.dp, (if (sel) palette.accent else palette.border).toComposeColor(), RoundedCornerShape(3.dp))
                        .padding(if (sel) 2.dp else 1.dp),
                ) {
                    // Use the selected template's custom colour/number parameters too.
                    // Other tiles remain generic previews of their own defaults.
                    val values = if (sel && selectedStyle != null) {
                        val numbers = HashMap(selectedStyle.params ?: emptyMap())
                        e.template.spacingParam?.let { spacing ->
                            selectedStyle.spacing?.let {
                                numbers[spacing.name] = it * 25.4 / com.xnotes.core.model.PageSize.DEFAULT_DPI
                            }
                        }
                        TemplateValues(numbers, selectedStyle.colors ?: emptyMap())
                    } else TemplateValues.DEFAULTS
                    val bmp = rememberTemplateThumb(e.template, TemplateThumbs.spec(e.key, pageMm, ink, accent, paper, values, wPx), keep = true)
                    if (bmp != null) Image(bmp, contentDescription = e.template.name, Modifier.size(THUMB_W, thumbH))
                }
                Spacer(Modifier.height(3.dp))
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (e.source == TemplateLibrary.Source.IMPORTED) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.remove_template)) }, onClick = { menu = false; onRemove(e.key) })
                    } else {
                        DropdownMenuItem(text = { Text(stringResource(R.string.keep_template)) }, onClick = { menu = false; onKeep(e.key) })
                    }
                }
            }
        }
        Column(
            Modifier
                .width(TILE_W)
                .clip(MaterialTheme.shapes.extraSmall)
                .combinedClickable(onClick = onImport)
                .padding(vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(TILE_W).height(36.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.import_template), color = palette.text.toComposeColor(), fontSize = 11.sp)
            }
            Box(
                Modifier
                    .size(THUMB_W, thumbH)
                    .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(3.dp))
                    .background(palette.surface.toComposeColor(), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = palette.textDim.toComposeColor(), fontSize = 20.sp)
            }
            Spacer(Modifier.height(3.dp))
        }
    }
}

/**
 * Show a legible preview and metadata for the selected template, then expose only the controls
 * declared by that template. The values are stored on the selected page/cover style, not applied
 * to an open note, and both creation tabs have independent state.
 */
@Composable
internal fun CreationTemplateSettings(
    entry: TemplateLibrary.Entry?,
    style: PageStyle,
    pageMm: Pair<Double, Double>,
    ink: Rgba,
    accent: Rgba,
    paper: Rgba,
    onChange: (PageStyle) -> Unit,
    coverColors: List<Rgba> = emptyList(),
    onCoverColorChange: ((Rgba) -> Unit)? = null,
    recentColors: List<Rgba> = emptyList(),
) {
    if (entry == null && onCoverColorChange == null) return
    val palette = LocalPalette.current
    val template = entry?.template
    var customizing by remember(entry?.key) { mutableStateOf(false) }
    var inkPickerOpen by remember { mutableStateOf(false) }
    var accentPickerOpen by remember { mutableStateOf(false) }
    val numbers = HashMap(style.params ?: emptyMap())
    template?.spacingParam?.let { spacing ->
        style.spacing?.let { numbers[spacing.name] = it * 25.4 / com.xnotes.core.model.PageSize.DEFAULT_DPI }
    }
    val previewInk = style.patternColor ?: ink
    val previewAccent = style.accentColor ?: accent
    if (template == null) {
        ModeChip(stringResource(R.string.create_template_settings), customizing) { customizing = !customizing }
    } else Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TemplatePreview(
            template, entry.key, pageMm, previewInk, previewAccent, paper,
            TemplateValues(numbers, style.colors ?: emptyMap()), 112.dp,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                template.name,
                color = palette.text.toComposeColor(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            template.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    description,
                    color = palette.textDim.toComposeColor(),
                    fontSize = 11.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ModeChip(stringResource(R.string.create_template_settings), customizing) {
                customizing = !customizing
            }
        }
    }
    if (customizing) {
        if (onCoverColorChange != null) {
            Text(stringResource(R.string.create_cover_colour),
                color = palette.textDim.toComposeColor(), fontSize = 12.sp)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                coverColors.forEach { color ->
                    Box(
                        Modifier.size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.toComposeColor())
                            .border(if (paper == color) 2.dp else 1.dp,
                                if (paper == color) palette.accent.toComposeColor()
                                else palette.border.toComposeColor(), RoundedCornerShape(6.dp))
                            .combinedClickable(onClick = { onCoverColorChange(color) }),
                    )
                }
                Box {
                    var colorPickerOpen by remember { mutableStateOf(false) }
                    ModeChip(stringResource(R.string.create_custom_colour),
                        paper !in coverColors) { colorPickerOpen = true }
                    if (colorPickerOpen) {
                        ColorPickerPopup(paper, recentColors, { colorPickerOpen = false }) {
                            onCoverColorChange(it)
                        }
                    }
                }
            }
        }
        if (template != null) Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box {
                ModeChip(stringResource(R.string.create_template_ink_colour), inkPickerOpen) {
                    inkPickerOpen = true
                    accentPickerOpen = false
                }
                if (inkPickerOpen) {
                    ColorPickerPopup(previewInk, emptyList(), { inkPickerOpen = false }) { color ->
                        onChange(style.copy(patternColor = color))
                    }
                }
            }
            Box {
                ModeChip(stringResource(R.string.caption_accent_colour), accentPickerOpen) {
                    accentPickerOpen = true
                    inkPickerOpen = false
                }
                if (accentPickerOpen) {
                    ColorPickerPopup(previewAccent, emptyList(), { accentPickerOpen = false }) { color ->
                        onChange(style.copy(accentColor = color))
                    }
                }
            }
        }
        if (template != null && template.params.isNotEmpty()) {
            TemplateParamControls(
                template, style, lower = null,
                dpi = com.xnotes.core.model.PageSize.DEFAULT_DPI,
                apply = onChange,
            )
        }
    }
}

/** Consolidate synonymous .xtemplate tags while retaining less common user-defined categories. */
internal fun canonicalTemplateTag(raw: String): String = when (raw.trim().lowercase()) {
    "cover", "covers" -> "cover"
    "page", "pages" -> "page"
    "line", "lines", "lined", "ruled", "ruling" -> "lines"
    "grid", "grids", "graph" -> "grid"
    "dot", "dots", "dotted" -> "dots"
    "plan", "planning", "planner", "planners" -> "planner"
    "write", "writing" -> "writing"
    "studies", "study" -> "study"
    else -> raw.trim().lowercase()
}

internal fun templateMatchesTag(entry: TemplateLibrary.Entry, tag: String): Boolean =
    tag.isBlank() || if (tag == "untagged") entry.template.tags.isEmpty()
    else entry.template.tags.any { canonicalTemplateTag(it) == tag }

/** Source-scoped tag filters, so users can jump to covers/ruled/grid/planner templates. */
@Composable
internal fun CreationTemplateTagFilters(
    entries: List<TemplateLibrary.Entry>,
    selected: String,
    visibleTags: List<String>? = null,
    onSelect: (String) -> Unit,
) {
    val available = entries.flatMap { it.template.tags }
        .map(::canonicalTemplateTag).filter { it.isNotBlank() }.distinct()
    val ordered = listOf("cover", "page", "lines", "grid", "dots", "planner", "writing", "study")
        .filter { it in available } + (available - listOf("cover", "page", "lines", "grid", "dots", "planner", "writing", "study").toSet()).sorted()
    val allTags = ordered + if (entries.any { it.template.tags.isEmpty() }) listOf("untagged") else emptyList()
    val tags = if (visibleTags == null) allTags
        else allTags.filter { it in visibleTags.map(::canonicalTemplateTag) }
    if (tags.isEmpty()) return
    val titles = mapOf(
        "cover" to stringResource(R.string.create_cover_tab),
        "page" to stringResource(R.string.create_tag_pages),
        "lines" to stringResource(R.string.create_tag_lines),
        "grid" to stringResource(R.string.create_tag_grid),
        "dots" to stringResource(R.string.create_tag_dots),
        "planner" to stringResource(R.string.create_tag_planner),
        "writing" to stringResource(R.string.create_tag_writing),
        "study" to stringResource(R.string.create_tag_study),
        "untagged" to stringResource(R.string.create_tag_untagged),
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ModeChip(stringResource(R.string.create_tag_all), selected.isBlank()) { onSelect("") }
        tags.forEach { tag ->
            ModeChip(titles[tag] ?: tag.replaceFirstChar { it.uppercase() }, selected == tag) {
                onSelect(tag)
            }
        }
    }
}

/** Only decode thumbnail-sized bitmaps; full-size photos otherwise stall image-tab composition. */
private fun decodeImageThumbnail(bytes: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}

/** Image-backed custom page/cover patterns, rendered as page-shaped thumbnails like template presets. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CustomImageStrip(
    images: List<CustomAssetFolders.CustomImage>,
    selectedBytes: ByteArray?,
    pageMm: Pair<Double, Double>,
    paper: Rgba,
    onSelect: (ByteArray) -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
    onDelete: (CustomAssetFolders.CustomImage) -> Unit,
) {
    val palette = LocalPalette.current
    var pendingDelete by remember { mutableStateOf<CustomAssetFolders.CustomImage?>(null) }
    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.create_delete_asset_confirm, CustomAssetFolders.displayName(entry.name))) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(entry)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    val aspect = (pageMm.second / pageMm.first).coerceIn(0.5, 2.0)
    val thumbH = THUMB_W * aspect.toFloat()
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (selectedBytes != null) {
            val activeImage by produceState<ImageBitmap?>(null, selectedBytes) {
                value = withContext(Dispatchers.Default) {
                    decodeImageThumbnail(selectedBytes)
                }
            }
            Column(
                Modifier.width(TILE_W).clip(MaterialTheme.shapes.extraSmall).combinedClickable(onClick = onClear).padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(THUMB_W, thumbH)
                    .border(1.dp, palette.accent.toComposeColor(), RoundedCornerShape(3.dp))
                    .clip(RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
                    if (activeImage != null) {
                        Image(activeImage!!, contentDescription = null,
                            modifier = Modifier.size(THUMB_W, thumbH).blur(5.dp),
                            contentScale = ContentScale.Crop)
                    }
                    Box(Modifier.matchParentSize().background(palette.surface.toComposeColor().copy(alpha = 0.42f)))
                    Text("×", color = palette.text.toComposeColor(), fontSize = 27.sp)
                }
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.none), color = palette.text.toComposeColor(), fontSize = 9.sp, maxLines = 1)
            }
        }
        images.forEach { entry ->
            key(entry.id) {
                val selected = selectedBytes?.contentEquals(entry.bytes) == true
                val bitmap by produceState<ImageBitmap?>(null, entry.id) {
                    value = withContext(Dispatchers.Default) {
                        decodeImageThumbnail(entry.bytes)
                    }
                }
                Column(
                    Modifier.width(TILE_W).clip(MaterialTheme.shapes.extraSmall)
                        .combinedClickable(
                            onClick = { onSelect(entry.bytes) },
                            onLongClick = { pendingDelete = entry },
                        ).padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(THUMB_W, thumbH).border(if (selected) 2.dp else 1.dp, (if (selected) palette.accent else palette.border).toComposeColor(), RoundedCornerShape(3.dp)).padding(if (selected) 2.dp else 1.dp)) {
                        if (bitmap != null) Image(bitmap!!, contentDescription = entry.name, modifier = Modifier.size(THUMB_W, thumbH), contentScale = ContentScale.Crop)
                        else Box(Modifier.size(THUMB_W, thumbH).background(paper.toComposeColor()), contentAlignment = Alignment.Center) { Text("?", color = palette.textDim.toComposeColor()) }
                    }
                }
            }
        }
        Column(
            Modifier.width(TILE_W).clip(MaterialTheme.shapes.extraSmall).combinedClickable(onClick = onImport).padding(vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(THUMB_W, thumbH).border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(3.dp)).background(palette.surface.toComposeColor(), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
                Text("+", color = palette.textDim.toComposeColor(), fontSize = 20.sp)
            }
            Spacer(Modifier.height(3.dp))
            Text(stringResource(R.string.create_import_image), color = palette.text.toComposeColor(), fontSize = 9.sp, maxLines = 1)
        }
    }
}

/** [t] with [values] set, as it lands on the whole page, [width] wide and outlined like a thumbnail. */
@Composable
internal fun TemplatePreview(
    t: Template,
    key: String,
    pageMm: Pair<Double, Double>,
    ink: Rgba,
    accent: Rgba,
    paper: Rgba,
    values: TemplateValues,
    width: Dp,
) {
    val h = width * (pageMm.second / pageMm.first).coerceIn(0.5, 2.0).toFloat()
    Box(Modifier.size(width, h).border(1.dp, LocalPalette.current.border.toComposeColor(), RoundedCornerShape(3.dp)).padding(1.dp)) {
        val bmp = rememberTemplateThumb(t, TemplateThumbs.spec(key, pageMm, ink, accent, paper, values, thumbPx(width)), keep = false)
        if (bmp != null) Image(bmp, contentDescription = t.name, Modifier.size(width, h))
    }
}

@Composable
private fun thumbPx(width: Dp): Int = with(LocalDensity.current) { width.toPx() }.toInt().coerceAtLeast(8)

/** [spec] drawn off the main thread, straight from [TemplateThumbs] when it is there already. */
@Composable
private fun rememberTemplateThumb(t: Template, spec: TemplateThumbs.Spec, keep: Boolean): ImageBitmap? {
    val state = produceState(if (keep) TemplateThumbs.cached(spec) else null, spec) {
        value = (if (keep) TemplateThumbs.cached(spec) else null) ?: withContext(Dispatchers.Default) { TemplateThumbs.render(t, spec, keep) }
    }
    return state.value
}

/** Strip thumbnails drawn ahead by [prewarm], so the Styles popup opens with them in place. */
internal object TemplateThumbs {

    data class Spec(
        val key: String,
        val pageMm: Pair<Double, Double>,
        val ink: Rgba,
        val accent: Rgba,
        val paper: Rgba,
        val numbers: Map<String, Double>,
        val colors: Map<String, Rgba>,
        val wPx: Int,
    )

    private const val CAPACITY = 96

    private val cache = object : LinkedHashMap<Spec, ImageBitmap>(CAPACITY, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Spec, ImageBitmap>) = size > CAPACITY
    }

    /** Thumbnails show the ruling plainly, whatever opacity the page itself uses. */
    fun spec(key: String, pageMm: Pair<Double, Double>, ink: Rgba, accent: Rgba, paper: Rgba, values: TemplateValues, wPx: Int) =
        Spec(key, pageMm, ink.copy(a = max(ink.a, 225)), accent.copy(a = max(accent.a, 225)), paper, values.numbers, values.colors, wPx)

    fun cached(s: Spec): ImageBitmap? = synchronized(cache) { cache[s] }

    /**
     * At picker scale most ordinary paper rulings become sub-pixel (the built-in 0.254 mm
     * stroke becomes roughly 0.2 px). Boost their *preview geometry* so lines, dots and
     * grids remain identifiable; do not change the template's real page/PDF rendering.
     */
    internal fun legiblePreview(out: TemplateOutput, scale: Double): TemplateOutput {
        if (scale <= 0.0) return out
        val minStroke = 0.95 / scale
        val minDotRadius = 0.75 / scale
        return TemplateOutput(out.layers.map { layer ->
            TemplateLayer(layer.color, layer.prims.map { primitive ->
                when (primitive) {
                    is TPrim.Stroke -> TPrim.Stroke(
                        primitive.pts, primitive.closed, max(primitive.width, minStroke),
                        primitive.dashOn, primitive.dashOff,
                    )
                    is TPrim.Ellipse -> when {
                        !primitive.fill -> TPrim.Ellipse(
                            primitive.cx, primitive.cy, primitive.rx, primitive.ry, false,
                            max(primitive.width, minStroke), primitive.dashOn, primitive.dashOff,
                        )
                        primitive.rx * scale < 0.75 && primitive.ry * scale < 0.75 -> TPrim.Ellipse(
                            primitive.cx, primitive.cy,
                            max(primitive.rx, minDotRadius), max(primitive.ry, minDotRadius),
                            true, primitive.width, primitive.dashOn, primitive.dashOff,
                        )
                        else -> primitive
                    }
                    is TPrim.Fill, is TPrim.FillRect -> primitive
                }
            })
        })
    }

    fun render(t: Template, s: Spec, keep: Boolean): ImageBitmap? {
        if (keep) cached(s)?.let { return it }
        val (w, h) = s.pageMm
        val hPx = (s.wPx * h / w).toInt().coerceAtLeast(8)
        val img = runCatching {
            val bmp = Bitmap.createBitmap(s.wPx, hPx, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(s.paper.toArgb())
            val values = TemplateValues(s.numbers, s.colors)
            val out = TemplateLibrary.layouts.layout(t, TemplatePage(w, h, Rect(0.0, 0.0, w, h), s.ink, s.accent, values))
            val scale = s.wPx / w
            TemplatePainter.paint(
                AndroidRenderer(c), legiblePreview(out, scale), scale,
                Rect(0.0, 0.0, s.wPx.toDouble(), hPx.toDouble()),
            )
            bmp.asImageBitmap()
        }.getOrNull() ?: return null
        if (keep) synchronized(cache) { cache[s] = img }
        return img
    }

    /** Draw the strip's thumbnails for each of [looks] that are not kept yet, one at a time. */
    suspend fun prewarm(entries: List<TemplateLibrary.Entry>, pageMm: Pair<Double, Double>, looks: List<TemplateLook>, wPx: Int) =
        withContext(Dispatchers.Default) {
            for (look in looks.distinct()) for (e in entries) {
                ensureActive()
                render(e.template, spec(e.key, pageMm, look.ink, look.accent, look.paper, TemplateValues.DEFAULTS, wPx), keep = true)
            }
        }
}

/** The effective colours a style level draws its template with. */
internal data class TemplateLook(val ink: Rgba, val accent: Rgba, val paper: Rgba) {
    companion object {
        /** [style]'s colours over [below] (the document's style, under a page's own). */
        fun of(style: PageStyle, below: PageStyle?, defaultPaper: Rgba) = TemplateLook(
            ink = style.patternColor ?: below?.patternColor ?: PageStyle.DEFAULT_PATTERN_COLOR,
            accent = style.accentColor ?: below?.accentColor ?: PageStyle.DEFAULT_ACCENT_COLOR,
            paper = style.pageColor ?: below?.pageColor ?: defaultPaper,
        )
    }
}

/** Keeps the Styles popup's thumbnails drawn for the open note, again after each close. */
@Composable
internal fun PrewarmTemplateThumbs(editor: Editor, popupOpen: Boolean) {
    val defaultPaper = LocalPalette.current.paper
    val wPx = thumbPx(THUMB_W)
    val version = TemplateLibraryUi.version
    LaunchedEffect(popupOpen, editor.noteOpen, editor.title, editor.pageIndex, version, defaultPaper, wPx) {
        if (popupOpen || !editor.noteOpen) return@LaunchedEffect
        delay(PREWARM_DELAY_MS)
        val doc = editor.documentStyle
        val looks = listOf(TemplateLook.of(doc, null, defaultPaper), TemplateLook.of(editor.currentPageStyle, doc, defaultPaper))
        TemplateThumbs.prewarm(editor.templateChoices(), editor.currentPageMm, looks, wPx)
    }
}

/** Lets a just-opened note draw its first pages before thumbnails take the CPU. */
private const val PREWARM_DELAY_MS = 600L
