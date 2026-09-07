package com.streamvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.streamvault.data.local.entity.ProviderEntity
import com.streamvault.data.local.entity.SyncMetadataEntity
import com.streamvault.domain.model.ProviderType
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProviderDao {
    @Query("SELECT * FROM providers ORDER BY created_at DESC")
    abstract fun getAll(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers ORDER BY created_at DESC")
    abstract suspend fun getAllSync(): List<ProviderEntity>

    @Query("SELECT * FROM providers WHERE is_active = 1 LIMIT 1")
    abstract fun getActive(): Flow<ProviderEntity?>

    @Query("SELECT * FROM providers WHERE id = :id")
    abstract suspend fun getById(id: Long): ProviderEntity?

    @Query("SELECT * FROM providers WHERE id = :id")
    abstract fun getByIdSync(id: Long): ProviderEntity?

    @Query("SELECT * FROM providers WHERE id IN (:ids)")
    abstract suspend fun getByIds(ids: List<Long>): List<ProviderEntity>

    @Query("SELECT * FROM providers WHERE type = :type")
    abstract fun getByTypeSync(type: ProviderType): List<ProviderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDirect(provider: ProviderEntity): Long

    @Update
    protected abstract suspend fun updateDirect(provider: ProviderEntity)

    @Query("DELETE FROM providers WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("UPDATE providers SET is_active = 0")
    abstract suspend fun deactivateAll()

    @Query("UPDATE providers SET is_active = 1 WHERE id = :id")
    abstract suspend fun activate(id: Long)

    @Query("UPDATE providers SET last_synced_at = :timestamp WHERE id = :id")
    abstract suspend fun updateSyncTime(id: Long, timestamp: Long)

    @Transaction
    open suspend fun insert(provider: ProviderEntity): Long {
        if (provider.isActive) {
            deactivateAll()
        }
        return insertDirect(provider)
    }

    @Transaction
    open suspend fun update(provider: ProviderEntity) {
        if (provider.isActive) {
            deactivateAll()
        }
        updateDirect(provider)
    }

    /** Atomically deactivates all providers then activates the given one. */
    @Transaction
    open suspend fun setActive(id: Long) {
        deactivateAll()
        activate(id)
    }
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE provider_id = :providerId")
    fun get(providerId: Long): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata WHERE provider_id = :providerId")
    suspend fun getSync(providerId: Long): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE provider_id = :providerId")
    suspend fun delete(providerId: Long)
}
