package com.securitytaskmanager.core

/** Pure search / filter / sort for the process table. */
object ListFilter {

    fun apply(
        all: List<ProcessInfo>,
        category: ProcessCategory,
        query: String,
        sort: SortColumn,
        ascending: Boolean,
        hideKernelThreads: Boolean
    ): List<ProcessInfo> {
        val q = query.trim().lowercase()
        val filtered = all.asSequence()
            .filter { it.category == category }
            .filter { !(hideKernelThreads && it.isKernelThread) }
            .filter { p ->
                q.isEmpty() ||
                    p.name.lowercase().contains(q) ||
                    p.packageName?.lowercase()?.contains(q) == true ||
                    p.appLabel?.lowercase()?.contains(q) == true ||
                    p.user.lowercase().contains(q) ||
                    p.pid.toString() == q ||
                    p.ports.any { it.toString().contains(q) }
            }
            .toList()
        val base: Comparator<ProcessInfo> = when (sort) {
            SortColumn.NAME -> compareBy { it.name.lowercase() }
            SortColumn.PID -> compareBy { it.pid }
            SortColumn.CPU -> compareBy { it.cpuPercent }
            SortColumn.MEMORY -> compareBy { it.rssKb }
            SortColumn.USER -> compareBy { it.user.lowercase() }
            SortColumn.PORT -> compareBy { it.ports.firstOrNull() ?: Int.MAX_VALUE }
            SortColumn.TIME -> compareBy { it.startTimeMs }
        }
        // PID as a stable tie-breaker so rows don't shuffle between refreshes
        val cmp = (if (ascending) base else base.reversed()).thenBy { it.pid }
        return filtered.sortedWith(cmp)
    }

    fun count(all: List<ProcessInfo>, category: ProcessCategory, hideKernelThreads: Boolean): Int =
        all.count { it.category == category && !(hideKernelThreads && it.isKernelThread) }
}
