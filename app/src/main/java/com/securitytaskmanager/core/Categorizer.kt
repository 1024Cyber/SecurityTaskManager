package com.securitytaskmanager.core

data class AppMeta(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val hasLauncher: Boolean
)

/** What the app knows about installed packages (built from PackageManager on-device). */
class AppIndex(
    val uidToPackages: Map<Int, List<String>>,
    val byPackage: Map<String, AppMeta>
) {
    companion object {
        val EMPTY = AppIndex(emptyMap(), emptyMap())
    }
}

object Categorizer {

    val CRITICAL_NAMES = setOf(
        "init", "zygote", "zygote64", "system_server", "surfaceflinger", "servicemanager",
        "hwservicemanager", "vndservicemanager", "vold", "netd", "installd", "healthd",
        "lmkd", "logd", "adbd", "keystore", "keystore2", "tombstoned", "ueventd", "kthreadd"
    )

    /** Preinstalled/vendor package prefixes that are kept OUT of the APPS tab. Edit freely. */
    val OEM_PREFIXES = listOf(
        "android.", "com.android.", "com.samsung.", "com.sec.", "com.qualcomm.", "com.qti.",
        "com.mediatek.", "com.miui.", "com.xiaomi.", "com.huawei.", "com.oneplus.", "com.oppo.",
        "com.coloros.", "com.vivo.", "com.lge.", "com.motorola.", "com.sonyericsson.",
        "com.google.android.gms", "com.google.android.gsf", "com.google.android.ext.",
        "com.google.android.permissioncontroller", "com.google.android.providers.",
        "com.google.android.networkstack", "com.google.android.cellbroadcast",
        "com.google.android.overlay", "com.google.android.packageinstaller",
        "com.google.android.apps.nexuslauncher", "com.google.mainline."
    )

    /** System apps users do think of as apps even though they match an OEM prefix. */
    val APP_ALLOWLIST = setOf("com.android.chrome", "com.android.vending")

    fun baseName(rawName: String): String =
        if (rawName.startsWith("/")) rawName.substringAfterLast('/') else rawName

    /** Package for a process: "com.foo:remote" -> com.foo, else the UID's package (shared UIDs: first). */
    fun packageFor(processName: String, uid: Int, index: AppIndex): String? {
        val candidate = processName.substringBefore(':')
        if (index.byPackage.containsKey(candidate)) return candidate
        if (uid >= 10000) return index.uidToPackages[uid]?.firstOrNull()
        return null
    }

    fun isKernelThread(pid: Int, ppid: Int, name: String): Boolean =
        pid == 2 || ppid == 2 || (name.startsWith("[") && name.endsWith("]"))

    fun isCritical(pid: Int, name: String, kernel: Boolean): Boolean =
        pid == 1 || kernel || CRITICAL_NAMES.contains(baseName(name).lowercase())

    fun isAppPackage(meta: AppMeta): Boolean {
        if (!meta.isSystem) return true                       // user-installed
        if (APP_ALLOWLIST.contains(meta.packageName)) return true
        if (!meta.hasLauncher) return false
        return OEM_PREFIXES.none { meta.packageName.startsWith(it) }
    }

    fun categorize(
        uid: Int, name: String, pkg: String?, kernel: Boolean, critical: Boolean, index: AppIndex
    ): ProcessCategory {
        if (kernel || critical || (uid in 0..999)) return ProcessCategory.CORE
        if (uid >= 10000 && pkg != null) {
            val meta = index.byPackage[pkg]
            if (meta != null && isAppPackage(meta)) return ProcessCategory.APP
        }
        return ProcessCategory.BACKGROUND
    }

    /** Android user names, matching what `ps` prints (u0_a123, system, ...). Fallback only. */
    fun userName(uid: Int): String = when {
        uid == 0 -> "root"
        uid == 1000 -> "system"
        uid == 1001 -> "radio"
        uid == 1002 -> "bluetooth"
        uid == 1010 -> "wifi"
        uid == 1013 -> "media"
        uid == 1017 -> "keystore"
        uid == 2000 -> "shell"
        uid == 9999 -> "nobody"
        uid >= 10000 -> "u${uid / 100000}_a${(uid % 100000) - 10000}"
        else -> uid.toString()
    }
}
