package com.xnotes.platform

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap

/** User-owned image libraries kept in hidden folders beside the selected notes root. */
object CustomAssetFolders {
    enum class Kind(val folder: String) { PAGE("page-patterns"), COVER("cover-patterns") }
    data class CustomImage(val id: String, val name: String, val bytes: ByteArray)

    private const val ROOT_DIR = ".xnotes-pages"

    /** Save an image to the page- or cover-pattern library. The new note embeds its own copy too. */
    fun storeImage(context: Context, treeUri: String?, bytes: ByteArray, sourceName: String?, kind: Kind): Boolean {
        if (treeUri.isNullOrBlank() || bytes.isEmpty()) return false
        return runCatching {
            val resolver = context.applicationContext.contentResolver
            val tree = Uri.parse(treeUri)
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
            val pagesId = ensureDirectory(context, tree, rootId, root, ROOT_DIR) ?: return false
            val pagesUri = DocumentsContract.buildDocumentUriUsingTree(tree, pagesId)
            val kindId = ensureDirectory(context, tree, pagesId, pagesUri, kind.folder) ?: return false
            val kindUri = DocumentsContract.buildDocumentUriUsingTree(tree, kindId)
            val rawName = sourceName?.substringAfterLast('/')?.substringAfterLast('\\')?.trim().orEmpty()
            val ext = rawName.substringAfterLast('.', "").lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
            val stem = rawName.substringBeforeLast('.', rawName).take(80).ifBlank { if (kind == Kind.COVER) "custom-cover" else "custom-page" }
            val name = "$stem-${System.currentTimeMillis()}.${ext ?: "img"}"
            val mime = ext?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) } ?: "application/octet-stream"
            val target = DocumentsContract.createDocument(resolver, kindUri, mime, name) ?: return false
            resolver.openOutputStream(target, "wt")?.use { it.write(bytes) } ?: return false
            true
        }.getOrDefault(false)
    }

    /** Read the selected kind's image library from the granted notes folder. */
    fun loadImages(context: Context, treeUri: String?, kind: Kind): List<CustomImage> {
        if (treeUri.isNullOrBlank()) return emptyList()
        return runCatching {
            val resolver = context.applicationContext.contentResolver
            val tree = Uri.parse(treeUri)
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val pagesId = childId(context, tree, rootId, ROOT_DIR) ?: return emptyList()
            val kindId = childId(context, tree, pagesId, kind.folder) ?: return emptyList()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, kindId)
            val refs = ArrayList<Pair<String, String>>()
            resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0) ?: continue
                    val id = c.getString(1) ?: continue
                    val mime = c.getString(2).orEmpty()
                    if (mime.startsWith("image/") || name.substringAfterLast('.', "").lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "avif", "img", "heic", "heif")) refs += name to id
                }
            }
            refs.mapNotNull { (name, id) ->
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: return@mapNotNull null
                if (bytes.isEmpty()) null else CustomImage(id, name, bytes)
            }
        }.getOrDefault(emptyList())
    }

    private fun ensureDirectory(context: Context, tree: Uri, parentId: String, parentUri: Uri, name: String): String? {
        childId(context, tree, parentId, name)?.let { return it }
        val created = DocumentsContract.createDocument(context.contentResolver, parentUri, DocumentsContract.Document.MIME_TYPE_DIR, name) ?: return null
        return DocumentsContract.getDocumentId(created)
    }

    private fun childId(context: Context, tree: Uri, parentId: String, name: String): String? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        return context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { c ->
            while (c.moveToNext()) if (c.getString(0) == name) return c.getString(1)
            null
        }
    }
}
