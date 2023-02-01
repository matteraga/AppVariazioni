package me.matteo.appvariazioni.classes.handlers

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import me.matteo.appvariazioni.classes.Response
import me.matteo.appvariazioni.classes.Utils
import java.io.OutputStream
import java.util.regex.Pattern

class FileHandler {
    suspend fun savePDF(context: Context, bytes: ByteArray, fileName: String): Response {
        try {
            val contentResolver = context.contentResolver
            if (contentResolver.persistedUriPermissions.size == 0 || contentResolver.persistedUriPermissions[0].uri == null) {
                return Response(false)
            }

            val directory =
                DocumentFile.fromTreeUri(context, contentResolver.persistedUriPermissions[0].uri)
                    ?: return Response(false)
            val newFile: DocumentFile = directory.createFile("application/pdf", fileName)
                ?: return Response(false)
            val uri = newFile.uri
            val out: OutputStream = contentResolver.openOutputStream(uri)
                ?: return Response(false)
            out.write(bytes)
            out.close()
            return Response(true, uri)
        } catch (thrown: Throwable) {
            return Response(false)
        }
    }
}