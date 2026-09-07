package com.streamvault.app.player

import com.streamvault.domain.repository.PlaybackCompatibilityRepository
import com.streamvault.player.AudioCompatibilityMemoryStore
import com.streamvault.player.PlaybackSupportSnapshotStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerMemoryEntryPoint {
    fun playbackCompatibilityRepository(): PlaybackCompatibilityRepository
    fun audioCompatibilityMemoryStore(): AudioCompatibilityMemoryStore
    fun playbackSupportSnapshotStore(): PlaybackSupportSnapshotStore
}