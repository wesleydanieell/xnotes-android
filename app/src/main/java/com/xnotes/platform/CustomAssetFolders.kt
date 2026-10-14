package com.xnotes.platform

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap

/** Hidden, user-owned libraries beside the selected notes root. Dot-folders are already omitted by the explorer. */
object CustomAssetFolders {
    private const val PAGES_DIR = ".xnotes-pages"

    /** Save a picked page/cover image in the reusable image library; the note also embeds its own copy. */
    fun storeImage(context: Context, treeUri: String?, bytes: ByteArray, sourceName: String?) {
        if (treeUri.isNullOrBlank() || bytes.isEmpty()) return
        runCatching {
            val resolver = context.applicationContext.contentResolver
            val tree = Uri.parse(treeUri)
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
            val rootChildren = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
            var folderId: String? = null
            resolver.query(rootChildren, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    if (c.getString(0) == PAGES_DIR && c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        folderId = c.getString(1)
                        break
                    }
                }
            }
            if (folderId == null) {
                val folder = DocumentsContract.createDocument(resolver, root, DocumentsContract.Document.MIME_TYPE_DIR, PAGES_DIR) ?: return
                folderId = DocumentsContract.getDocumentId(folder)
            }
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(tree, folderId!!)
            val rawName = sourceName?.substringAfterLast('/')?.substringAfterLast('\\')?.trim().orEmpty()
            val ext = rawName.substringAfterLast('.', "").lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
            val name = if (rawName.isNotBlank() && ext != null) {
                "${rawName.substringBeforeLast('.').take(100)}-${System.currentTimeMillis()}.$ext"
            } else "custom-page-${System.currentTimeMillis()}.img"
            val mime = ext?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) } ?: "application/octet-stream"
            val target = DocumentsContract.createDocument(resolver, folderUri, mime, name) ?: return
            resolver.openOutputStream(target, "wt")?.use { it.write(bytes) }
        }
    }
}
