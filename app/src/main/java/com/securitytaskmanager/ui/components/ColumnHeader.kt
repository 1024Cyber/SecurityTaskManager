package com.securitytaskmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securitytaskmanager.core.SortColumn
import com.securitytaskmanager.ui.theme.PrimaryGreen
import com.securitytaskmanager.ui.theme.SurfaceVariant
import com.securitytaskmanager.ui.theme.TextSecondary

@Composable
fun ColumnHeaders(sort: SortColumn, ascending: Boolean, onSort: (SortColumn) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant)
            .padding(horizontal = ProcessColumns.HorizontalPadding, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColumnLead()
        HeaderCell("NAME", SortColumn.NAME, ProcessColumns.NAME, sort, ascending, onSort)
        HeaderCell("PID", SortColumn.PID, ProcessColumns.PID, sort, ascending, onSort)
        HeaderCell("CPU", SortColumn.CPU, ProcessColumns.CPU, sort, ascending, onSort)
        HeaderCell("MEM", SortColumn.MEMORY, ProcessColumns.MEM, sort, ascending, onSort)
        HeaderCell("USER", SortColumn.USER, ProcessColumns.USER, sort, ascending, onSort)
        HeaderCell("PORT", SortColumn.PORT, ProcessColumns.PORT, sort, ascending, onSort)
        HeaderCell("TIME", SortColumn.TIME, ProcessColumns.TIME, sort, ascending, onSort)
    }
}

@Composable
private fun RowScope.HeaderCell(
    label: String,
    column: SortColumn,
    weight: Float,
    sort: SortColumn,
    ascending: Boolean,
    onSort: (SortColumn) -> Unit
) {
    val active = sort == column
    // The arrow is part of the text (not an Icon), so sorting never changes a cell's layout.
    val arrow = if (active) (if (ascending) "▲" else "▼") else ""
    ColumnCell(weight, Modifier.clickable { onSort(column) }) {
        Text(
            text = label + arrow,
            color = if (active) PrimaryGreen else TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
