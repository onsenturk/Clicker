package com.streamvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import com.streamvault.data.local.entity.CategoryCount
import com.streamvault.data.local.entity.FavoriteEntity
import com.streamvault.data.local.entity.PlaybackCompatibilityRecordEntity
import com.streamvault.data.local.entity.PlaybackHistoryEntity
import com.streamvault.data.local.entity.PlaybackHistoryLiteEntity
import com.streamvault.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteDao {
    @Query("SELECT * FROM favorites WHERE provider_id = :providerId AND group_id IS NULL ORDER BY position ASC")
    abstract fun getAllGlobal(providerId: Long): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE provider_id IN (:providerIds) AND group_id IS NULL ORDER BY provider_id ASC, position ASC")
    abstract fun getAllGlobalByProviders(providerIds: List<Long>): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE provider_id = :providerId AND content_type = :contentType AND group_id IS NULL ORDER BY position ASC")
    abstract fun getGlobalByType(providerId: Long, contentType: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE provider_id IN (:providerIds) AND content_type = :contentType AND group_id IS NULL ORDER BY provider_id ASC, position ASC")
    abstract fun getGlobalByTypeForProviders(providerIds: List<Long>, contentType: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE provider_id = :providerId AND content_type = :contentType ORDER BY position ASC")
    abstract fun getAllByType(providerId: Long, contentType: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE provider_id IN (:providerIds) AND content_type = :contentType ORDER BY provider_id ASC, position ASC")
    abstract fun getAllByTypeForProviders(providerIds: List<Long>, contentType: String): Flow<List<FavoriteEntity>>

    @Query(
        """
        SELECT f.*
        FROM favorites AS f
        INNER JOIN virtual_groups AS g
            ON g.id = f.group_id
           AND g.provider_id = f.provider_id
           AND g.content_type = f.content_type
        WHERE g.id = :groupId
        ORDER BY f.position ASC
        """
    )
    abstract fun getByGroup(groupId: Long): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE provider_id = :providerId AND content_id = :contentId AND content_type = :contentType AND (:groupId IS NULL AND group_id IS NULL OR group_id = :groupId) LIMIT 1")
    abstract suspend fun get(providerId: Long, contentId: Long, contentType: String, groupId: Long?): FavoriteEntity?

    @Query("SELECT COUNT(*) FROM favorites WHERE provider_id = :providerId AND group_id IS NULL AND content_type = :contentType")
    abstract fun getGlobalFavoriteCount(providerId: Long, contentType: String): Flow<Int>

    @Query("SELECT group_id as category_id, COUNT(*) as item_count FROM favorites WHERE provider_id = :providerId AND group_id IS NOT NULL AND content_type = :contentType GROUP BY group_id")
    abstract fun getGroupFavoriteCounts(providerId: Long, contentType: String): Flow<List<CategoryCount>>

    @Query("SELECT group_id as category_id, COUNT(*) as item_count FROM favorites WHERE provider_id IN (:providerIds) AND group_id IS NOT NULL AND content_type = :contentType GROUP BY group_id")
    abstract fun getGroupFavoriteCountsForProviders(providerIds: List<Long>, contentType: String): Flow<List<CategoryCount>>

    @Query("SELECT group_id FROM favorites WHERE provider_id = :providerId AND content_id = :contentId AND content_type = :contentType AND group_id IS NOT NULL")
    abstract suspend fun getGroupMemberships(providerId: Long, contentId: Long, contentType: String): List<Long>

    @Query("SELECT MAX(position) FROM favorites WHERE provider_id = :providerId AND (:groupId IS NULL AND group_id IS NULL OR group_id = :groupId)")
    abstract suspend fun getMaxPosition(providerId: Long, groupId: Long?): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDirect(favorite: FavoriteEntity)

    @Update
    abstract suspend fun updateAll(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE provider_id = :providerId AND content_id = :contentId AND content_type = :contentType AND (:groupId IS NULL AND group_id IS NULL OR group_id = :groupId)")
    abstract suspend fun delete(providerId: Long, contentId: Long, contentType: String, groupId: Long?)

    @Query("DELETE FROM favorites WHERE provider_id = :providerId AND content_id = :contentId AND content_type = :contentType")
    abstract suspend fun deleteByContent(providerId: Long, contentId: Long, contentType: String)

    @Query("DELETE FROM favorites WHERE provider_id = :providerId AND content_type = :contentType")
    abstract suspend fun deleteByProviderAndType(providerId: Long, contentType: String)

    @Query("DELETE FROM favorites WHERE provider_id = :providerId AND content_type = :contentType AND group_id IS NULL")
    abstract suspend fun deleteGlobalByProviderAndType(providerId: Long, contentType: String)

    @Query(
        "UPDATE favorites SET content_id = :newContentId, content_type = :newContentType, " +
            "group_id = NULL, group_key = 0 WHERE provider_id = :providerId " +
            "AND content_id = :oldContentId AND content_type = :oldContentType"
    )
    abstract suspend fun migrateContent(
        providerId: Long,
        oldContentId: Long,
        oldContentType: String,
        newContentId: Long,
        newContentType: String
    )

    @Query("DELETE FROM favorites WHERE content_type = 'LIVE' AND content_id NOT IN (SELECT id FROM channels)")
    abstract suspend fun deleteMissingLiveFavorites(): Int

    @Query("DELETE FROM favorites WHERE content_type = 'MOVIE' AND content_id NOT IN (SELECT id FROM movies)")
    abstract suspend fun deleteMissingMovieFavorites(): Int

    @Query("DELETE FROM favorites WHERE content_type = 'SERIES' AND content_id NOT IN (SELECT id FROM series)")
    abstract suspend fun deleteMissingSeriesFavorites(): Int

    @Query("SELECT * FROM favorites WHERE id = :favoriteId LIMIT 1")
    protected abstract suspend fun getById(favoriteId: Long): FavoriteEntity?

    @Query("SELECT provider_id, content_type FROM virtual_groups WHERE id = :groupId LIMIT 1")
    protected abstract suspend fun getGroupConstraint(groupId: Long): FavoriteGroupConstraint?

    @Query("UPDATE favorites SET group_id = :groupId, group_key = COALESCE(:groupId, 0) WHERE id = :favoriteId")
    protected abstract suspend fun updateGroupDirect(favoriteId: Long, groupId: Long?)

    @Transaction
    open suspend fun insert(favorite: FavoriteEntity) {
        validateGroupAssignment(
            providerId = favorite.providerId,
            contentType = favorite.contentType.name,
            groupId = favorite.groupId
        )
        insertDirect(favorite)
    }

    @Transaction
    open suspend fun updateGroup(favoriteId: Long, groupId: Long?) {
        if (groupId == null) {
            updateGroupDirect(favoriteId, null)
            return
        }

        val favorite = getById(favoriteId)
            ?: throw IllegalArgumentException("Favorite $favoriteId does not exist")

        validateGroupAssignment(
            providerId = favorite.providerId,
            contentType = favorite.contentType.name,
            groupId = groupId
        )
        updateGroupDirect(favoriteId, groupId)
    }

    private suspend fun validateGroupAssignment(providerId: Long, contentType: String, groupId: Long?) {
        val targetGroupId = groupId ?: return
        val group = getGroupConstraint(targetGroupId)
            ?: throw IllegalArgumentException("Favorite group $targetGroupId does not exist")

        require(group.providerId == providerId) {
            "Favorite group $targetGroupId belongs to provider ${group.providerId}, not $providerId"
        }
        require(group.contentType == contentType) {
            "Favorite group $targetGroupId accepts ${group.contentType}, not $contentType"
        }
    }
}

@Dao
@RewriteQueriesToDropUnusedColumns
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY last_watched_at DESC LIMIT :limit")
    fun getRecentlyWatched(limit: Int = 100): Flow<List<PlaybackHistoryLiteEntity>>

    @Query("SELECT * FROM playback_history WHERE provider_id = :providerId ORDER BY last_watched_at DESC LIMIT :limit")
    fun getRecentlyWatchedByProvider(providerId: Long, limit: Int = 100): Flow<List<PlaybackHistoryLiteEntity>>

    @Query("SELECT * FROM playback_history WHERE provider_id IN (:providerIds) ORDER BY last_watched_at DESC LIMIT :limit")
    fun getRecentlyWatchedByProviders(providerIds: Set<Long>, limit: Int = 100): Flow<List<PlaybackHistoryLiteEntity>>

    @Query("SELECT * FROM playback_history WHERE provider_id = :providerId ORDER BY last_watched_at DESC")
    fun getByProvider(providerId: Long): Flow<List<PlaybackHistoryLiteEntity>>

    @Query("SELECT * FROM playback_history ORDER BY last_watched_at DESC")
    suspend fun getAllSync(): List<PlaybackHistoryEntity>

    @Query("SELECT * FROM playback_history WHERE content_id = :contentId AND content_type = :contentType AND provider_id = :providerId")
    suspend fun get(contentId: Long, contentType: String, providerId: Long): PlaybackHistoryEntity?

    @Query(
        """
        SELECT ph.* FROM playback_history ph
        JOIN movies current_movie
          ON current_movie.id = :contentId
         AND current_movie.provider_id = :providerId
        JOIN tmdb_identity identity
          ON identity.tmdb_id = current_movie.tmdb_id
         AND identity.content_type = 'MOVIE'
        JOIN movies candidate_movie
          ON candidate_movie.tmdb_id = identity.tmdb_id
        WHERE current_movie.tmdb_id IS NOT NULL
          AND ph.content_type = 'MOVIE'
          AND ph.provider_id = candidate_movie.provider_id
          AND ph.content_id = candidate_movie.id
        ORDER BY ph.last_watched_at DESC
        LIMIT 1
        """
    )
    suspend fun getLatestMovieHistoryBySharedTmdb(contentId: Long, providerId: Long): PlaybackHistoryEntity?

    @Query(
        """
        SELECT ph.* FROM playback_history ph
        JOIN series current_series
          ON current_series.id = :seriesId
         AND current_series.provider_id = :providerId
        JOIN tmdb_identity identity
          ON identity.tmdb_id = current_series.tmdb_id
         AND identity.content_type = 'SERIES'
        JOIN series candidate_series
          ON candidate_series.tmdb_id = identity.tmdb_id
        WHERE current_series.tmdb_id IS NOT NULL
          AND (
              (ph.content_type = 'SERIES' AND ph.content_id = candidate_series.id)
              OR (
                  ph.content_type = 'SERIES_EPISODE'
                  AND ph.series_id = candidate_series.id
              )
          )
          AND ph.provider_id = candidate_series.provider_id
        ORDER BY ph.last_watched_at DESC
        LIMIT 1
        """
    )
    suspend fun getLatestSeriesHistoryBySharedTmdb(seriesId: Long, providerId: Long): PlaybackHistoryEntity?

    @Query(
        """
        SELECT * FROM playback_history
        WHERE content_type = 'SERIES_EPISODE'
          AND provider_id = :providerId
          AND series_id = :seriesId
          AND season_number = :seasonNumber
          AND episode_number = :episodeNumber
        ORDER BY last_watched_at DESC
        LIMIT 1
        """
    )
    suspend fun getLatestEpisodeHistoryByCoordinates(
        providerId: Long,
        seriesId: Long,
        seasonNumber: Int,
        episodeNumber: Int
    ): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE content_id = :contentId AND content_type = :contentType AND provider_id = :providerId")
    suspend fun delete(contentId: Long, contentType: String, providerId: Long)

    @Query(
        "UPDATE playback_history SET content_id = :newContentId, content_type = :newContentType, " +
            "series_id = :newSeriesId, season_number = :seasonNumber, episode_number = :episodeNumber " +
            "WHERE content_id = :oldContentId AND content_type = :oldContentType AND provider_id = :providerId"
    )
    suspend fun migrateContent(
        oldContentId: Long,
        oldContentType: String,
        newContentId: Long,
        newContentType: String,
        providerId: Long,
        newSeriesId: Long? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    )

    @Query("DELETE FROM playback_history")
    suspend fun deleteAll()

    @Query("DELETE FROM playback_history WHERE provider_id = :providerId")
    suspend fun deleteByProvider(providerId: Long)

    @Query("DELETE FROM playback_history WHERE provider_id = :providerId AND content_type = :contentType")
    suspend fun deleteByProviderAndType(providerId: Long, contentType: String)
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY used_at DESC")
    suspend fun getAllSync(): List<SearchHistoryEntity>

    @Query("SELECT * FROM search_history WHERE query = :query AND content_scope = :contentScope AND provider_id = :providerId LIMIT 1")
    suspend fun get(query: String, contentScope: String, providerId: Long): SearchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SearchHistoryEntity): Long

    @Query(
        """
        SELECT * FROM search_history
        WHERE content_scope = :contentScope
          AND provider_id = :providerId
        ORDER BY used_at DESC
        LIMIT :limit
        """
    )
    fun observeRecent(contentScope: String, providerId: Long, limit: Int): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: SearchHistoryEntity): Long

    @Query(
        """
        UPDATE search_history
        SET used_at = :usedAt,
            use_count = use_count + 1
        WHERE query = :query
          AND content_scope = :contentScope
          AND provider_id = :providerId
        """
    )
    suspend fun incrementUseCount(query: String, contentScope: String, providerId: Long, usedAt: Long): Int

    @Transaction
    suspend fun record(query: String, contentScope: String, providerId: Long, usedAt: Long) {
        val updated = incrementUseCount(query, contentScope, providerId, usedAt)
        if (updated > 0) return

        val inserted = insertIgnore(
            SearchHistoryEntity(
                query = query,
                contentScope = contentScope,
                providerId = providerId,
                usedAt = usedAt,
                useCount = 1
            )
        )
        if (inserted == -1L) {
            incrementUseCount(query, contentScope, providerId, usedAt)
        }
    }

    @Query("DELETE FROM search_history WHERE content_scope = :contentScope AND provider_id = :providerId")
    suspend fun deleteByScope(contentScope: String, providerId: Long)

    @Query("DELETE FROM search_history WHERE provider_id = :providerId")
    suspend fun deleteByProvider(providerId: Long)

    @Query("DELETE FROM search_history WHERE used_at < :minUsedAt")
    suspend fun pruneOlderThan(minUsedAt: Long)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()
}

@Dao
interface PlaybackCompatibilityDao {
    @Query(
        """
        SELECT * FROM playback_compatibility_records
        WHERE device_fingerprint = :deviceFingerprint
          AND stream_type = :streamType
          AND video_mime_type = :videoMimeType
          AND resolution_bucket = :resolutionBucket
        ORDER BY failure_count DESC, last_failed_at DESC
        """
    )
    suspend fun getKnownBadCandidates(
        deviceFingerprint: String,
        streamType: String,
        videoMimeType: String,
        resolutionBucket: String
    ): List<PlaybackCompatibilityRecordEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompatibilityRecordIgnore(record: PlaybackCompatibilityRecordEntity): Long

    @Query(
        """
        UPDATE playback_compatibility_records
        SET device_model = :deviceModel,
            android_sdk = :androidSdk,
            failure_type = :failureType,
            last_failed_at = :failedAt,
            failure_count = failure_count + 1
        WHERE device_fingerprint = :deviceFingerprint
          AND stream_type = :streamType
          AND video_mime_type = :videoMimeType
          AND resolution_bucket = :resolutionBucket
          AND decoder_name = :decoderName
          AND surface_type = :surfaceType
        """
    )
    suspend fun updateFailureRecord(
        deviceFingerprint: String,
        deviceModel: String,
        androidSdk: Int,
        streamType: String,
        videoMimeType: String,
        resolutionBucket: String,
        decoderName: String,
        surfaceType: String,
        failureType: String,
        failedAt: Long
    ): Int

    @Transaction
    suspend fun recordFailure(
        deviceFingerprint: String,
        deviceModel: String,
        androidSdk: Int,
        streamType: String,
        videoMimeType: String,
        resolutionBucket: String,
        decoderName: String,
        surfaceType: String,
        failureType: String,
        failedAt: Long
    ) {
        val updated = updateFailureRecord(
            deviceFingerprint = deviceFingerprint,
            deviceModel = deviceModel,
            androidSdk = androidSdk,
            streamType = streamType,
            videoMimeType = videoMimeType,
            resolutionBucket = resolutionBucket,
            decoderName = decoderName,
            surfaceType = surfaceType,
            failureType = failureType,
            failedAt = failedAt
        )
        if (updated > 0) return

        val inserted = insertCompatibilityRecordIgnore(
            PlaybackCompatibilityRecordEntity(
                deviceFingerprint = deviceFingerprint,
                deviceModel = deviceModel,
                androidSdk = androidSdk,
                streamType = streamType,
                videoMimeType = videoMimeType,
                resolutionBucket = resolutionBucket,
                decoderName = decoderName,
                surfaceType = surfaceType,
                failureType = failureType,
                lastFailedAt = failedAt,
                failureCount = 1
            )
        )
        if (inserted == -1L) {
            updateFailureRecord(
                deviceFingerprint = deviceFingerprint,
                deviceModel = deviceModel,
                androidSdk = androidSdk,
                streamType = streamType,
                videoMimeType = videoMimeType,
                resolutionBucket = resolutionBucket,
                decoderName = decoderName,
                surfaceType = surfaceType,
                failureType = failureType,
                failedAt = failedAt
            )
        }
    }

    @Query(
        """
        UPDATE playback_compatibility_records
        SET device_model = :deviceModel,
            android_sdk = :androidSdk,
            last_succeeded_at = :succeededAt,
            success_count = success_count + 1
        WHERE device_fingerprint = :deviceFingerprint
          AND stream_type = :streamType
          AND video_mime_type = :videoMimeType
          AND resolution_bucket = :resolutionBucket
          AND decoder_name = :decoderName
          AND surface_type = :surfaceType
        """
    )
    suspend fun updateSuccessRecord(
        deviceFingerprint: String,
        deviceModel: String,
        androidSdk: Int,
        streamType: String,
        videoMimeType: String,
        resolutionBucket: String,
        decoderName: String,
        surfaceType: String,
        succeededAt: Long
    ): Int

    @Transaction
    suspend fun recordSuccess(
        deviceFingerprint: String,
        deviceModel: String,
        androidSdk: Int,
        streamType: String,
        videoMimeType: String,
        resolutionBucket: String,
        decoderName: String,
        surfaceType: String,
        succeededAt: Long
    ) {
        val updated = updateSuccessRecord(
            deviceFingerprint = deviceFingerprint,
            deviceModel = deviceModel,
            androidSdk = androidSdk,
            streamType = streamType,
            videoMimeType = videoMimeType,
            resolutionBucket = resolutionBucket,
            decoderName = decoderName,
            surfaceType = surfaceType,
            succeededAt = succeededAt
        )
        if (updated > 0) return

        val inserted = insertCompatibilityRecordIgnore(
            PlaybackCompatibilityRecordEntity(
                deviceFingerprint = deviceFingerprint,
                deviceModel = deviceModel,
                androidSdk = androidSdk,
                streamType = streamType,
                videoMimeType = videoMimeType,
                resolutionBucket = resolutionBucket,
                decoderName = decoderName,
                surfaceType = surfaceType,
                lastSucceededAt = succeededAt,
                successCount = 1
            )
        )
        if (inserted == -1L) {
            updateSuccessRecord(
                deviceFingerprint = deviceFingerprint,
                deviceModel = deviceModel,
                androidSdk = androidSdk,
                streamType = streamType,
                videoMimeType = videoMimeType,
                resolutionBucket = resolutionBucket,
                decoderName = decoderName,
                surfaceType = surfaceType,
                succeededAt = succeededAt
            )
        }
    }

    @Query("DELETE FROM playback_compatibility_records WHERE last_failed_at < :olderThanMs AND last_succeeded_at < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long): Int

    @Query(
        """
        DELETE FROM playback_compatibility_records
        WHERE id NOT IN (
            SELECT id FROM playback_compatibility_records
            ORDER BY MAX(last_failed_at, last_succeeded_at) DESC
            LIMIT :maxRecords
        )
        """
    )
    suspend fun keepMostRecent(maxRecords: Int): Int
}
