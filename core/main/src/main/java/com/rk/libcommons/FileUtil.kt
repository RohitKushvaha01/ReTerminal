package com.rk.libcommons

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

fun Context.localDir(): File {
    return File(filesDir.parentFile, "local").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

fun Context.alpineDir(): File {
    return localDir().child("alpine").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

fun Context.alpineHomeDir(): File {
    return alpineDir().child("root").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

fun Context.localBinDir(): File {
    return localDir().child("bin").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

fun Context.localLibDir(): File {
    return localDir().child("lib").also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
}

fun File.child(fileName: String): File {
    return File(this, fileName)
}

fun File.createFileIfNot(): File {
    if (exists().not()) {
        createNewFile()
    }
    return this
}

fun Context.getFileNameFromUri(uri: Uri): String? {
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                return cursor.getString(nameIndex)
            }
        }
    } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
        return File(uri.path!!).name
    }
    return null
}
