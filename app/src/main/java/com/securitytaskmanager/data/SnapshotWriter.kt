package com.securitytaskmanager.data

import android.content.Context
import android.net.Uri
import com.securitytaskmanager.core.ExportFormat
import com.securitytaskmanager.core.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

enum class ExportKind(val mime: String, val ext: String) {
    JSON("application/json", "json"),
    CSV("text/csv", "csv")
}

object SnapshotWriter {

    /** Writes to a Uri chosen with the system file picker (no storage permission needed). */
    suspend fun write(context: Context, uri: Uri, kind: ExportKind, snapshot: Snapshot, appVersion: String) =
        withContext(Dispatchers.IO) {
            val text = when (kind) {
                ExportKind.JSON -> ExportFormat.toJson(snapshot, appVersion)
                ExportKind.CSV -> ExportFormat.toCsv(snapshot)
            }
            val out = context.contentResolver.openOutputStream(uri, "wt")
                ?: throw IOException("Could not open the selected file for writing")
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }

    fun suggestedName(kind: ExportKind): String {
        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        return "stm_snapshot_$ts.${kind.ext}"
    }
}
