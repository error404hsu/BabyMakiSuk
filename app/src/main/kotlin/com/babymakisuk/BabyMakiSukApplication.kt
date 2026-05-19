package com.babymakisuk

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.babymakisuk.corefirebase.auth.FirebaseAuthRepository
import com.babymakisuk.coredata.worker.DataRetentionWorker
import com.babymakisuk.coredata.worker.MemoReminderWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BabyMakiSukApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var authRepository: FirebaseAuthRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        MemoReminderWorker.createChannel(this)
        DataRetentionWorker.schedule(this)
        applicationScope.launch {
            authRepository.signInAnonymously()
        }
    }
}
