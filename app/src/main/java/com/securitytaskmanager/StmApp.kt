package com.securitytaskmanager

import android.app.Application
import com.topjohnwu.superuser.Shell

class StmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // su if granted, otherwise a plain shell (limited mode). 20 s covers a slow first su prompt.
        Shell.setDefaultBuilder(Shell.Builder.create().setTimeout(20))
    }
}
