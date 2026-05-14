package com.babymakisuk

import android.app.Application
import com.babymakisuk.coredata.worker.MemoReminderWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BabyMakiSukApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MemoReminderWorker.createChannel(this)
    }
}
