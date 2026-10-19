package com.securitytaskmanager.data

import android.content.Context
import android.content.pm.ApplicationInfo
import com.securitytaskmanager.core.AppIndex
import com.securitytaskmanager.core.AppMeta

/** Builds the UID/package lookup the categorizer needs. Blocking: call from IO. */
object AppIndexLoader {

    @Suppress("DEPRECATION")
    fun load(context: Context): AppIndex {
        val pm = context.packageManager
        val apps = try {
            pm.getInstalledApplications(0)
        } catch (e: Exception) {
            return AppIndex.EMPTY
        }
        val byPackage = HashMap<String, AppMeta>(apps.size)
        val uidMap = HashMap<Int, MutableList<String>>()
        for (ai in apps) {
            // Updated system apps (e.g. Chrome) keep FLAG_SYSTEM, so this is "shipped with the device".
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val label = try {
                pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                ai.packageName
            }
            val hasLauncher = pm.getLaunchIntentForPackage(ai.packageName) != null
            byPackage[ai.packageName] = AppMeta(ai.packageName, label, isSystem, hasLauncher)
            uidMap.getOrPut(ai.uid) { mutableListOf() }.add(ai.packageName)
        }
        return AppIndex(uidMap, byPackage)
    }
}
