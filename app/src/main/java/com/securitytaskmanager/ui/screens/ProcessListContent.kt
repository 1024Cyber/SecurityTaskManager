package com.securitytaskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securitytaskmanager.core.ProcessInfo
import com.securitytaskmanager.core.SortColumn
import com.securitytaskmanager.ui.components.ColumnHeaders
import com.securitytaskmanager.ui.components.ProcessRow
import com.securitytaskmanager.ui.theme.BackgroundDark
import com.securitytaskmanager.ui.theme.PrimaryGreen
import com.securitytaskmanager.ui.theme.SurfaceVariant
import com.securitytaskmanager.ui.theme.TextPrimary
import com.securitytaskmanager.ui.theme.TextSecondary

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProcessListContent(
    rows: List<ProcessInfo>,
    loading: Boolean,
    refreshing: Boolean,
    lockedForNonRoot: Boolean,
    sort: SortColumn,
    ascending: Boolean,
    emptyHint: String,
    onSort: (SortColumn) -> Unit,
    onRefresh: () -> Unit,
    onSelect: (ProcessInfo) -> Unit
) {
    val pull = rememberPullRefreshState(refreshing = refreshing, onRefresh = onRefresh)

    Box(Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(Modifier.fillMaxSize()) {
            ColumnHeaders(sort = sort, ascending = ascending, onSort = onSort)

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(32.dp))
                }
            } else {
                // Always a scrollable LazyColumn (even when empty) so the pull gesture has
                // a scroll container to attach to.
                Box(Modifier.fillMaxSize().pullRefresh(pull)) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (rows.isEmpty()) {
                            item {
                                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        emptyHint, color = TextSecondary, fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }
                            }
                        }
                        items(rows, key = { it.pid }) { p ->
                            ProcessRow(process = p, onClick = { onSelect(p) })
                            HorizontalDivider(color = SurfaceVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                        }
                    }
                    PullRefreshIndicator(
                        refreshing = refreshing, state = pull,
                        modifier = Modifier.align(Alignment.TopCenter),
                        backgroundColor = SurfaceVariant, contentColor = PrimaryGreen
                    )
                }
            }
        }

        if (lockedForNonRoot) {
            Box(
                Modifier.fillMaxSize().background(BackgroundDark.copy(alpha = 0.94f)).clickable(enabled = true) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("🔒", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Root access required", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Core system processes are only visible on rooted devices.",
                        color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
