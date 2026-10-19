package com.securitytaskmanager.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for the process table's geometry.
 *
 * The header and every row build their columns ONLY through [ColumnLead] and [ColumnCell]
 * using these values, so they cannot drift apart: change a weight here and header and rows
 * change together.
 */
object ProcessColumns {
    val HorizontalPadding: Dp = 12.dp
    val LeadWidth: Dp = 17.dp          // status-dot column

    const val NAME = 2.6f
    const val PID = 1f
    const val CPU = 1.1f
    const val MEM = 1.1f
    const val USER = 1.5f
    const val PORT = 1.1f
    const val TIME = 1f
}

/** Fixed-width lead column: the status dot in rows, empty in the header. */
@Composable
fun RowScope.ColumnLead(content: @Composable () -> Unit = {}) {
    Box(modifier = Modifier.width(ProcessColumns.LeadWidth), contentAlignment = Alignment.CenterStart) {
        content()
    }
}

/** One table cell: a weighted Box with start-aligned content, identical for header and rows. */
@Composable
fun RowScope.ColumnCell(
    weight: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.weight(weight), contentAlignment = Alignment.CenterStart) {
        content()
    }
}
