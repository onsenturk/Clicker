package com.streamvault.app.ui

import com.streamvault.domain.manager.DriveBackupSyncManager
import com.streamvault.domain.repository.CombinedM3uRepository
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.usecase.ImportBackup
import com.streamvault.domain.usecase.ValidateAndAddProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProviderSetupTestEntryPoint {
    fun providerRepository(): ProviderRepository
    fun combinedM3uRepository(): CombinedM3uRepository
    fun validateAndAddProvider(): ValidateAndAddProvider
    fun importBackup(): ImportBackup
    fun driveBackupSyncManager(): DriveBackupSyncManager
}