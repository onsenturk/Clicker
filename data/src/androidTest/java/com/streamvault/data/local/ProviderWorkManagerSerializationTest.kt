package com.streamvault.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.streamvault.data.sync.BackgroundEpgSyncWorker
import com.streamvault.data.sync.ProviderSyncWorker
import com.streamvault.data.sync.StalkerIndexWorker
import com.streamvault.data.sync.XtreamIndexWorker
import com.streamvault.data.sync.providerWorkUniqueName
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Exercises the actual worker enqueue APIs against WorkManager's unique-work database. */
@RunWith(AndroidJUnit4::class)
class ProviderWorkManagerSerializationTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setTaskExecutor(SynchronousExecutor())
                .build()
        )
        workManager = WorkManager.getInstance(context)
        workManager.cancelAllWork().result.get(10, TimeUnit.SECONDS)
    }

    @After
    fun tearDown() {
        workManager.cancelAllWork().result.get(10, TimeUnit.SECONDS)
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun everyBackgroundPhaseForAProviderIsAppendedToOneSerialChain() {
        val providerId = 71L
        ProviderSyncWorker.enqueueProvider(context, providerId)
        XtreamIndexWorker.enqueue(context, providerId, initialDelaySeconds = ONE_HOUR_SECONDS)
        StalkerIndexWorker.enqueue(
            context,
            providerId,
            initialDelaySeconds = ONE_HOUR_SECONDS,
            appendSuccessor = true
        )
        BackgroundEpgSyncWorker.enqueue(context, providerId, initialDelaySeconds = ONE_HOUR_SECONDS)

        val work = workManager.getWorkInfosForUniqueWork(providerWorkUniqueName(providerId))
            .get(10, TimeUnit.SECONDS)

        assertThat(work).hasSize(4)
        assertThat(work.count { it.state == WorkInfo.State.ENQUEUED }).isEqualTo(1)
        assertThat(work.count { it.state == WorkInfo.State.BLOCKED }).isEqualTo(3)
        assertThat(work.flatMap { info -> info.tags }).containsAtLeast(
            ProviderSyncWorker::class.java.name,
            XtreamIndexWorker::class.java.name,
            StalkerIndexWorker::class.java.name,
            BackgroundEpgSyncWorker::class.java.name
        )
    }

    @Test
    fun ordinaryStalkerEnqueue_keepsExistingProviderWork() {
        val providerId = 72L
        ProviderSyncWorker.enqueueProvider(context, providerId)
        StalkerIndexWorker.enqueue(context, providerId, initialDelaySeconds = ONE_HOUR_SECONDS)

        val work = workManager.getWorkInfosForUniqueWork(providerWorkUniqueName(providerId))
            .get(10, TimeUnit.SECONDS)

        assertThat(work).hasSize(1)
        assertThat(work.single().state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertThat(work.single().tags).contains(ProviderSyncWorker::class.java.name)
    }

    private companion object {
        const val ONE_HOUR_SECONDS = 3_600L
    }
}
