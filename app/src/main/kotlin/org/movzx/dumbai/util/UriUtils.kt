package org.movzx.dumbai.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

fun resolveUriToPath(context: Context, uri: Uri): String? {
    if ("com.android.externalstorage.documents" == uri.authority) {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]

        if ("primary".equals(type, ignoreCase = true))
            return "${Environment.getExternalStorageDirectory()}/${split[1]}"
        else return "/storage/$type/${split.getOrNull(1) ?: ""}"
    }

    return null
}
