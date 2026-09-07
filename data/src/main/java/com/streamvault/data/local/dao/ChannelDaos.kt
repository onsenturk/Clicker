package com.streamvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.streamvault.data.local.entity.CategoryCount
import com.streamvault.data.local.entity.CategoryEntity
import com.streamvault.data.local.entity.ChannelBrowseEntity
import com.streamvault.data.local.entity.ChannelEntity
import com.streamvault.data.local.entity.ChannelGuideLookupEntity
import com.streamvault.data.local.entity.ChannelGuideSyncEntity
import com.streamvault.data.local.entity.ChannelPreferenceEntity
import com.streamvault.data.local.entity.VirtualGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChannelDao {
    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId
        ORDER BY c.number ASC
        """
    )
    abstract fun getByProvider(providerId: Long): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId AND c.error_count = 0
        ORDER BY c.number ASC
        """
    )
    abstract fun getByProviderWithoutErrors(providerId: Long): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId AND c.error_count = 0
        ORDER BY c.number ASC
        LIMIT :limit
        """
    )
    abstract fun getByProviderWithoutErrorsBrowsePage(providerId: Long, limit: Int): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId
        ORDER BY c.number ASC
        LIMIT :limit
        """
    )
    abstract fun getByProviderBrowsePage(providerId: Long, limit: Int): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId AND c.category_id = :categoryId
        ORDER BY c.number ASC
        LIMIT :limit
        """
    )
    abstract fun getByCategoryBrowsePage(providerId: Long, categoryId: Long, limit: Int): Flow<List<ChannelBrowseEntity>>

    @Query("SELECT * FROM channels WHERE provider_id = :providerId ORDER BY number ASC LIMIT :limit OFFSET :offset")
    abstract fun getByProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<ChannelEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId AND c.category_id = :categoryId
        ORDER BY c.number ASC
        """
    )
    abstract fun getByCategory(providerId: Long, categoryId: Long): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId AND c.category_id = :categoryId AND c.error_count = 0
        ORDER BY c.number ASC
        """
    )
    abstract fun getByCategoryWithoutErrors(providerId: Long, categoryId: Long): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId AND c.category_id = :categoryId AND c.error_count = 0
        ORDER BY c.number ASC
        LIMIT :limit
        """
    )
    abstract fun getByCategoryWithoutErrorsBrowsePage(
        providerId: Long,
        categoryId: Long,
        limit: Int
    ): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId
        ORDER BY c.number ASC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun getByProviderBrowsePageOffset(providerId: Long, limit: Int, offset: Int): List<ChannelBrowseEntity>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId AND c.error_count = 0
        ORDER BY c.number ASC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun getByProviderWithoutErrorsBrowsePageOffset(providerId: Long, limit: Int, offset: Int): List<ChannelBrowseEntity>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId AND c.category_id = :categoryId
        ORDER BY c.number ASC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun getByCategoryBrowsePageOffset(providerId: Long, categoryId: Long, limit: Int, offset: Int): List<ChannelBrowseEntity>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId AND c.category_id = :categoryId AND c.error_count = 0
        ORDER BY c.number ASC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun getByCategoryWithoutErrorsBrowsePageOffset(providerId: Long, categoryId: Long, limit: Int, offset: Int): List<ChannelBrowseEntity>

    @Query("SELECT * FROM channels WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY number ASC LIMIT :limit OFFSET :offset")
    abstract fun getByCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<ChannelEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        JOIN channels_fts ON c.id = channels_fts.rowid
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId
          AND channels_fts MATCH :query
        ORDER BY c.name ASC
        LIMIT :limit
        """
    )
    abstract fun search(providerId: Long, query: String, limit: Int): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId
          AND (
              LOWER(c.name) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(c.group_title, '')) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(c.category_name, '')) LIKE LOWER(:queryLike) ESCAPE '\'
          )
        ORDER BY c.name ASC
        LIMIT :limit
        """
    )
    abstract fun searchFallback(providerId: Long, queryLike: String, limit: Int): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        JOIN channels_fts ON c.id = channels_fts.rowid
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId
          AND c.category_id = :categoryId
          AND channels_fts MATCH :query
        ORDER BY c.name ASC
        LIMIT :limit
        """
    )
    abstract fun searchByCategory(providerId: Long, categoryId: Long, query: String, limit: Int): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId
          AND c.category_id = :categoryId
          AND (
              LOWER(c.name) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(c.group_title, '')) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(c.category_name, '')) LIKE LOWER(:queryLike) ESCAPE '\'
          )
        ORDER BY c.name ASC
        LIMIT :limit
        """
    )
    abstract fun searchByCategoryFallback(providerId: Long, categoryId: Long, queryLike: String, limit: Int): Flow<List<ChannelBrowseEntity>>

    @Query("SELECT * FROM channels WHERE id = :id")
    abstract suspend fun getById(id: Long): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(channel: ChannelEntity): Long

    @Query("SELECT * FROM channels WHERE provider_id = :providerId AND stream_id = :streamId LIMIT 1")
    abstract suspend fun getByStreamId(providerId: Long, streamId: Long): ChannelEntity?

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.id = :id
        LIMIT 1
        """
    )
    abstract suspend fun getBrowseById(id: Long): ChannelBrowseEntity?

    @Query(
        """
        SELECT id, stream_id, epg_channel_id
        FROM channels
        WHERE id IN (:ids)
        """
    )
    abstract suspend fun getGuideLookupsByIds(ids: List<Long>): List<ChannelGuideLookupEntity>

    @Query(
        """
        SELECT name, stream_id, epg_channel_id
        FROM channels
        WHERE provider_id = :providerId
        ORDER BY number ASC, id ASC
        """
    )
    abstract suspend fun getGuideSyncEntriesByProvider(providerId: Long): List<ChannelGuideSyncEntity>

    @Query("SELECT * FROM channels WHERE provider_id = :providerId")
    abstract suspend fun getByProviderSync(providerId: Long): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE provider_id = :providerId AND category_id = :categoryId")
    abstract suspend fun getByCategorySync(providerId: Long, categoryId: Long): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(channels: List<ChannelEntity>)

    @Update
    abstract suspend fun updateAll(channels: List<ChannelEntity>)

    @Query("SELECT id, stream_id AS remote_id FROM channels WHERE provider_id = :providerId")
    abstract suspend fun getIdMappings(providerId: Long): List<RemoteIdMapping>

    @Query("SELECT COUNT(*) FROM channels WHERE provider_id = :providerId")
    abstract suspend fun countByProvider(providerId: Long): Int

    @Query("DELETE FROM channels WHERE provider_id = :providerId")
    abstract suspend fun deleteByProvider(providerId: Long)

    @Query("DELETE FROM channels WHERE id IN (:ids)")
    abstract suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM channels WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Transaction
    open suspend fun replaceAll(providerId: Long, channels: List<ChannelEntity>) {
        val existingByRemoteId = getIdMappings(providerId).associate { it.remoteId to it.id }
        val remapped = channels
            .distinctBy { it.streamId }
            .map { entity -> entity.copy(id = existingByRemoteId[entity.streamId] ?: 0L) }
        deleteByProvider(providerId)
        insertAll(remapped)
    }

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.id IN (:ids)
        """
    )
    abstract fun getByIds(ids: List<Long>): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.logical_group_id IN (:logicalGroupIds)
        ORDER BY c.provider_id ASC, c.number ASC, c.name ASC
        """
    )
    abstract fun getByLogicalGroupIds(logicalGroupIds: List<String>): Flow<List<ChannelBrowseEntity>>

    @Query(
        """
        SELECT c.id, c.stream_id, c.name, c.logo_url, c.group_title, c.category_id, c.category_name, c.stream_url,
               c.epg_channel_id, c.number, c.catch_up_supported, c.catch_up_days, c.catchUpSource,
               c.provider_id,
               (SELECT guide_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS guide_source_policy,
               (SELECT channel_logo_source_policy FROM provider_configs WHERE provider_id = c.provider_id) AS channel_logo_source_policy,
               ec.icon_url AS epg_icon_url,
               c.is_adult, c.is_user_protected, c.logical_group_id, c.error_count
        FROM channels c
        JOIN providers p ON p.id = c.provider_id
        LEFT JOIN channel_epg_mappings cem ON cem.provider_channel_id = c.id AND cem.provider_id = c.provider_id
        LEFT JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id AND ec.xmltv_channel_id = cem.xmltv_channel_id
        WHERE c.provider_id = :providerId AND c.logical_group_id = :logicalGroupId
        ORDER BY c.number ASC, c.name ASC
        """
    )
    abstract suspend fun getByLogicalGroupId(providerId: Long, logicalGroupId: String): List<ChannelBrowseEntity>

    @Query("SELECT category_id, COUNT(*) as item_count FROM channels WHERE provider_id = :providerId AND category_id IS NOT NULL GROUP BY category_id")
    abstract fun getRawCategoryCounts(providerId: Long): Flow<List<CategoryCount>>

    @Query(
        """
        SELECT category_id, COUNT(*) as item_count
        FROM channels
        WHERE provider_id = :providerId
          AND category_id IS NOT NULL
          AND NOT (TRIM(name) LIKE '##%' AND TRIM(name) LIKE '%##')
        GROUP BY category_id
        """
    )
    abstract fun getCategoryCounts(providerId: Long): Flow<List<CategoryCount>>

    @Query(
        """
        SELECT
            category_id,
            COUNT(
                DISTINCT CASE
                    WHEN logical_group_id IS NOT NULL AND logical_group_id != '' THEN logical_group_id
                    ELSE CAST(id AS TEXT)
                END
            ) AS item_count
        FROM channels
        WHERE provider_id = :providerId
          AND category_id IS NOT NULL
        GROUP BY category_id
        """
    )
    abstract fun getRawGroupedCategoryCounts(providerId: Long): Flow<List<CategoryCount>>

    @Query(
        """
        SELECT
            category_id,
            COUNT(
                DISTINCT CASE
                    WHEN logical_group_id IS NOT NULL AND logical_group_id != '' THEN logical_group_id
                    ELSE CAST(id AS TEXT)
                END
            ) AS item_count
        FROM channels
        WHERE provider_id = :providerId
          AND category_id IS NOT NULL
          AND NOT (TRIM(name) LIKE '##%' AND TRIM(name) LIKE '%##')
        GROUP BY category_id
        """
    )
    abstract fun getGroupedCategoryCounts(providerId: Long): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM channels WHERE provider_id = :providerId")
    abstract fun getRawCount(providerId: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*)
        FROM channels
        WHERE provider_id = :providerId
          AND NOT (TRIM(name) LIKE '##%' AND TRIM(name) LIKE '%##')
        """
    )
    abstract fun getCount(providerId: Long): Flow<Int>

    @Query("SELECT COALESCE(MAX(catch_up_days), 0) FROM channels WHERE catch_up_supported = 1")
    abstract suspend fun getMaxCatchUpDaysAcrossAllProviders(): Int

    @Query("UPDATE channels SET is_user_protected = :isProtected WHERE provider_id = :providerId AND category_id = :categoryId")
    abstract suspend fun updateProtectionStatus(providerId: Long, categoryId: Long, isProtected: Boolean)

    @Query("UPDATE channels SET is_user_protected = :isProtected WHERE id = :id AND provider_id = :providerId")
    abstract suspend fun updateItemProtection(id: Long, providerId: Long, isProtected: Boolean): Int

    @Query("UPDATE channels SET is_user_protected = 0 WHERE provider_id = :providerId AND category_id IN (:categoryIds)")
    abstract suspend fun clearProtectionForCategories(providerId: Long, categoryIds: List<Long>)

    @Query("UPDATE channels SET is_user_protected = 0 WHERE provider_id = :providerId")
    abstract suspend fun clearItemProtectionByProvider(providerId: Long)

    @Query("UPDATE channels SET is_user_protected = CASE WHEN category_id IN (SELECT category_id FROM categories WHERE provider_id = :providerId AND type = 'LIVE' AND is_user_protected = 1) THEN 1 ELSE 0 END WHERE provider_id = :providerId")
    abstract suspend fun resetProtectionToCategoryState(providerId: Long)

    @Query("UPDATE channels SET error_count = error_count + 1 WHERE id = :id")
    abstract suspend fun incrementErrorCount(id: Long)

    @Query("UPDATE channels SET error_count = 0 WHERE id = :id")
    abstract suspend fun resetErrorCount(id: Long)

    @Query("""
        UPDATE channels SET logo_url = (
            SELECT ec.icon_url
            FROM channel_epg_mappings cem
            JOIN epg_channels ec ON ec.epg_source_id = cem.epg_source_id
                AND ec.xmltv_channel_id = cem.xmltv_channel_id
            WHERE cem.provider_channel_id = channels.id
                AND cem.provider_id = :providerId
                AND ec.icon_url IS NOT NULL AND ec.icon_url != ''
            LIMIT 1
        )
        WHERE provider_id = :providerId AND (logo_url IS NULL OR logo_url = '')
    """)
    abstract suspend fun backfillEpgIcons(providerId: Long)
}

@Dao
interface ChannelPreferenceDao {
    @Query("SELECT * FROM channel_preferences")
    suspend fun getAllSync(): List<ChannelPreferenceEntity>

    @Query("DELETE FROM channel_preferences WHERE channel_id IN (SELECT id FROM channels WHERE provider_id = :providerId)")
    suspend fun deleteByProvider(providerId: Long)

    @Query("SELECT aspect_ratio FROM channel_preferences WHERE channel_id = :channelId LIMIT 1")
    fun observeAspectRatio(channelId: Long): Flow<String?>

    @Query("SELECT audio_video_offset_ms FROM channel_preferences WHERE channel_id = :channelId LIMIT 1")
    fun observeAudioVideoOffset(channelId: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preference: ChannelPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(preference: ChannelPreferenceEntity): Long

    @Query("UPDATE channel_preferences SET aspect_ratio = :aspectRatio, updated_at = :updatedAt WHERE channel_id = :channelId")
    suspend fun updateAspectRatio(channelId: Long, aspectRatio: String?, updatedAt: Long): Int

    @Query("UPDATE channel_preferences SET audio_video_offset_ms = :offsetMs, updated_at = :updatedAt WHERE channel_id = :channelId")
    suspend fun updateAudioVideoOffset(channelId: Long, offsetMs: Int?, updatedAt: Long): Int

    @Transaction
    suspend fun setAspectRatio(channelId: Long, aspectRatio: String) {
        val updatedAt = System.currentTimeMillis()
        if (updateAspectRatio(channelId, aspectRatio, updatedAt) == 0) {
            val inserted = insertIgnore(
                ChannelPreferenceEntity(
                    channelId = channelId,
                    aspectRatio = aspectRatio,
                    updatedAt = updatedAt
                )
            )
            if (inserted == -1L) {
                updateAspectRatio(channelId, aspectRatio, updatedAt)
            }
        }
    }

    @Transaction
    suspend fun setAudioVideoOffset(channelId: Long, offsetMs: Int?) {
        val updatedAt = System.currentTimeMillis()
        if (updateAudioVideoOffset(channelId, offsetMs, updatedAt) == 0) {
            val inserted = insertIgnore(
                ChannelPreferenceEntity(
                    channelId = channelId,
                    audioVideoOffsetMs = offsetMs,
                    updatedAt = updatedAt
                )
            )
            if (inserted == -1L) {
                updateAudioVideoOffset(channelId, offsetMs, updatedAt)
            }
        }
    }

    @Query("DELETE FROM channel_preferences")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE provider_id = :providerId AND type = :type ORDER BY provider_order ASC, id ASC")
    fun getByProviderAndType(providerId: Long, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE provider_id = :providerId AND type = :type ORDER BY provider_order ASC, id ASC")
    suspend fun getByProviderAndTypeSync(providerId: Long, type: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun updateAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE provider_id = :providerId AND type = :type")
    suspend fun deleteByProviderAndType(providerId: Long, type: String)

    @Query("DELETE FROM categories WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Transaction
    suspend fun replaceAll(providerId: Long, type: String, categories: List<CategoryEntity>) {
        deleteByProviderAndType(providerId, type)
        insertAll(categories)
    }

    @Query("UPDATE categories SET is_user_protected = :isProtected WHERE provider_id = :providerId AND category_id = :categoryId AND type = :type")
    suspend fun updateProtectionStatus(providerId: Long, categoryId: Long, type: String, isProtected: Boolean)
}

@Dao
interface VirtualGroupDao {
    @Query("SELECT * FROM virtual_groups WHERE provider_id = :providerId AND content_type = :contentType ORDER BY position ASC")
    fun getByType(providerId: Long, contentType: String): Flow<List<VirtualGroupEntity>>

    @Query("SELECT * FROM virtual_groups WHERE provider_id IN (:providerIds) AND content_type = :contentType ORDER BY provider_id ASC, position ASC")
    fun getByTypeForProviders(providerIds: List<Long>, contentType: String): Flow<List<VirtualGroupEntity>>

    @Query("SELECT * FROM virtual_groups WHERE id = :id")
    suspend fun getById(id: Long): VirtualGroupEntity?

    @Query("SELECT MAX(position) FROM virtual_groups WHERE provider_id = :providerId AND content_type = :contentType")
    suspend fun getMaxPosition(providerId: Long, contentType: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: VirtualGroupEntity): Long

    @Query("UPDATE virtual_groups SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM virtual_groups WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM virtual_groups WHERE provider_id = :providerId AND content_type = :contentType")
    suspend fun deleteByProviderAndType(providerId: Long, contentType: String)
}
