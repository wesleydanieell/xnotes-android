package com.xnotes.platform

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import android.provider.DocumentsContract
import android.content.res.AssetManager
import com.xnotes.core.geometry.Rect
import com.xnotes.core.model.Document
import com.xnotes.core.model.Page
import com.xnotes.core.model.PageTemplates
import com.xnotes.core.model.marginStrips
import com.xnotes.core.model.resolvedAccentColor
import com.xnotes.core.model.resolvedColors
import com.xnotes.core.model.resolvedParams
import com.xnotes.core.model.resolvedPatternColor
import com.xnotes.core.model.resolvedSpacing
import com.xnotes.core.model.resolvedTemplate
import com.xnotes.core.pal.Renderer
import com.xnotes.core.template.Template
import com.xnotes.core.template.TemplateEval
import com.xnotes.core.template.TemplateLayoutCache
import com.xnotes.core.template.TemplatePage
import com.xnotes.core.template.TemplatePainter
import com.xnotes.core.template.TemplateValues
import com.xnotes.format.TemplateFormatException
import com.xnotes.format.TemplateReader
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * The page templates the app knows: the three built-in rulings, the bundled examples, and the
 * user's imports (`filesDir/templates`). Built-ins go by their ruling id (`lines`); every other
 * template goes by a key hashed from its text, so the copy a note carries in its bundle and the one
 * in the library are the same template exactly when their bytes are. Also paints a page's template,
 * shared by the canvas caches, thumbnails and PDF export.
 */
object TemplateLibrary {

    /** Where a template comes from; [NOTE] is one a note carries that the library does not have. */
    enum class Source { BUILT_IN, BUNDLED, IMPORTED, NOTE }

    class Entry(val key: String, val template: Template, val text: String, val source: Source)

    private const val ASSET_DIR = "templates"
    private const val EXAMPLES_DIR = "templates/examples"
    private const val EXT = ".xtemplate"

    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var importDir: File? = null
    @Volatile
    private var storageTree: Uri? = null
    @Volatile
    private var storageDirId: String? = null
    private const val STORAGE_DIR = ".xnotes-templates"
    @Volatile
    private var bundled: List<Entry> = emptyList()
    private val imported = ConcurrentHashMap<String, Entry>()
    private val parsed = ConcurrentHashMap<String, Template>()

    private val eval = TemplateEval(AndroidGlyphOutliner())
    val layouts = TemplateLayoutCache(eval)

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        importDir = File(app.filesDir, "templates")
        bundled = loadBundled(app.assets)
        reloadImported()
    }

    /** Every template to offer, built-ins first, then the bundled examples, then imports. */
    fun all(): List<Entry> = bundled + imported.values.sortedBy { it.template.name.lowercase() }

    /** Store imported templates in a hidden folder beside the selected notes root as well as privately. */
    fun configureStorageTree(treeUri: String?) {
        storageTree = treeUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        storageDirId = ensureStorageDir()
        reloadImported()
    }

    fun entry(key: String): Entry? = bundled.firstOrNull { it.key == key } ?: imported[key]

    /** The key of a template that is not built in: a hash of its text. */
    fun keyOf(text: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return d.take(8).joinToString("") { "%02x".format(it) }
    }

    /**
     * The template [key] names for [doc]: the note's own embedded copy first, so a note keeps
     * drawing what it was drawn with, then the library.
     */
    fun template(doc: Document, key: String): Template? {
        if (key == PageTemplates.NONE) return null
        if (!PageTemplates.isBuiltIn(key)) doc.templates[key]?.let { return parse(key, it) }
        return entry(key)?.template
    }

    /** [doc] with [key]'s text embedded, when it is a library template the note does not carry yet. */
    fun embed(doc: Document, key: String?) {
        if (key == null || key == PageTemplates.NONE || PageTemplates.isBuiltIn(key) || doc.templates.containsKey(key)) return
        val text = entry(key)?.text ?: return
        doc.templates = doc.templates + (key to text)
    }

    /** Adopt [text] as a user template; throws [TemplateFormatException] when it is not one. */
    fun import(text: String): Entry {
        var importText = text
        var t = TemplateReader.read(importText)
        var key = keyOf(importText)
        val existing = entry(key)
        if (existing != null) {
            if (existing.source == Source.IMPORTED) return existing
            // Let users explicitly import a bundled example into their custom collection too.
            // Preserve its design and name, but add ignored metadata so the custom copy has its
            // own stable content key and does not get shadowed by the bundled entry.
            importText = JSONObject(text).put("xnotesImportedCopy", java.util.UUID.randomUUID().toString()).toString(2)
            t = TemplateReader.read(importText)
            key = keyOf(importText)
        }
        val dir = importDir ?: throw TemplateFormatException("template storage is unavailable")
        dir.mkdirs()
        File(dir, key + EXT).writeText(importText, Charsets.UTF_8)
        writeExternal(key + EXT, importText.toByteArray(Charsets.UTF_8))
        return Entry(key, t, importText, Source.IMPORTED).also { imported[key] = it; parsed[key] = t }
    }

    fun remove(key: String) {
        imported.remove(key) ?: return
        importDir?.let { File(it, key + EXT).delete() }
        deleteExternal(key + EXT)
    }

    private fun parse(key: String, text: String): Template? =
        parsed[key] ?: runCatching { TemplateReader.read(text) }.getOrNull()?.also { parsed[key] = it }

    private fun loadBundled(am: AssetManager): List<Entry> {
        fun read(path: String) = runCatching { am.open(path).use { String(it.readBytes(), Charsets.UTF_8) } }.getOrNull()
        val out = ArrayList<Entry>()
        for (id in listOf("lines", "dots", "grid")) {
            val text = read("$ASSET_DIR/$id$EXT") ?: continue
            val t = runCatching { TemplateReader.read(text) }.getOrNull() ?: continue
            out += Entry(id, t, text, Source.BUILT_IN)
        }
        val examples = (runCatching { am.list(EXAMPLES_DIR) }.getOrNull() ?: emptyArray()).filter { it.endsWith(EXT) }
        out += examples.mapNotNull { name ->
            val text = read("$EXAMPLES_DIR/$name") ?: return@mapNotNull null
            val t = runCatching { TemplateReader.read(text) }.getOrNull() ?: return@mapNotNull null
            Entry(keyOf(text), t, text, Source.BUNDLED)
        }.sortedBy { it.template.name.lowercase() }
        return out
    }

    private fun reloadImported() {
        imported.clear()
        importDir?.listFiles()?.forEach { f ->
            if (!f.isFile || !f.name.endsWith(EXT)) return@forEach
            val text = runCatching { f.readText(Charsets.UTF_8) }.getOrNull() ?: return@forEach
            val t = runCatching { TemplateReader.read(text) }.getOrNull() ?: return@forEach
            val key = keyOf(text)
            imported[key] = Entry(key, t, text, Source.IMPORTED)
        }
        val tree = storageTree ?: return
        val dirId = storageDirId ?: return
        externalChildren(tree, dirId).forEach { (name, id) ->
            if (!name.endsWith(EXT)) return@forEach
            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
            val text = runCatching {
                appContext?.contentResolver?.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }.getOrNull() ?: return@forEach
            val t = runCatching { TemplateReader.read(text) }.getOrNull() ?: return@forEach
            val key = keyOf(text)
            imported[key] = Entry(key, t, text, Source.IMPORTED)
        }
    }

    private fun ensureStorageDir(): String? = runCatching {
        val context = appContext ?: return null
        val tree = storageTree ?: return null
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        externalChildren(tree, rootId).firstOrNull { it.first == STORAGE_DIR }?.second?.let { return it }
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        val created = DocumentsContract.createDocument(
            context.contentResolver, root, DocumentsContract.Document.MIME_TYPE_DIR, STORAGE_DIR,
        ) ?: return null
        DocumentsContract.getDocumentId(created)
    }.getOrNull()

    private fun externalChildren(tree: Uri, parentId: String): List<Pair<String, String>> = runCatching {
        val context = appContext ?: return emptyList()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val out = ArrayList<Pair<String, String>>()
        context.contentResolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0) ?: continue
                val id = c.getString(1) ?: continue
                out += name to id
            }
        }
        out
    }.getOrDefault(emptyList())

    private fun writeExternal(name: String, bytes: ByteArray) {
        runCatching {
            val context = appContext ?: return
            val tree = storageTree ?: return
            val dirId = storageDirId ?: return
            val existing = externalChildren(tree, dirId).firstOrNull { it.first == name }?.second
            val target = existing?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it) }
                ?: DocumentsContract.createDocument(
                    context.contentResolver,
                    DocumentsContract.buildDocumentUriUsingTree(tree, dirId),
                    "application/json", name,
                ) ?: return
            context.contentResolver.openOutputStream(target, "wt")?.use { it.write(bytes) }
        }
    }

    private fun deleteExternal(name: String) {
        runCatching {
            val context = appContext ?: return
            val tree = storageTree ?: return
            val dirId = storageDirId ?: return
            val id = externalChildren(tree, dirId).firstOrNull { it.first == name }?.second ?: return
            DocumentsContract.deleteDocument(context.contentResolver, DocumentsContract.buildDocumentUriUsingTree(tree, id))
        }
    }

    // --- painting ---

    /** [page]'s parameter values from its style chain, lengths converted to template mm. */
    fun values(t: Template, doc: Document, page: Page): TemplateValues {
        val numbers = HashMap(page.resolvedParams(doc))
        val spacing = t.spacingParam
        page.resolvedSpacing(doc)?.let { px -> if (spacing != null) numbers[spacing.name] = px * 25.4 / doc.dpi }
        return TemplateValues(numbers, page.resolvedColors(doc))
    }

    /**
     * Paint [page]'s template into [r] in page-local content px. [cover] is the page's whole paper,
     * margins included; [region] is the part being painted. A page showing an imported PDF gets its
     * template in the margins only, so the PDF itself is never drawn over.
     */
    fun paint(r: Renderer, doc: Document, page: Page, cover: Rect, region: Rect) {
        val key = page.resolvedTemplate(doc)
        val t = template(doc, key) ?: return
        val mm = doc.dpi / 25.4
        val content = Rect(0.0, 0.0, page.width, page.height)
        val papers = if (page.pdfPage == null) listOf(cover) else marginStrips(cover, content)
        val values = values(t, doc, page)
        val ink = page.resolvedPatternColor(doc)
        val accent = page.resolvedAccentColor(doc)
        for (paper in papers) {
            if (!paper.intersects(region)) continue
            val tp = TemplatePage(
                page.width / mm, page.height / mm,
                Rect(paper.x / mm, paper.y / mm, paper.w / mm, paper.h / mm),
                ink, accent, values,
            )
            TemplatePainter.paint(r, layouts.layout(t, tp), mm, region)
        }
    }
}
