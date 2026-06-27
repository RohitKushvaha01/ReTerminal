package com.rk

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import com.rk.libcommons.alpineHomeDir
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import com.rk.terminal.R

class AlpineDocumentProvider : DocumentsProvider() {

    private val baseDir: File get() = context!!.alpineHomeDir()

    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val applicationName = "ReTerminal"

        val row = result.newRow()
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, getDocIdForFile(baseDir))
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocIdForFile(baseDir))
        row.add(DocumentsContract.Root.COLUMN_SUMMARY, null)
        row.add(
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.FLAG_SUPPORTS_CREATE or DocumentsContract.Root.FLAG_SUPPORTS_SEARCH or DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD
        )
        row.add(DocumentsContract.Root.COLUMN_TITLE, applicationName)
        row.add(DocumentsContract.Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES)
        row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, baseDir.freeSpace)
        row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        includeFile(result, documentId, null)
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(parentDocumentId)
        val files = parent.listFiles()
        if (files != null) {
            for (file in files) {
                includeFile(result, null, file)
            }
        } else {
            Log.e("DocumentsProvider", "Unable to list files in $parentDocumentId")
        }
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = getFileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    @Throws(FileNotFoundException::class)
    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point,
        signal: CancellationSignal
    ): AssetFileDescriptor {
        val file = getFileForDocId(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun onCreate(): Boolean = true

    @Throws(FileNotFoundException::class)
    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        val parent = getFileForDocId(parentDocumentId)
        var newFile = File(parent, displayName)
        var noConflictId = 2
        while (newFile.exists()) {
            newFile = File(parent, "$displayName ($noConflictId)")
            noConflictId++
        }
        try {
            val succeeded = if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                newFile.mkdir()
            } else {
                newFile.createNewFile()
            }
            if (!succeeded) {
                throw FileNotFoundException("Failed to create document with id " + newFile.absolutePath)
            }
        } catch (e: IOException) {
            throw FileNotFoundException("Failed to create document with id " + newFile.absolutePath)
        }
        return getDocIdForFile(newFile)
    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        val file = getFileForDocId(documentId)
        if (!file.delete()) {
            throw FileNotFoundException("Failed to delete document with id $documentId")
        }
    }

    @Throws(FileNotFoundException::class)
    override fun getDocumentType(documentId: String): String {
        val file = getFileForDocId(documentId)
        return getMimeType(file)
    }

    @Throws(FileNotFoundException::class)
    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<String>?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(rootId)
        val pending = LinkedList<File>()
        pending.add(parent)

        val MAX_SEARCH_RESULTS = 50
        while (!pending.isEmpty() && result.count < MAX_SEARCH_RESULTS) {
            val file = pending.removeFirst()
            val isInsideHome: Boolean = try {
                file.canonicalPath.startsWith(baseDir.canonicalPath)
            } catch (e: IOException) {
                true
            }
            if (isInsideHome) {
                if (file.isDirectory) {
                    file.listFiles()?.let { Collections.addAll(pending, *it) }
                } else {
                    if (file.name.lowercase(Locale.getDefault()).contains(query)) {
                        includeFile(result, null, file)
                    }
                }
            }
        }
        return result
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    @Throws(FileNotFoundException::class)
    private fun includeFile(result: MatrixCursor, docId: String?, file: File?) {
        val finalDocId = docId ?: getDocIdForFile(file!!)
        val finalFile = file ?: getFileForDocId(finalDocId)

        var flags = 0
        if (finalFile.isDirectory) {
            if (finalFile.canWrite()) flags = flags or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
        } else if (finalFile.canWrite()) {
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_WRITE
        }
        if (finalFile.parentFile?.canWrite() == true) flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_DELETE

        val displayName = finalFile.name
        val mimeType = getMimeType(finalFile)
        if (mimeType.startsWith("image/")) flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL

        val row = result.newRow()
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, finalDocId)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(DocumentsContract.Document.COLUMN_SIZE, finalFile.length())
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, finalFile.lastModified())
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags)
        row.add(DocumentsContract.Document.COLUMN_ICON, R.mipmap.ic_launcher)
    }

    companion object {
        fun isDocumentProviderEnabled(context: Context): Boolean {
            val componentName = ComponentName(context, AlpineDocumentProvider::class.java)
            val state = context.packageManager.getComponentEnabledSetting(componentName)
            return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }

        fun setDocumentProviderEnabled(context: Context, enabled: Boolean) {
            if (isDocumentProviderEnabled(context) == enabled) return
            val componentName = ComponentName(context, AlpineDocumentProvider::class.java)
            val newState = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            context.packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
        }

        private const val ALL_MIME_TYPES = "*/*"
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
        )
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
        )

        private fun getDocIdForFile(file: File): String = file.absolutePath

        @Throws(FileNotFoundException::class)
        private fun getFileForDocId(docId: String): File {
            val f = File(docId)
            if (!f.exists()) throw FileNotFoundException(f.absolutePath + " not found")
            return f
        }

        private fun getMimeType(file: File): String {
            if (file.isDirectory) return DocumentsContract.Document.MIME_TYPE_DIR
            val name = file.name
            val lastDot = name.lastIndexOf('.')
            if (lastDot >= 0) {
                val extension = name.substring(lastDot + 1).lowercase(Locale.getDefault())
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (mime != null) return mime
            }
            return "application/octet-stream"
        }
    }
}
