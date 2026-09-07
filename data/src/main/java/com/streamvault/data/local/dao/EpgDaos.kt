package com.streamvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import com.streamvault.data.local.entity.ChannelEpgMappingEntity
import com.streamvault.data.local.entity.EpgChannelEntity
import com.streamvault.data.local.entity.EpgProgrammeEntity
import com.streamvault.data.local.entity.EpgSourceEntity
import com.streamvault.data.local.entity.ProgramBrowseEntity
import com.streamvault.data.local.entity.ProgramEntity
import com.streamvault.data.local.entity.ProviderEpgSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
@RewriteQueriesToDropUnusedColumns
interface ProgramDao {
    @Query(
        """
        SELECT
            id,
            provider_id,
            channel_id,
            title,
            CASE
                WHEN LENGTH(description) > 600 THEN SUBSTR(description, 1, 600) || '...'
                ELSE description
            END AS description,
            start_time,
            end_time,
            lang,
                        rating,
                        image_url,
                        genre,
                        category,
            has_archive
        FROM programs
        WHERE provider_id = :providerId
          AND channel_id = :channelId
          AND end_time > :startTime
          AND start_time < :endTime
        ORDER BY start_time ASC
        """
    )
    fun getForChannel(providerId: Long, channelId: String, startTime: Long, endTime: Long): Flow<List<ProgramBrowseEntity>>

    @Query(
        """
        SELECT
            id,
            provider_id,
            channel_id,
            title,
            CASE
                WHEN LENGTH(description) > 600 THEN SUBSTR(description, 1, 600) || '...'
                ELSE description
            END AS description,
            start_time,
            end_time,
            lang,
                        rating,
                        image_url,
                        genre,
                        category,
            has_archive
        FROM programs
        WHERE provider_id = :providerId
          AND channel_id IN (:channelIds)
          AND end_time > :startTime
          AND start_time < :endTime
        ORDER BY channel_id ASC, start_time ASC
        """
    )
    fun getForChannels(providerId: Long, channelIds: List<String>, startTime: Long, endTime: Long): Flow<List<ProgramBrowseEntity>>

    @Query(
        """
        SELECT
            programs.id,
            programs.provider_id,
            programs.channel_id,
            programs.title,
            CASE
                WHEN LENGTH(programs.description) > 600 THEN SUBSTR(programs.description, 1, 600) || '...'
                ELSE programs.description
            END AS description,
            programs.start_time,
            programs.end_time,
            programs.lang,
            programs.rating,
            programs.image_url,
            programs.genre,
            programs.category,
            programs.has_archive
        FROM programs
        INNER JOIN channels
            ON channels.provider_id = programs.provider_id
           AND (
               channels.epg_channel_id = programs.channel_id
               OR CAST(channels.stream_id AS TEXT) = programs.channel_id
           )
        WHERE programs.provider_id = :providerId
          AND channels.category_id = :categoryId
          AND programs.end_time > :startTime
          AND programs.start_time < :endTime
        ORDER BY channels.number ASC, programs.channel_id ASC, programs.start_time ASC
        """
    )
    fun getForCategory(providerId: Long, categoryId: Long, startTime: Long, endTime: Long): Flow<List<ProgramBrowseEntity>>

    @Query(
        """
        SELECT
            programs.id,
            programs.provider_id,
            programs.channel_id,
            programs.title,
            CASE
                WHEN LENGTH(programs.description) > 600 THEN SUBSTR(programs.description, 1, 600) || '...'
                ELSE programs.description
            END AS description,
            programs.start_time,
            programs.end_time,
            programs.lang,
                        programs.rating,
                        programs.image_url,
                        programs.genre,
                        programs.category,
            programs.has_archive
        FROM programs
        WHERE programs.provider_id = :providerId
          AND programs.end_time > :startTime
          AND programs.start_time < :endTime
          AND (
              LOWER(programs.title) LIKE LOWER(:queryPattern) ESCAPE '\'
              OR LOWER(programs.description) LIKE LOWER(:queryPattern) ESCAPE '\'
          )
          AND (
              :categoryId IS NULL
              OR EXISTS (
                  SELECT 1 FROM channels
                  WHERE channels.provider_id = programs.provider_id
                    AND (
                        channels.epg_channel_id = programs.channel_id
                        OR CAST(channels.stream_id AS TEXT) = programs.channel_id
                    )
                    AND channels.category_id = :categoryId
              )
          )
        ORDER BY programs.start_time ASC, programs.channel_id ASC
        LIMIT :limit
        """
    )
    fun searchPrograms(
        providerId: Long,
        queryPattern: String,
        startTime: Long,
        endTime: Long,
        categoryId: Long?,
        limit: Int
    ): Flow<List<ProgramBrowseEntity>>

    @Query(
        """
        SELECT
            id,
            provider_id,
            channel_id,
            title,
            CASE
                WHEN LENGTH(description) > 600 THEN SUBSTR(description, 1, 600) || '...'
                ELSE description
            END AS description,
            start_time,
            end_time,
            lang,
                        rating,
                        image_url,
                        genre,
                        category,
            has_archive
        FROM programs
        WHERE provider_id = :providerId
          AND channel_id = :channelId
          AND start_time <= :now
          AND end_time > :now
        LIMIT 1
        """
    )
    fun getNowPlaying(providerId: Long, channelId: String, now: Long): Flow<ProgramBrowseEntity?>

    @Query(
        """
        SELECT
            id,
            provider_id,
            channel_id,
            title,
            CASE
                WHEN LENGTH(description) > 600 THEN SUBSTR(description, 1, 600) || '...'
                ELSE description
            END AS description,
            start_time,
            end_time,
            lang,
                        rating,
                        image_url,
                        genre,
                        category,
            has_archive
        FROM programs
        WHERE provider_id = :providerId
          AND channel_id IN (:channelIds)
          AND start_time <= :now
          AND end_time > :now
        """
    )
    fun getNowPlayingForChannels(providerId: Long, channelIds: List<String>, now: Long): Flow<List<ProgramBrowseEntity>>

    @Query(
        """
        SELECT
            id,
            provider_id,
            channel_id,
            title,
            CASE
                WHEN LENGTH(description) > 600 THEN SUBSTR(description, 1, 600) || '...'
                ELSE description
            END AS description,
            start_time,
            end_time,
            lang,
                        rating,
                        image_url,
                        genre,
                        category,
            has_archive
        FROM programs
        WHERE provider_id = :providerId
          AND channel_id IN (:channelIds)
          AND start_time <= :now
          AND end_time > :now
        """
    )
    suspend fun getNowPlayingForChannelsSync(providerId: Long, channelIds: List<String>, now: Long): List<ProgramBrowseEntity>

    @Query(
        """
        SELECT
            id,
            provider_id,
            channel_id,
            title,
            CASE
                WHEN LENGTH(description) > 600 THEN SUBSTR(description, 1, 600) || '...'
                ELSE description
            END AS description,
            start_time,
            end_time,
            lang,
                        rating,
                        image_url,
                        genre,
                        category,
            has_archive
        FROM programs
        WHERE provider_id = :providerId
          AND channel_id IN (:channelIds)
          AND end_time > :startTime
          AND start_time < :endTime
        ORDER BY channel_id ASC, start_time ASC
        """
    )
    suspend fun getForChannelsSync(providerId: Long, channelIds: List<String>, startTime: Long, endTime: Long): List<ProgramBrowseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<ProgramEntity>)

        @Query(
                """
                SELECT DISTINCT channel_id
                FROM programs
                WHERE provider_id = :providerId
                    AND channel_id IN (:channelIds)
                """
        )
        suspend fun getChannelIdsWithPrograms(providerId: Long, channelIds: List<String>): List<String>

    @Query("SELECT COUNT(*) FROM programs WHERE provider_id = :providerId")
    suspend fun countByProvider(providerId: Long): Int

    @Query("SELECT COUNT(*) FROM programs WHERE provider_id = :providerId")
    fun observeCountByProvider(providerId: Long): kotlinx.coroutines.flow.Flow<Int>

    @Query("DELETE FROM programs WHERE end_time < :beforeTime")
    suspend fun deleteOld(beforeTime: Long): Int

    @Query("DELETE FROM programs WHERE provider_id = :providerId")
    suspend fun deleteByProvider(providerId: Long)

    @Query("UPDATE programs SET provider_id = :targetProviderId WHERE provider_id = :sourceProviderId")
    suspend fun moveToProvider(sourceProviderId: Long, targetProviderId: Long)

    @Query("DELETE FROM programs WHERE provider_id = :providerId AND channel_id = :channelId")
    suspend fun deleteForChannel(providerId: Long, channelId: String)
}

@Dao
interface EpgSourceDao {
    @Query("SELECT * FROM epg_sources WHERE id > 0 ORDER BY priority ASC, name ASC")
    fun getAll(): Flow<List<EpgSourceEntity>>

    @Query("SELECT * FROM epg_sources WHERE id > 0 ORDER BY priority ASC, name ASC")
    suspend fun getAllSync(): List<EpgSourceEntity>

    @Query("SELECT * FROM epg_sources WHERE id = :id")
    suspend fun getById(id: Long): EpgSourceEntity?

    @Query("SELECT * FROM epg_sources WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): EpgSourceEntity?

    @Query("SELECT * FROM epg_sources WHERE id > 0 AND enabled = 1 ORDER BY priority ASC, name ASC")
    suspend fun getEnabled(): List<EpgSourceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(source: EpgSourceEntity): Long

    @Update
    suspend fun update(source: EpgSourceEntity)

    @Query("DELETE FROM epg_sources WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE epg_sources SET enabled = :enabled, updated_at = :now WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE epg_sources SET last_refresh_at = :at, last_error = :error, updated_at = :at WHERE id = :id")
    suspend fun updateRefreshStatus(id: Long, at: Long, error: String?)

    @Query("UPDATE epg_sources SET last_error = :error, updated_at = :at WHERE id = :id")
    suspend fun updateRefreshError(id: Long, error: String?, at: Long = System.currentTimeMillis())

    @Query("UPDATE epg_sources SET last_refresh_at = :at, last_success_at = :at, last_error = NULL, updated_at = :at WHERE id = :id")
    suspend fun updateRefreshSuccess(id: Long, at: Long)

    @Query("UPDATE epg_sources SET etag = :etag, last_modified_header = :lastModified WHERE id = :id")
    suspend fun updateConditionalHeaders(id: Long, etag: String?, lastModified: String?)
}

@Dao
abstract class ProviderEpgSourceDao {
    @Query("""
        SELECT pes.*, es.name AS epg_source_name, es.url AS epg_source_url
        FROM provider_epg_sources pes
        JOIN epg_sources es ON es.id = pes.epg_source_id
        WHERE pes.provider_id = :providerId
        ORDER BY pes.priority ASC
    """)
    abstract fun getForProvider(providerId: Long): Flow<List<ProviderEpgSourceWithDetails>>

    @Query("""
        SELECT pes.*
        FROM provider_epg_sources pes
        JOIN epg_sources es ON es.id = pes.epg_source_id
        WHERE pes.provider_id = :providerId AND pes.enabled = 1 AND es.enabled = 1
        ORDER BY pes.priority ASC
    """)
    abstract suspend fun getEnabledForProviderSync(providerId: Long): List<ProviderEpgSourceEntity>

    @Query("""
        SELECT pes.*
        FROM provider_epg_sources pes
        WHERE pes.provider_id = :providerId
        ORDER BY pes.priority ASC
    """)
    abstract suspend fun getForProviderSync(providerId: Long): List<ProviderEpgSourceEntity>

    @Query("SELECT DISTINCT provider_id FROM provider_epg_sources WHERE epg_source_id = :epgSourceId")
    abstract suspend fun getProviderIdsForSourceSync(epgSourceId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(assignment: ProviderEpgSourceEntity): Long

    @Update
    abstract suspend fun update(assignment: ProviderEpgSourceEntity)

    @Query("DELETE FROM provider_epg_sources WHERE provider_id = :providerId AND epg_source_id = :epgSourceId")
    abstract suspend fun delete(providerId: Long, epgSourceId: Long)

    @Query("DELETE FROM provider_epg_sources WHERE provider_id = :providerId")
    abstract suspend fun deleteByProvider(providerId: Long)

    /** Atomically swaps the priority of two assignment rows within a single transaction. */
    @Transaction
    open suspend fun swapPriorities(
        entity1: ProviderEpgSourceEntity,
        entity2: ProviderEpgSourceEntity
    ) {
        update(entity1)
        update(entity2)
    }
}

@Dao
interface EpgChannelDao {
    @Query("SELECT * FROM epg_channels WHERE epg_source_id = :sourceId ORDER BY display_name ASC")
    suspend fun getBySource(sourceId: Long): List<EpgChannelEntity>

    @Query("""
        SELECT * FROM epg_channels
        WHERE epg_source_id = :sourceId
          AND (LOWER(xmltv_channel_id) LIKE LOWER(:pattern) ESCAPE '\'
               OR LOWER(display_name) LIKE LOWER(:pattern) ESCAPE '\'
               OR LOWER(normalized_name) LIKE LOWER(:pattern) ESCAPE '\')
        ORDER BY display_name ASC
        LIMIT :limit
    """)
    suspend fun searchBySource(sourceId: Long, pattern: String, limit: Int): List<EpgChannelEntity>

    @Query("SELECT * FROM epg_channels WHERE epg_source_id = :sourceId AND xmltv_channel_id = :channelId LIMIT 1")
    suspend fun getBySourceAndChannelId(sourceId: Long, channelId: String): EpgChannelEntity?

    @Query("SELECT * FROM epg_channels WHERE epg_source_id IN (:sourceIds)")
    suspend fun getBySources(sourceIds: List<Long>): List<EpgChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<EpgChannelEntity>)

    @Query("DELETE FROM epg_channels WHERE epg_source_id = :sourceId")
    suspend fun deleteBySource(sourceId: Long)

    @Query("UPDATE epg_channels SET epg_source_id = :newSourceId WHERE epg_source_id = :oldSourceId")
    suspend fun moveToSource(oldSourceId: Long, newSourceId: Long)
}

@Dao
interface EpgProgrammeDao {
    @Query("""
        SELECT * FROM epg_programmes
        WHERE epg_source_id = :sourceId
          AND xmltv_channel_id = :channelId
          AND end_time > :startTime
          AND start_time < :endTime
        ORDER BY start_time ASC
    """)
    suspend fun getForChannel(sourceId: Long, channelId: String, startTime: Long, endTime: Long): List<EpgProgrammeEntity>

    @Query("""
        SELECT * FROM epg_programmes
        WHERE epg_source_id = :sourceId
          AND xmltv_channel_id IN (:channelIds)
          AND end_time > :startTime
          AND start_time < :endTime
        ORDER BY xmltv_channel_id ASC, start_time ASC
    """)
    suspend fun getForChannels(sourceId: Long, channelIds: List<String>, startTime: Long, endTime: Long): List<EpgProgrammeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programmes: List<EpgProgrammeEntity>)

    @Query("DELETE FROM epg_programmes WHERE epg_source_id = :sourceId")
    suspend fun deleteBySource(sourceId: Long)

    @Query("UPDATE epg_programmes SET epg_source_id = :newSourceId WHERE epg_source_id = :oldSourceId")
    suspend fun moveToSource(oldSourceId: Long, newSourceId: Long)

    @Query("DELETE FROM epg_programmes WHERE end_time < :beforeTime")
    suspend fun deleteOld(beforeTime: Long): Int

    @Query("SELECT COUNT(*) FROM epg_programmes WHERE epg_source_id = :sourceId")
    suspend fun countBySource(sourceId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM epg_programmes
        WHERE epg_source_id = :sourceId
          AND xmltv_channel_id = :channelId
          AND end_time > :now
    """)
    suspend fun countUpcomingForChannel(sourceId: Long, channelId: String, now: Long): Int
}

@Dao
abstract class ChannelEpgMappingDao {
    @Query("SELECT * FROM channel_epg_mappings WHERE provider_id = :providerId")
    abstract suspend fun getForProvider(providerId: Long): List<ChannelEpgMappingEntity>

    @Query("SELECT * FROM channel_epg_mappings WHERE provider_id = :providerId AND provider_channel_id = :channelId LIMIT 1")
    abstract suspend fun getForChannel(providerId: Long, channelId: Long): ChannelEpgMappingEntity?

    @Query("""
        SELECT * FROM channel_epg_mappings
        WHERE provider_id = :providerId
          AND provider_channel_id IN (:channelIds)
    """)
    abstract suspend fun getForChannels(providerId: Long, channelIds: List<Long>): List<ChannelEpgMappingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(mappings: List<ChannelEpgMappingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(mapping: ChannelEpgMappingEntity)

    @Query("DELETE FROM channel_epg_mappings WHERE provider_id = :providerId AND provider_channel_id = :channelId")
    abstract suspend fun deleteForChannel(providerId: Long, channelId: Long)

    @Query("DELETE FROM channel_epg_mappings WHERE provider_id = :providerId")
    abstract suspend fun deleteByProvider(providerId: Long)

    @Transaction
    open suspend fun replaceForProvider(providerId: Long, mappings: List<ChannelEpgMappingEntity>) {
        deleteByProvider(providerId)
        if (mappings.isNotEmpty()) {
            insertAll(mappings)
        }
    }

    @Query("""
        SELECT source_type, match_type, COUNT(*) as cnt
        FROM channel_epg_mappings
        WHERE provider_id = :providerId
        GROUP BY source_type, match_type
    """)
    abstract suspend fun getResolutionStats(providerId: Long): List<EpgResolutionStatRow>

    @Query(
        """
        SELECT COUNT(*) FROM channel_epg_mappings
        WHERE provider_id = :providerId
          AND confidence > 0
          AND confidence < :minConfidence
        """
    )
    abstract suspend fun countLowConfidence(providerId: Long, minConfidence: Float): Int

    @Query(
        """
        SELECT COUNT(*) FROM channel_epg_mappings
        WHERE provider_id = :providerId
          AND failed_attempts < :maxAttempts
          AND (
              source_type = 'NONE'
              OR (confidence > 0 AND confidence < :minConfidence)
          )
        """
    )
    abstract suspend fun countRematchCandidates(providerId: Long, minConfidence: Float, maxAttempts: Int): Int

    @Query(
        """
        SELECT * FROM channel_epg_mappings
        WHERE provider_id = :providerId
          AND failed_attempts < :maxAttempts
          AND (
              source_type = 'NONE'
              OR (confidence > 0 AND confidence < :minConfidence)
          )
        ORDER BY confidence ASC, failed_attempts ASC, provider_channel_id ASC
        LIMIT :limit
        """
    )
    abstract suspend fun getChannelsNeedingRematch(
        providerId: Long,
        minConfidence: Float,
        maxAttempts: Int,
        limit: Int
    ): List<ChannelEpgMappingEntity>
}
