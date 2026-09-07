package com.streamvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.streamvault.data.local.entity.CategoryCount
import com.streamvault.data.local.entity.EpisodeBrowseEntity
import com.streamvault.data.local.entity.EpisodeEntity
import com.streamvault.data.local.entity.MovieBrowseEntity
import com.streamvault.data.local.entity.MovieEntity
import com.streamvault.data.local.entity.SeriesBrowseEntity
import com.streamvault.data.local.entity.SeriesEntity
import com.streamvault.data.local.entity.TmdbIdentityEntity
import com.streamvault.data.local.entity.VodCatalogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
@RewriteQueriesToDropUnusedColumns
interface MovieDao {
    @Query("SELECT * FROM movies WHERE provider_id = :providerId ORDER BY added_at DESC, name ASC, id ASC")
    fun getByProvider(providerId: Long): Flow<List<MovieBrowseEntity>>

    /** SQL-level parental filter — avoids loading protected items into memory. */
    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND is_user_protected = 0 ORDER BY added_at DESC, name ASC, id ASC")
    fun getByProviderUnprotected(providerId: Long): Flow<List<MovieBrowseEntity>>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId ORDER BY added_at DESC, name ASC, id ASC LIMIT :limit OFFSET :offset")
    fun getByProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<MovieBrowseEntity>>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId ORDER BY name ASC, id ASC LIMIT :limit")
    suspend fun getByProviderCursorPage(providerId: Long, limit: Int): List<MovieBrowseEntity>

    @Query(
        """
        SELECT * FROM movies
        WHERE provider_id = :providerId
          AND (name > :lastName OR (name = :lastName AND id > :lastId))
        ORDER BY name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getByProviderCursorPageAfter(providerId: Long, lastName: String, lastId: Long, limit: Int): List<MovieBrowseEntity>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
          AND EXISTS (
              SELECT 1 FROM favorites
              WHERE favorites.content_type = 'MOVIE'
                                AND favorites.provider_id = movies.provider_id
                AND favorites.group_id IS NULL
                AND favorites.content_id = movies.id
          )
        ORDER BY movies.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getFavoritesByProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM movies
        WHERE provider_id = :providerId
          AND EXISTS (
              SELECT 1 FROM favorites
              WHERE favorites.content_type = 'MOVIE'
                                AND favorites.provider_id = movies.provider_id
                AND favorites.group_id IS NULL
                AND favorites.content_id = movies.id
          )
        """
    )
    fun getFavoriteCountByProvider(providerId: Long): Flow<Int>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
          AND EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = movies.provider_id
                AND playback_history.content_type = 'MOVIE'
                AND playback_history.content_id = movies.id
                AND playback_history.resume_position_ms > 0
                AND (
                    playback_history.total_duration_ms <= 0
                    OR playback_history.resume_position_ms < CAST(playback_history.total_duration_ms * 0.95 AS INTEGER)
                )
          )
        ORDER BY movies.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getInProgressByProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM movies
        WHERE provider_id = :providerId
          AND EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = movies.provider_id
                AND playback_history.content_type = 'MOVIE'
                AND playback_history.content_id = movies.id
                AND playback_history.resume_position_ms > 0
                AND (
                    playback_history.total_duration_ms <= 0
                    OR playback_history.resume_position_ms < CAST(playback_history.total_duration_ms * 0.95 AS INTEGER)
                )
          )
        """
    )
    fun getInProgressCountByProvider(providerId: Long): Flow<Int>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
          AND NOT EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = movies.provider_id
                AND playback_history.content_type = 'MOVIE'
                AND playback_history.content_id = movies.id
                AND playback_history.resume_position_ms > 0
          )
        ORDER BY movies.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getUnwatchedByProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM movies
        WHERE movies.provider_id = :providerId
          AND NOT EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = movies.provider_id
                AND playback_history.content_type = 'MOVIE'
                AND playback_history.content_id = movies.id
                AND playback_history.resume_position_ms > 0
          )
        """
    )
    fun getUnwatchedCountByProvider(providerId: Long): Flow<Int>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
        ORDER BY COALESCE(movies.watch_count, 0) DESC, movies.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getByWatchCountProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
        ORDER BY COALESCE(movies.watch_count, 0) DESC, movies.name ASC, movies.id ASC
        LIMIT :limit
        """
    )
    suspend fun getByWatchCountProviderCursorPage(providerId: Long, limit: Int): List<MovieBrowseEntity>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
          AND (
              COALESCE(movies.watch_count, 0) < :lastWatchCount
              OR (
                  COALESCE(movies.watch_count, 0) = :lastWatchCount
                  AND (movies.name > :lastName OR (movies.name = :lastName AND movies.id > :lastId))
              )
          )
        ORDER BY COALESCE(movies.watch_count, 0) DESC, movies.name ASC, movies.id ASC
        LIMIT :limit
        """
    )
    suspend fun getByWatchCountProviderCursorPageAfter(
        providerId: Long,
        lastWatchCount: Int,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<MovieBrowseEntity>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY added_at DESC, name ASC, id ASC")
    fun getByCategory(providerId: Long, categoryId: Long): Flow<List<MovieBrowseEntity>>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY name ASC, id ASC LIMIT :limit")
    suspend fun getByCategoryCursorPage(providerId: Long, categoryId: Long, limit: Int): List<MovieBrowseEntity>

    @Query(
        """
        SELECT * FROM movies
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND (name > :lastName OR (name = :lastName AND id > :lastId))
        ORDER BY name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getByCategoryCursorPageAfter(
        providerId: Long,
        categoryId: Long,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<MovieBrowseEntity>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
          AND movies.category_id = :categoryId
          AND EXISTS (
              SELECT 1 FROM favorites
              WHERE favorites.content_type = 'MOVIE'
                                AND favorites.provider_id = movies.provider_id
                AND favorites.group_id IS NULL
                AND favorites.content_id = movies.id
          )
        ORDER BY movies.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getFavoritesByCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM movies
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND EXISTS (
              SELECT 1 FROM favorites
              WHERE favorites.content_type = 'MOVIE'
                                AND favorites.provider_id = movies.provider_id
                AND favorites.group_id IS NULL
                AND favorites.content_id = movies.id
          )
        """
    )
    fun getFavoriteCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
          AND movies.category_id = :categoryId
          AND EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = movies.provider_id
                AND playback_history.content_type = 'MOVIE'
                AND playback_history.content_id = movies.id
                AND playback_history.resume_position_ms > 0
                AND (
                    playback_history.total_duration_ms <= 0
                    OR playback_history.resume_position_ms < CAST(playback_history.total_duration_ms * 0.95 AS INTEGER)
                )
          )
        ORDER BY movies.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getInProgressByCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM movies
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = movies.provider_id
                AND playback_history.content_type = 'MOVIE'
                AND playback_history.content_id = movies.id
                AND playback_history.resume_position_ms > 0
                AND (
                    playback_history.total_duration_ms <= 0
                    OR playback_history.resume_position_ms < CAST(playback_history.total_duration_ms * 0.95 AS INTEGER)
                )
          )
        """
    )
    fun getInProgressCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
          AND movies.category_id = :categoryId
          AND NOT EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = movies.provider_id
                AND playback_history.content_type = 'MOVIE'
                AND playback_history.content_id = movies.id
                AND playback_history.resume_position_ms > 0
          )
        ORDER BY movies.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getUnwatchedByCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM movies
        WHERE movies.provider_id = :providerId
          AND movies.category_id = :categoryId
          AND NOT EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = movies.provider_id
                AND playback_history.content_type = 'MOVIE'
                AND playback_history.content_id = movies.id
                AND playback_history.resume_position_ms > 0
          )
        """
    )
    fun getUnwatchedCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
          AND movies.category_id = :categoryId
        ORDER BY COALESCE(movies.watch_count, 0) DESC, movies.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getByWatchCountCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
          AND movies.category_id = :categoryId
        ORDER BY COALESCE(movies.watch_count, 0) DESC, movies.name ASC, movies.id ASC
        LIMIT :limit
        """
    )
    suspend fun getByWatchCountCategoryCursorPage(providerId: Long, categoryId: Long, limit: Int): List<MovieBrowseEntity>

    @Query(
        """
        SELECT movies.* FROM movies
        WHERE movies.provider_id = :providerId
          AND movies.category_id = :categoryId
          AND (
              COALESCE(movies.watch_count, 0) < :lastWatchCount
              OR (
                  COALESCE(movies.watch_count, 0) = :lastWatchCount
                  AND (movies.name > :lastName OR (movies.name = :lastName AND movies.id > :lastId))
              )
          )
        ORDER BY COALESCE(movies.watch_count, 0) DESC, movies.name ASC, movies.id ASC
        LIMIT :limit
        """
    )
    suspend fun getByWatchCountCategoryCursorPageAfter(
        providerId: Long,
        categoryId: Long,
        lastWatchCount: Int,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<MovieBrowseEntity>

    /** SQL-level parental filter per category. */
    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND category_id = :categoryId AND is_user_protected = 0 ORDER BY added_at DESC, name ASC, id ASC")
    fun getByCategoryUnprotected(providerId: Long, categoryId: Long): Flow<List<MovieBrowseEntity>>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY added_at DESC, name ASC, id ASC LIMIT :limit OFFSET :offset")
    fun getByCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<MovieBrowseEntity>>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY added_at DESC, name ASC, id ASC LIMIT :limit")
    fun getByCategoryPreview(providerId: Long, categoryId: Long, limit: Int): Flow<List<MovieBrowseEntity>>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND rating > 0 ORDER BY rating DESC, name ASC LIMIT :limit")
    fun getTopRatedPreview(providerId: Long, limit: Int): Flow<List<MovieBrowseEntity>>

    @Query("SELECT COUNT(*) FROM movies WHERE provider_id = :providerId AND rating > 0")
    fun getTopRatedCountByProvider(providerId: Long): Flow<Int>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND rating > 0 ORDER BY rating DESC, name ASC, id ASC LIMIT :limit")
    suspend fun getTopRatedCursorPage(providerId: Long, limit: Int): List<MovieBrowseEntity>

    @Query(
        """
        SELECT * FROM movies
        WHERE provider_id = :providerId
          AND rating > 0
          AND (
              rating < :lastRating
              OR (rating = :lastRating AND (name > :lastName OR (name = :lastName AND id > :lastId)))
          )
        ORDER BY rating DESC, name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getTopRatedCursorPageAfter(
        providerId: Long,
        lastRating: Float,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<MovieBrowseEntity>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND category_id = :categoryId AND rating > 0 ORDER BY rating DESC, name ASC LIMIT :limit")
    fun getTopRatedByCategoryPreview(providerId: Long, categoryId: Long, limit: Int): Flow<List<MovieBrowseEntity>>

    @Query("SELECT COUNT(*) FROM movies WHERE provider_id = :providerId AND category_id = :categoryId AND rating > 0")
    fun getTopRatedCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND category_id = :categoryId AND rating > 0 ORDER BY rating DESC, name ASC, id ASC LIMIT :limit")
    suspend fun getTopRatedByCategoryCursorPage(providerId: Long, categoryId: Long, limit: Int): List<MovieBrowseEntity>

    @Query(
        """
        SELECT * FROM movies
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND rating > 0
          AND (
              rating < :lastRating
              OR (rating = :lastRating AND (name > :lastName OR (name = :lastName AND id > :lastId)))
          )
        ORDER BY rating DESC, name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getTopRatedByCategoryCursorPageAfter(
        providerId: Long,
        categoryId: Long,
        lastRating: Float,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<MovieBrowseEntity>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND added_at > 0 ORDER BY added_at DESC, name ASC, id ASC LIMIT :limit")
    fun getFreshPreview(providerId: Long, limit: Int): Flow<List<MovieBrowseEntity>>

        @Query(
            """
            SELECT * FROM movies
            WHERE provider_id = :providerId
            ORDER BY
                CASE WHEN COALESCE(release_date, '') != '' THEN 1 ELSE 0 END DESC,
                release_date DESC,
                CASE WHEN COALESCE(year, '') != '' THEN 1 ELSE 0 END DESC,
                year DESC,
                added_at DESC,
                name ASC,
                id ASC
            LIMIT :limit
            """
        )
        fun getReleasedPreview(providerId: Long, limit: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM movies
        WHERE provider_id = :providerId
          AND (
                            added_at > 0
          )
        """
    )
    fun getFreshCountByProvider(providerId: Long): Flow<Int>

        @Query("SELECT * FROM movies WHERE provider_id = :providerId AND added_at > 0 ORDER BY added_at DESC, name ASC, id ASC LIMIT :limit")
    suspend fun getFreshCursorPage(providerId: Long, limit: Int): List<MovieBrowseEntity>

    @Query(
        """
        SELECT * FROM movies
        WHERE provider_id = :providerId
          AND added_at > 0
          AND (
              added_at < :lastAddedAt
              OR (
                  added_at = :lastAddedAt
                  AND (name > :lastName OR (name = :lastName AND id > :lastId))
              )
          )
        ORDER BY added_at DESC, name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getFreshCursorPageAfter(
        providerId: Long,
        lastAddedAt: Long,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<MovieBrowseEntity>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND category_id = :categoryId AND added_at > 0 ORDER BY added_at DESC, name ASC, id ASC LIMIT :limit")
    fun getFreshByCategoryPreview(providerId: Long, categoryId: Long, limit: Int): Flow<List<MovieBrowseEntity>>

        @Query(
            """
            SELECT * FROM movies
            WHERE provider_id = :providerId
              AND category_id = :categoryId
            ORDER BY
                CASE WHEN COALESCE(release_date, '') != '' THEN 1 ELSE 0 END DESC,
                release_date DESC,
                CASE WHEN COALESCE(year, '') != '' THEN 1 ELSE 0 END DESC,
                year DESC,
                added_at DESC,
                name ASC,
                id ASC
            LIMIT :limit
            """
        )
        fun getReleasedByCategoryPreview(providerId: Long, categoryId: Long, limit: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM movies
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND (
                            added_at > 0
          )
        """
    )
    fun getFreshCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

        @Query("SELECT * FROM movies WHERE provider_id = :providerId AND category_id = :categoryId AND added_at > 0 ORDER BY added_at DESC, name ASC, id ASC LIMIT :limit")
    suspend fun getFreshByCategoryCursorPage(providerId: Long, categoryId: Long, limit: Int): List<MovieBrowseEntity>

    @Query(
        """
        SELECT * FROM movies
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND added_at > 0
          AND (
              added_at < :lastAddedAt
              OR (
                  added_at = :lastAddedAt
                  AND (name > :lastName OR (name = :lastName AND id > :lastId))
              )
          )
        ORDER BY added_at DESC, name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getFreshByCategoryCursorPageAfter(
        providerId: Long,
        categoryId: Long,
        lastAddedAt: Long,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<MovieBrowseEntity>

    @Query(
        """
        SELECT m.* FROM movies m
        JOIN movies_fts ON m.id = movies_fts.rowid
        WHERE m.provider_id = :providerId
          AND movies_fts MATCH :query
          AND (:includeProtected != 0 OR m.is_user_protected = 0)
        ORDER BY
          CASE
            WHEN LOWER(m.name) = LOWER(:rawQuery) THEN 0
            WHEN LOWER(m.name) LIKE LOWER(:prefixLike) ESCAPE '\' THEN 1
            ELSE 2
          END ASC,
          m.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchPage(
        providerId: Long,
        query: String,
        rawQuery: String,
        prefixLike: String,
        includeProtected: Int,
        limit: Int,
        offset: Int
    ): List<MovieBrowseEntity>

    @Query(
        """
        SELECT m.* FROM movies m
        JOIN movies_fts ON m.id = movies_fts.rowid
        WHERE m.provider_id = :providerId
          AND m.category_id = :categoryId
          AND movies_fts MATCH :query
          AND (:includeProtected != 0 OR m.is_user_protected = 0)
        ORDER BY
          CASE
            WHEN LOWER(m.name) = LOWER(:rawQuery) THEN 0
            WHEN LOWER(m.name) LIKE LOWER(:prefixLike) ESCAPE '\' THEN 1
            ELSE 2
          END ASC,
          m.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchByCategoryPage(
        providerId: Long,
        categoryId: Long,
        query: String,
        rawQuery: String,
        prefixLike: String,
        includeProtected: Int,
        limit: Int,
        offset: Int
    ): List<MovieBrowseEntity>

    @Query(
        """
        SELECT m.* FROM movies m
        JOIN movies_fts ON m.id = movies_fts.rowid
        WHERE m.provider_id = :providerId
          AND movies_fts MATCH :query
        ORDER BY m.name ASC
        LIMIT :limit
        """
    )
    fun search(providerId: Long, query: String, limit: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT m.* FROM movies m
        WHERE m.provider_id = :providerId
          AND (
              LOWER(m.name) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(m.genre, '')) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(m.category_name, '')) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(m.year, '')) LIKE LOWER(:queryLike) ESCAPE '\'
          )
        ORDER BY m.name ASC
        LIMIT :limit
        """
    )
    fun searchFallback(providerId: Long, queryLike: String, limit: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT m.* FROM movies m
        JOIN movies_fts ON m.id = movies_fts.rowid
        WHERE m.provider_id = :providerId
          AND m.category_id = :categoryId
          AND movies_fts MATCH :query
        ORDER BY m.name ASC
        LIMIT :limit
        """
    )
    fun searchByCategory(providerId: Long, categoryId: Long, query: String, limit: Int): Flow<List<MovieBrowseEntity>>

    @Query(
        """
        SELECT m.* FROM movies m
        WHERE m.provider_id = :providerId
          AND m.category_id = :categoryId
          AND (
              LOWER(m.name) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(m.genre, '')) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(m.category_name, '')) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(m.year, '')) LIKE LOWER(:queryLike) ESCAPE '\'
          )
        ORDER BY m.name ASC
        LIMIT :limit
        """
    )
    fun searchByCategoryFallback(providerId: Long, categoryId: Long, queryLike: String, limit: Int): Flow<List<MovieBrowseEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getById(id: Long): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: MovieEntity): Long

    @Query("SELECT * FROM movies WHERE provider_id = :providerId")
    suspend fun getByProviderSync(providerId: Long): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND tmdb_id = :tmdbId")
    suspend fun getByProviderAndTmdbIdSync(providerId: Long, tmdbId: Long): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND year = :year")
    suspend fun getByProviderAndYearSync(providerId: Long, year: String): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND release_date LIKE :yearPrefix")
    suspend fun getByProviderAndReleaseYearPrefixSync(providerId: Long, yearPrefix: String): List<MovieEntity>

    @Query("SELECT tmdb_id FROM movies WHERE provider_id = :providerId AND tmdb_id IS NOT NULL")
    suspend fun getTmdbIdsByProvider(providerId: Long): List<TmdbIdMapping>

    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    fun getByIds(ids: List<Long>): Flow<List<MovieBrowseEntity>>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND stream_id = :streamId")
    suspend fun getByStreamId(providerId: Long, streamId: Long): MovieEntity?

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND stream_id IN (:streamIds)")
    suspend fun getByStreamIds(providerId: Long, streamIds: List<Long>): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE provider_id = :providerId AND stream_id IN (:streamIds)")
    fun observeByStreamIds(providerId: Long, streamIds: List<Long>): Flow<List<MovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<MovieEntity>)

    @Query("SELECT id, stream_id AS remote_id FROM movies WHERE provider_id = :providerId")
    suspend fun getIdMappings(providerId: Long): List<RemoteIdMapping>

    @Query("SELECT id, stream_id AS remote_id FROM movies WHERE provider_id = :providerId AND category_id = :categoryId")
    suspend fun getIdMappingsByCategory(providerId: Long, categoryId: Long): List<RemoteIdMapping>

    @Update
    suspend fun update(movie: MovieEntity)

    @Update
    suspend fun updateAll(movies: List<MovieEntity>)

    @Query(
        """
        UPDATE movies
        SET watch_progress = COALESCE((
            SELECT resume_position_ms FROM playback_history
            WHERE playback_history.content_id = movies.id
              AND playback_history.content_type = 'MOVIE'
              AND playback_history.provider_id = movies.provider_id
        ), 0),
            watch_count = COALESCE((
                SELECT watch_count FROM playback_history
                WHERE playback_history.content_id = movies.id
                  AND playback_history.content_type = 'MOVIE'
                  AND playback_history.provider_id = movies.provider_id
            ), 0),
            last_watched_at = COALESCE((
                SELECT last_watched_at FROM playback_history
                WHERE playback_history.content_id = movies.id
                  AND playback_history.content_type = 'MOVIE'
                  AND playback_history.provider_id = movies.provider_id
            ), 0)
        WHERE id = :id AND provider_id = :providerId
        """
    )
    suspend fun syncWatchProgressFromHistory(id: Long, providerId: Long)

    @Query(
        """
        UPDATE movies
        SET watch_progress = COALESCE((
            SELECT resume_position_ms FROM playback_history
            WHERE playback_history.content_id = movies.id
              AND playback_history.content_type = 'MOVIE'
              AND playback_history.provider_id = movies.provider_id
        ), 0),
            watch_count = COALESCE((
                SELECT watch_count FROM playback_history
                WHERE playback_history.content_id = movies.id
                  AND playback_history.content_type = 'MOVIE'
                  AND playback_history.provider_id = movies.provider_id
            ), 0),
            last_watched_at = COALESCE((
                SELECT last_watched_at FROM playback_history
                WHERE playback_history.content_id = movies.id
                  AND playback_history.content_type = 'MOVIE'
                  AND playback_history.provider_id = movies.provider_id
            ), 0)
        WHERE provider_id = :providerId
        """
    )
    suspend fun syncWatchProgressFromHistoryByProvider(providerId: Long)

    @Query(
        """
        UPDATE movies
        SET watch_progress = COALESCE((
            SELECT resume_position_ms FROM playback_history
            WHERE playback_history.content_id = movies.id
              AND playback_history.content_type = 'MOVIE'
              AND playback_history.provider_id = movies.provider_id
        ), 0),
            watch_count = COALESCE((
                SELECT watch_count FROM playback_history
                WHERE playback_history.content_id = movies.id
                  AND playback_history.content_type = 'MOVIE'
                  AND playback_history.provider_id = movies.provider_id
            ), 0),
            last_watched_at = COALESCE((
                SELECT last_watched_at FROM playback_history
                WHERE playback_history.content_id = movies.id
                  AND playback_history.content_type = 'MOVIE'
                  AND playback_history.provider_id = movies.provider_id
            ), 0)
        """
    )
    suspend fun syncAllWatchProgressFromHistory()

    @Query("UPDATE movies SET watch_progress = 0, watch_count = 0, last_watched_at = 0")
    suspend fun resetAllWatchProgress()

    @Query("SELECT COUNT(*) FROM movies WHERE provider_id = :providerId")
    suspend fun countByProvider(providerId: Long): Int

    @Query("DELETE FROM movies WHERE provider_id = :providerId")
    suspend fun deleteByProvider(providerId: Long)

    @Query("DELETE FROM movies WHERE provider_id = :providerId AND category_id = :categoryId")
    suspend fun deleteByProviderAndCategory(providerId: Long, categoryId: Long)

    @Query("DELETE FROM movies WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM movies WHERE provider_id = :providerId AND category_id = :categoryId AND stream_id NOT IN (:remoteIds)")
    suspend fun deleteMissingByCategory(providerId: Long, categoryId: Long, remoteIds: List<Long>)

    @Query("""
        UPDATE movies
        SET watch_progress = (
            SELECT resume_position_ms FROM playback_history
            WHERE playback_history.content_id = movies.id
            AND playback_history.content_type = 'MOVIE'
            AND playback_history.provider_id = :providerId
        ),
            watch_count = COALESCE((
                SELECT watch_count FROM playback_history
                WHERE playback_history.content_id = movies.id
                AND playback_history.content_type = 'MOVIE'
                AND playback_history.provider_id = :providerId
            ), 0),
            last_watched_at = COALESCE((
                SELECT last_watched_at FROM playback_history
                WHERE playback_history.content_id = movies.id
                AND playback_history.content_type = 'MOVIE'
                AND playback_history.provider_id = :providerId
            ), 0)
        WHERE provider_id = :providerId AND EXISTS (
            SELECT 1 FROM playback_history
            WHERE playback_history.content_id = movies.id
            AND playback_history.content_type = 'MOVIE'
            AND playback_history.provider_id = :providerId
        )
    """)
    suspend fun restoreWatchProgress(providerId: Long)

    @Transaction
    suspend fun replaceAll(providerId: Long, movies: List<MovieEntity>) {
        val existingByRemoteId = getIdMappings(providerId).associate { it.remoteId to it.id }
        val remapped = movies
            .distinctBy { it.streamId }
            .map { entity -> entity.copy(id = existingByRemoteId[entity.streamId] ?: 0L) }
        deleteByProvider(providerId)
        insertAll(remapped)
        restoreWatchProgress(providerId)
    }

    @Transaction
    suspend fun replaceCategory(providerId: Long, categoryId: Long, movies: List<MovieEntity>) {
        val existingByRemoteId = getIdMappingsByCategory(providerId, categoryId).associate { it.remoteId to it.id }
        val remapped = movies
            .distinctBy { it.streamId }
            .map { entity -> entity.copy(id = existingByRemoteId[entity.streamId] ?: 0L) }

        if (remapped.isEmpty()) {
            deleteByProviderAndCategory(providerId, categoryId)
        } else {
            insertAll(remapped)
            deleteMissingByCategory(providerId, categoryId, remapped.map { it.streamId })
        }

        restoreWatchProgress(providerId)
    }

    @Transaction
    suspend fun upsertCategoryPage(providerId: Long, movies: List<MovieEntity>) {
        if (movies.isEmpty()) return
        val existingByRemoteId = getIdMappings(providerId).associate { it.remoteId to it.id }
        val remapped = movies
            .distinctBy { it.streamId }
            .map { entity -> entity.copy(id = existingByRemoteId[entity.streamId] ?: 0L) }
        insertAll(remapped)
        restoreWatchProgress(providerId)
    }

    @Query("SELECT category_id, COUNT(*) as item_count FROM movies WHERE provider_id = :providerId AND category_id IS NOT NULL GROUP BY category_id")
    fun getCategoryCounts(providerId: Long): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM movies WHERE provider_id = :providerId")
    fun getCount(providerId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM movies WHERE provider_id = :providerId AND category_id = :categoryId")
    fun getCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

    @Query("UPDATE movies SET is_user_protected = :isProtected WHERE provider_id = :providerId AND category_id = :categoryId")
    suspend fun updateProtectionStatus(providerId: Long, categoryId: Long, isProtected: Boolean)

    @Query("UPDATE movies SET is_user_protected = :isProtected WHERE id = :id AND provider_id = :providerId")
    suspend fun updateItemProtection(id: Long, providerId: Long, isProtected: Boolean): Int

    @Query("UPDATE movies SET is_user_protected = 0 WHERE provider_id = :providerId AND category_id IN (:categoryIds)")
    suspend fun clearProtectionForCategories(providerId: Long, categoryIds: List<Long>)

    @Query("UPDATE movies SET is_user_protected = 0 WHERE provider_id = :providerId")
    suspend fun clearItemProtectionByProvider(providerId: Long)

    @Query("UPDATE movies SET is_user_protected = CASE WHEN category_id IN (SELECT category_id FROM categories WHERE provider_id = :providerId AND type IN ('MOVIE', 'VOD') AND is_user_protected = 1) THEN 1 ELSE 0 END WHERE provider_id = :providerId")
    suspend fun resetProtectionToCategoryState(providerId: Long)
}

@Dao
@RewriteQueriesToDropUnusedColumns
interface SeriesDao {
    @Query("SELECT * FROM series WHERE provider_id = :providerId ORDER BY last_modified DESC, name ASC, id ASC")
    fun getByProvider(providerId: Long): Flow<List<SeriesBrowseEntity>>

    @Query("SELECT * FROM series WHERE provider_id = :providerId ORDER BY last_modified DESC, name ASC, id ASC LIMIT :limit OFFSET :offset")
    fun getByProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<SeriesBrowseEntity>>

    @Query("SELECT * FROM series WHERE provider_id = :providerId ORDER BY name ASC, id ASC LIMIT :limit")
    suspend fun getByProviderCursorPage(providerId: Long, limit: Int): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT * FROM series
        WHERE provider_id = :providerId
          AND (name > :lastName OR (name = :lastName AND id > :lastId))
        ORDER BY name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getByProviderCursorPageAfter(providerId: Long, lastName: String, lastId: Long, limit: Int): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT series.* FROM series
        WHERE series.provider_id = :providerId
          AND EXISTS (
              SELECT 1 FROM favorites
              WHERE favorites.content_type = 'SERIES'
                                AND favorites.provider_id = series.provider_id
                AND favorites.group_id IS NULL
                AND favorites.content_id = series.id
          )
        ORDER BY series.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getFavoritesByProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM series
        WHERE provider_id = :providerId
          AND EXISTS (
              SELECT 1 FROM favorites
              WHERE favorites.content_type = 'SERIES'
                                AND favorites.provider_id = series.provider_id
                AND favorites.group_id IS NULL
                AND favorites.content_id = series.id
          )
        """
    )
    fun getFavoriteCountByProvider(providerId: Long): Flow<Int>

    @Query(
        """
        SELECT series.* FROM series
        WHERE series.provider_id = :providerId
          AND EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = series.provider_id
                AND playback_history.resume_position_ms > 0
                AND (
                    playback_history.total_duration_ms <= 0
                    OR playback_history.resume_position_ms < CAST(playback_history.total_duration_ms * 0.95 AS INTEGER)
                )
                AND (
                    (playback_history.content_type = 'SERIES' AND playback_history.content_id = series.id)
                    OR (
                        playback_history.content_type = 'SERIES_EPISODE'
                        AND EXISTS (
                            SELECT 1 FROM episodes
                            WHERE episodes.id = playback_history.content_id
                              AND episodes.series_id = series.id
                        )
                    )
                )
          )
        ORDER BY series.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getInProgressByProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM series
        WHERE provider_id = :providerId
          AND EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = series.provider_id
                AND playback_history.resume_position_ms > 0
                AND (
                    playback_history.total_duration_ms <= 0
                    OR playback_history.resume_position_ms < CAST(playback_history.total_duration_ms * 0.95 AS INTEGER)
                )
                AND (
                    (playback_history.content_type = 'SERIES' AND playback_history.content_id = series.id)
                    OR (
                        playback_history.content_type = 'SERIES_EPISODE'
                        AND EXISTS (
                            SELECT 1 FROM episodes
                            WHERE episodes.id = playback_history.content_id
                              AND episodes.series_id = series.id
                        )
                    )
                )
          )
        """
    )
    fun getInProgressCountByProvider(providerId: Long): Flow<Int>

    @Query(
        """
        SELECT series.* FROM series
        WHERE series.provider_id = :providerId
          AND (
              NOT EXISTS (
                  SELECT 1 FROM episodes
                  WHERE episodes.series_id = series.id
                    AND episodes.provider_id = series.provider_id
              )
              OR EXISTS (
                  SELECT 1 FROM episodes e
                  WHERE e.series_id = series.id
                    AND e.provider_id = series.provider_id
                    AND NOT EXISTS (
                        SELECT 1 FROM playback_history ph
                        WHERE ph.content_id = e.id
                          AND ph.content_type = 'SERIES_EPISODE'
                          AND ph.provider_id = series.provider_id
                          AND ph.total_duration_ms > 0
                          AND ph.resume_position_ms >= CAST(ph.total_duration_ms * 0.95 AS INTEGER)
                    )
              )
          )
        ORDER BY series.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getUnwatchedByProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM series
        WHERE provider_id = :providerId
          AND (
              NOT EXISTS (
                  SELECT 1 FROM episodes
                  WHERE episodes.series_id = series.id
                    AND episodes.provider_id = :providerId
              )
              OR EXISTS (
                  SELECT 1 FROM episodes e
                  WHERE e.series_id = series.id
                    AND e.provider_id = :providerId
                    AND NOT EXISTS (
                        SELECT 1 FROM playback_history ph
                        WHERE ph.content_id = e.id
                          AND ph.content_type = 'SERIES_EPISODE'
                          AND ph.provider_id = :providerId
                          AND ph.total_duration_ms > 0
                          AND ph.resume_position_ms >= CAST(ph.total_duration_ms * 0.95 AS INTEGER)
                    )
              )
          )
        """
    )
    fun getUnwatchedCountByProvider(providerId: Long): Flow<Int>

    @Query(
        """
        SELECT series.* FROM series
        WHERE series.provider_id = :providerId
        ORDER BY COALESCE((
            SELECT MAX(playback_history.watch_count)
            FROM playback_history
            WHERE playback_history.provider_id = series.provider_id
              AND (
                  (playback_history.content_type = 'SERIES' AND playback_history.content_id = series.id)
                  OR (
                      playback_history.content_type = 'SERIES_EPISODE'
                      AND EXISTS (
                          SELECT 1 FROM episodes
                          WHERE episodes.id = playback_history.content_id
                            AND episodes.series_id = series.id
                      )
                  )
              )
        ), 0) DESC, series.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getByWatchCountProviderPage(providerId: Long, limit: Int, offset: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT series.* FROM series
        WHERE series.provider_id = :providerId
        ORDER BY COALESCE((
            SELECT MAX(playback_history.watch_count)
            FROM playback_history
            WHERE playback_history.provider_id = series.provider_id
              AND (
                  (playback_history.content_type = 'SERIES' AND playback_history.content_id = series.id)
                  OR (
                      playback_history.content_type = 'SERIES_EPISODE'
                      AND EXISTS (
                          SELECT 1 FROM episodes
                          WHERE episodes.id = playback_history.content_id
                            AND episodes.series_id = series.id
                      )
                  )
              )
        ), 0) DESC, series.name ASC, series.id ASC
        LIMIT :limit
        """
    )
    suspend fun getByWatchCountProviderCursorPage(providerId: Long, limit: Int): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT series.* FROM series
        WHERE series.provider_id = :providerId
          AND (
              COALESCE((
                  SELECT MAX(playback_history.watch_count)
                  FROM playback_history
                  WHERE playback_history.provider_id = series.provider_id
                    AND (
                        (playback_history.content_type = 'SERIES' AND playback_history.content_id = series.id)
                        OR (
                            playback_history.content_type = 'SERIES_EPISODE'
                            AND EXISTS (
                                SELECT 1 FROM episodes
                                WHERE episodes.id = playback_history.content_id
                                  AND episodes.series_id = series.id
                            )
                        )
                    )
              ), 0) < :lastWatchCount
              OR (
                  COALESCE((
                      SELECT MAX(playback_history.watch_count)
                      FROM playback_history
                      WHERE playback_history.provider_id = series.provider_id
                        AND (
                            (playback_history.content_type = 'SERIES' AND playback_history.content_id = series.id)
                            OR (
                                playback_history.content_type = 'SERIES_EPISODE'
                                AND EXISTS (
                                    SELECT 1 FROM episodes
                                    WHERE episodes.id = playback_history.content_id
                                      AND episodes.series_id = series.id
                                )
                            )
                        )
                  ), 0) = :lastWatchCount
                  AND (series.name > :lastName OR (series.name = :lastName AND series.id > :lastId))
              )
          )
        ORDER BY COALESCE((
            SELECT MAX(playback_history.watch_count)
            FROM playback_history
            WHERE playback_history.provider_id = series.provider_id
              AND (
                  (playback_history.content_type = 'SERIES' AND playback_history.content_id = series.id)
                  OR (
                      playback_history.content_type = 'SERIES_EPISODE'
                      AND EXISTS (
                          SELECT 1 FROM episodes
                          WHERE episodes.id = playback_history.content_id
                            AND episodes.series_id = series.id
                      )
                  )
              )
        ), 0) DESC, series.name ASC, series.id ASC
        LIMIT :limit
        """
    )
    suspend fun getByWatchCountProviderCursorPageAfter(
        providerId: Long,
        lastWatchCount: Int,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<SeriesBrowseEntity>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY last_modified DESC, name ASC, id ASC")
    fun getByCategory(providerId: Long, categoryId: Long): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT series.* FROM series
        WHERE series.provider_id = :providerId
          AND series.category_id = :categoryId
          AND EXISTS (
              SELECT 1 FROM favorites
              WHERE favorites.content_type = 'SERIES'
                                AND favorites.provider_id = series.provider_id
                AND favorites.group_id IS NULL
                AND favorites.content_id = series.id
          )
        ORDER BY series.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getFavoritesByCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM series
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND EXISTS (
              SELECT 1 FROM favorites
              WHERE favorites.content_type = 'SERIES'
                                AND favorites.provider_id = series.provider_id
                AND favorites.group_id IS NULL
                AND favorites.content_id = series.id
          )
        """
    )
    fun getFavoriteCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

    @Query(
        """
        SELECT series.* FROM series
        WHERE series.provider_id = :providerId
          AND series.category_id = :categoryId
          AND EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = series.provider_id
                AND playback_history.resume_position_ms > 0
                AND (
                    playback_history.total_duration_ms <= 0
                    OR playback_history.resume_position_ms < CAST(playback_history.total_duration_ms * 0.95 AS INTEGER)
                )
                AND (
                    (playback_history.content_type = 'SERIES' AND playback_history.content_id = series.id)
                    OR (
                        playback_history.content_type = 'SERIES_EPISODE'
                        AND EXISTS (
                            SELECT 1 FROM episodes
                            WHERE episodes.id = playback_history.content_id
                              AND episodes.series_id = series.id
                        )
                    )
                )
          )
        ORDER BY series.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getInProgressByCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM series
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND EXISTS (
              SELECT 1 FROM playback_history
              WHERE playback_history.provider_id = series.provider_id
                AND playback_history.resume_position_ms > 0
                AND (
                    playback_history.total_duration_ms <= 0
                    OR playback_history.resume_position_ms < CAST(playback_history.total_duration_ms * 0.95 AS INTEGER)
                )
                AND (
                    (playback_history.content_type = 'SERIES' AND playback_history.content_id = series.id)
                    OR (
                        playback_history.content_type = 'SERIES_EPISODE'
                        AND EXISTS (
                            SELECT 1 FROM episodes
                            WHERE episodes.id = playback_history.content_id
                              AND episodes.series_id = series.id
                        )
                    )
                )
          )
        """
    )
    fun getInProgressCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

    @Query(
        """
        SELECT series.* FROM series
        WHERE series.provider_id = :providerId
          AND series.category_id = :categoryId
          AND (
              NOT EXISTS (
                  SELECT 1 FROM episodes
                  WHERE episodes.series_id = series.id
                    AND episodes.provider_id = series.provider_id
              )
              OR EXISTS (
                  SELECT 1 FROM episodes e
                  WHERE e.series_id = series.id
                    AND e.provider_id = series.provider_id
                    AND NOT EXISTS (
                        SELECT 1 FROM playback_history ph
                        WHERE ph.content_id = e.id
                          AND ph.content_type = 'SERIES_EPISODE'
                          AND ph.provider_id = series.provider_id
                          AND ph.total_duration_ms > 0
                          AND ph.resume_position_ms >= CAST(ph.total_duration_ms * 0.95 AS INTEGER)
                    )
              )
          )
        ORDER BY series.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getUnwatchedByCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*)
        FROM series
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND (
              NOT EXISTS (
                  SELECT 1 FROM episodes
                  WHERE episodes.series_id = series.id
                    AND episodes.provider_id = :providerId
              )
              OR EXISTS (
                  SELECT 1 FROM episodes e
                  WHERE e.series_id = series.id
                    AND e.provider_id = :providerId
                    AND NOT EXISTS (
                        SELECT 1 FROM playback_history ph
                        WHERE ph.content_id = e.id
                          AND ph.content_type = 'SERIES_EPISODE'
                          AND ph.provider_id = :providerId
                          AND ph.total_duration_ms > 0
                          AND ph.resume_position_ms >= CAST(ph.total_duration_ms * 0.95 AS INTEGER)
                    )
              )
          )
        """
    )
    fun getUnwatchedCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

    @Query(
        """
        SELECT series.* FROM series
        WHERE series.provider_id = :providerId
          AND series.category_id = :categoryId
        ORDER BY COALESCE((
            SELECT MAX(playback_history.watch_count)
            FROM playback_history
            WHERE playback_history.provider_id = series.provider_id
              AND (
                  (playback_history.content_type = 'SERIES' AND playback_history.content_id = series.id)
                  OR (
                      playback_history.content_type = 'SERIES_EPISODE'
                      AND EXISTS (
                          SELECT 1 FROM episodes
                          WHERE episodes.id = playback_history.content_id
                            AND episodes.series_id = series.id
                      )
                  )
              )
        ), 0) DESC, series.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getByWatchCountCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<SeriesBrowseEntity>>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY last_modified DESC, name ASC, id ASC LIMIT :limit OFFSET :offset")
    fun getByCategoryPage(providerId: Long, categoryId: Long, limit: Int, offset: Int): Flow<List<SeriesBrowseEntity>>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY name ASC, id ASC LIMIT :limit")
    suspend fun getByCategoryCursorPage(providerId: Long, categoryId: Long, limit: Int): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT * FROM series
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND (name > :lastName OR (name = :lastName AND id > :lastId))
        ORDER BY name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getByCategoryCursorPageAfter(
        providerId: Long,
        categoryId: Long,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<SeriesBrowseEntity>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY last_modified DESC, name ASC, id ASC LIMIT :limit")
    fun getByCategoryPreview(providerId: Long, categoryId: Long, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query("SELECT * FROM series WHERE provider_id = :providerId ORDER BY rating DESC, name ASC LIMIT :limit")
    fun getTopRatedPreview(providerId: Long, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query("SELECT COUNT(*) FROM series WHERE provider_id = :providerId AND rating > 0")
    fun getTopRatedCountByProvider(providerId: Long): Flow<Int>

    @Query("SELECT * FROM series WHERE provider_id = :providerId ORDER BY rating DESC, name ASC, id ASC LIMIT :limit")
    suspend fun getTopRatedCursorPage(providerId: Long, limit: Int): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT * FROM series
        WHERE provider_id = :providerId
          AND (
              rating < :lastRating
              OR (rating = :lastRating AND (name > :lastName OR (name = :lastName AND id > :lastId)))
          )
        ORDER BY rating DESC, name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getTopRatedCursorPageAfter(
        providerId: Long,
        lastRating: Float,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<SeriesBrowseEntity>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY rating DESC, name ASC LIMIT :limit")
    fun getTopRatedByCategoryPreview(providerId: Long, categoryId: Long, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query("SELECT COUNT(*) FROM series WHERE provider_id = :providerId AND category_id = :categoryId AND rating > 0")
    fun getTopRatedCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY rating DESC, name ASC, id ASC LIMIT :limit")
    suspend fun getTopRatedByCategoryCursorPage(providerId: Long, categoryId: Long, limit: Int): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT * FROM series
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND (
              rating < :lastRating
              OR (rating = :lastRating AND (name > :lastName OR (name = :lastName AND id > :lastId)))
          )
        ORDER BY rating DESC, name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getTopRatedByCategoryCursorPageAfter(
        providerId: Long,
        categoryId: Long,
        lastRating: Float,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<SeriesBrowseEntity>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND last_modified > 0 ORDER BY last_modified DESC, name ASC LIMIT :limit")
    fun getFreshPreview(providerId: Long, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT * FROM series
        WHERE provider_id = :providerId
        ORDER BY
            CASE WHEN COALESCE(release_date, '') != '' THEN 1 ELSE 0 END DESC,
            release_date DESC,
            last_modified DESC,
            name ASC,
            id ASC
        LIMIT :limit
        """
    )
    fun getReleasedPreview(providerId: Long, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM series
                WHERE provider_id = :providerId
                    AND last_modified > 0
        """
    )
    fun getFreshCountByProvider(providerId: Long): Flow<Int>

        @Query("SELECT * FROM series WHERE provider_id = :providerId AND last_modified > 0 ORDER BY last_modified DESC, name ASC, id ASC LIMIT :limit")
    suspend fun getFreshCursorPage(providerId: Long, limit: Int): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT * FROM series
        WHERE provider_id = :providerId
                    AND last_modified > 0
          AND (
              last_modified < :lastModified
              OR (last_modified = :lastModified AND (name > :lastName OR (name = :lastName AND id > :lastId)))
          )
        ORDER BY last_modified DESC, name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getFreshCursorPageAfter(
        providerId: Long,
        lastModified: Long,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<SeriesBrowseEntity>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND category_id = :categoryId AND last_modified > 0 ORDER BY last_modified DESC, name ASC LIMIT :limit")
    fun getFreshByCategoryPreview(providerId: Long, categoryId: Long, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT * FROM series
        WHERE provider_id = :providerId
          AND category_id = :categoryId
        ORDER BY
            CASE WHEN COALESCE(release_date, '') != '' THEN 1 ELSE 0 END DESC,
            release_date DESC,
            last_modified DESC,
            name ASC,
            id ASC
        LIMIT :limit
        """
    )
    fun getReleasedByCategoryPreview(providerId: Long, categoryId: Long, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM series
                WHERE provider_id = :providerId
                    AND category_id = :categoryId
                    AND last_modified > 0
        """
    )
    fun getFreshCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

        @Query("SELECT * FROM series WHERE provider_id = :providerId AND category_id = :categoryId AND last_modified > 0 ORDER BY last_modified DESC, name ASC, id ASC LIMIT :limit")
    suspend fun getFreshByCategoryCursorPage(providerId: Long, categoryId: Long, limit: Int): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT * FROM series
        WHERE provider_id = :providerId
          AND category_id = :categoryId
                    AND last_modified > 0
          AND (
              last_modified < :lastModified
              OR (last_modified = :lastModified AND (name > :lastName OR (name = :lastName AND id > :lastId)))
          )
        ORDER BY last_modified DESC, name ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getFreshByCategoryCursorPageAfter(
        providerId: Long,
        categoryId: Long,
        lastModified: Long,
        lastName: String,
        lastId: Long,
        limit: Int
    ): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT s.* FROM series s
        JOIN series_fts ON s.id = series_fts.rowid
        WHERE s.provider_id = :providerId
          AND series_fts MATCH :query
          AND (:includeProtected != 0 OR s.is_user_protected = 0)
        ORDER BY
          CASE
            WHEN LOWER(s.name) = LOWER(:rawQuery) THEN 0
            WHEN LOWER(s.name) LIKE LOWER(:prefixLike) ESCAPE '\' THEN 1
            ELSE 2
          END ASC,
          s.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchPage(
        providerId: Long,
        query: String,
        rawQuery: String,
        prefixLike: String,
        includeProtected: Int,
        limit: Int,
        offset: Int
    ): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT s.* FROM series s
        JOIN series_fts ON s.id = series_fts.rowid
        WHERE s.provider_id = :providerId
          AND s.category_id = :categoryId
          AND series_fts MATCH :query
          AND (:includeProtected != 0 OR s.is_user_protected = 0)
        ORDER BY
          CASE
            WHEN LOWER(s.name) = LOWER(:rawQuery) THEN 0
            WHEN LOWER(s.name) LIKE LOWER(:prefixLike) ESCAPE '\' THEN 1
            ELSE 2
          END ASC,
          s.name ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchByCategoryPage(
        providerId: Long,
        categoryId: Long,
        query: String,
        rawQuery: String,
        prefixLike: String,
        includeProtected: Int,
        limit: Int,
        offset: Int
    ): List<SeriesBrowseEntity>

    @Query(
        """
        SELECT s.* FROM series s
        JOIN series_fts ON s.id = series_fts.rowid
        WHERE s.provider_id = :providerId
          AND series_fts MATCH :query
        ORDER BY s.name ASC
        LIMIT :limit
        """
    )
    fun search(providerId: Long, query: String, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT s.* FROM series s
        WHERE s.provider_id = :providerId
          AND (
              LOWER(s.name) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(s.genre, '')) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(s.category_name, '')) LIKE LOWER(:queryLike) ESCAPE '\'
          )
        ORDER BY s.name ASC
        LIMIT :limit
        """
    )
    fun searchFallback(providerId: Long, queryLike: String, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT s.* FROM series s
        JOIN series_fts ON s.id = series_fts.rowid
        WHERE s.provider_id = :providerId
          AND s.category_id = :categoryId
          AND series_fts MATCH :query
        ORDER BY s.name ASC
        LIMIT :limit
        """
    )
    fun searchByCategory(providerId: Long, categoryId: Long, query: String, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query(
        """
        SELECT s.* FROM series s
        WHERE s.provider_id = :providerId
          AND s.category_id = :categoryId
          AND (
              LOWER(s.name) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(s.genre, '')) LIKE LOWER(:queryLike) ESCAPE '\'
              OR LOWER(COALESCE(s.category_name, '')) LIKE LOWER(:queryLike) ESCAPE '\'
          )
        ORDER BY s.name ASC
        LIMIT :limit
        """
    )
    fun searchByCategoryFallback(providerId: Long, categoryId: Long, queryLike: String, limit: Int): Flow<List<SeriesBrowseEntity>>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getById(id: Long): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(series: SeriesEntity): Long

    @Query("SELECT * FROM series WHERE provider_id = :providerId")
    suspend fun getByProviderSync(providerId: Long): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND tmdb_id = :tmdbId")
    suspend fun getByProviderAndTmdbIdSync(providerId: Long, tmdbId: Long): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND release_date LIKE :yearPrefix")
    suspend fun getByProviderAndReleaseYearPrefixSync(providerId: Long, yearPrefix: String): List<SeriesEntity>

    @Query("SELECT tmdb_id FROM series WHERE provider_id = :providerId AND tmdb_id IS NOT NULL")
    suspend fun getTmdbIdsByProvider(providerId: Long): List<TmdbIdMapping>

    @Query("SELECT * FROM series WHERE id IN (:ids)")
    fun getByIds(ids: List<Long>): Flow<List<SeriesBrowseEntity>>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND series_id = :seriesId LIMIT 1")
    suspend fun getBySeriesId(providerId: Long, seriesId: Long): SeriesEntity?

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND series_id IN (:seriesIds)")
    suspend fun getBySeriesIds(providerId: Long, seriesIds: List<Long>): List<SeriesEntity>

    @Query("SELECT COUNT(*) FROM series WHERE provider_id = :providerId AND category_id = :categoryId AND catalog_origin = :origin")
    suspend fun countByCategoryAndOrigin(
        providerId: Long,
        categoryId: Long,
        origin: com.streamvault.domain.model.SeriesCatalogOrigin
    ): Int

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND series_id IN (:seriesIds)")
    fun observeBySeriesIds(providerId: Long, seriesIds: List<Long>): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE provider_id = :providerId AND provider_series_id = :providerSeriesId LIMIT 1")
    suspend fun getByProviderSeriesId(providerId: Long, providerSeriesId: String): SeriesEntity?

    // REPLACE deletes the existing parent row before inserting it again. Because
    // episodes reference series with ON DELETE CASCADE, a catalog-page refresh
    // could silently erase already-hydrated episodes. UPSERT updates matched
    // primary keys in place and keeps those children intact.
    @Upsert
    suspend fun insertAll(series: List<SeriesEntity>)

    @Update
    suspend fun update(series: SeriesEntity)

    @Update
    suspend fun updateAll(series: List<SeriesEntity>)

    @Query(
        """
        SELECT id, COALESCE(NULLIF(provider_series_id, ''), CAST(series_id AS TEXT)) AS remote_id
        FROM series
        WHERE provider_id = :providerId
        """
    )
    suspend fun getIdMappings(providerId: Long): List<SeriesRemoteIdMapping>

    @Query(
        """
        SELECT id, COALESCE(NULLIF(provider_series_id, ''), CAST(series_id AS TEXT)) AS remote_id
        FROM series
        WHERE provider_id = :providerId AND category_id = :categoryId
        """
    )
    suspend fun getIdMappingsByCategory(providerId: Long, categoryId: Long): List<SeriesRemoteIdMapping>

    @Query("SELECT COUNT(*) FROM series WHERE provider_id = :providerId")
    suspend fun countByProvider(providerId: Long): Int

    @Query("DELETE FROM series WHERE provider_id = :providerId")
    suspend fun deleteByProvider(providerId: Long)

    @Query("DELETE FROM series WHERE provider_id = :providerId AND category_id = :categoryId")
    suspend fun deleteByProviderAndCategory(providerId: Long, categoryId: Long)

    @Query("DELETE FROM series WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM series WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    suspend fun replaceAll(providerId: Long, series: List<SeriesEntity>) {
        val existingByRemoteId = getIdMappings(providerId).associate { it.remoteId to it.id }
        fun SeriesEntity.remoteKey(): String = providerSeriesId?.takeIf { it.isNotBlank() } ?: seriesId.toString()
        val remapped = series
            .distinctBy { it.remoteKey() }
            .map { entity -> entity.copy(id = existingByRemoteId[entity.remoteKey()] ?: 0L) }
        deleteByProvider(providerId)
        insertAll(remapped)
    }

    @Query(
        """
        DELETE FROM series
        WHERE provider_id = :providerId
          AND category_id = :categoryId
          AND COALESCE(NULLIF(provider_series_id, ''), CAST(series_id AS TEXT)) NOT IN (:remoteIds)
        """
    )
    suspend fun deleteMissingByCategory(providerId: Long, categoryId: Long, remoteIds: List<String>)

    @Transaction
    suspend fun replaceCategory(providerId: Long, categoryId: Long, series: List<SeriesEntity>) {
        val existingByRemoteId = getIdMappingsByCategory(providerId, categoryId).associate { it.remoteId to it.id }
        fun SeriesEntity.remoteKey(): String = providerSeriesId?.takeIf { it.isNotBlank() } ?: seriesId.toString()
        val remapped = series
            .distinctBy { it.remoteKey() }
            .map { entity -> entity.copy(id = existingByRemoteId[entity.remoteKey()] ?: 0L) }

        if (remapped.isEmpty()) {
            deleteByProviderAndCategory(providerId, categoryId)
        } else {
            insertAll(remapped)
            deleteMissingByCategory(providerId, categoryId, remapped.map { it.remoteKey() })
        }
    }

    @Transaction
    suspend fun upsertCategoryPage(providerId: Long, series: List<SeriesEntity>) {
        if (series.isEmpty()) return
        val existing = getByProviderSync(providerId)
        val existingByRemoteId = existing.associateBy {
            it.providerSeriesId?.takeIf(String::isNotBlank) ?: it.seriesId.toString()
        }
        fun SeriesEntity.remoteKey(): String = providerSeriesId?.takeIf { it.isNotBlank() } ?: seriesId.toString()
        val remapped = series
            .distinctBy { it.remoteKey() }
            .map { incoming ->
                val current = existingByRemoteId[incoming.remoteKey()]
                when {
                    current == null -> incoming
                    current.catalogOrigin == com.streamvault.domain.model.SeriesCatalogOrigin.NATIVE &&
                        incoming.catalogOrigin == com.streamvault.domain.model.SeriesCatalogOrigin.VOD_DERIVED ->
                        current.copy(
                            episodePlaybackTemplateUrl = current.episodePlaybackTemplateUrl
                                ?: incoming.episodePlaybackTemplateUrl
                        )
                    else -> {
                        val preserveDetails = current.cacheState == "DETAIL_HYDRATED" &&
                            current.detailHydratedAt > 0L
                        incoming.copy(
                            id = current.id,
                            backdropUrl = if (preserveDetails) current.backdropUrl else incoming.backdropUrl,
                            plot = if (preserveDetails) current.plot else incoming.plot,
                            cast = if (preserveDetails) current.cast else incoming.cast,
                            director = if (preserveDetails) current.director else incoming.director,
                            releaseDate = if (preserveDetails) current.releaseDate else incoming.releaseDate,
                            tmdbId = incoming.tmdbId ?: current.tmdbId,
                            youtubeTrailer = incoming.youtubeTrailer ?: current.youtubeTrailer,
                            episodeRunTime = if (preserveDetails) current.episodeRunTime else incoming.episodeRunTime,
                            isUserProtected = current.isUserProtected,
                            cacheState = if (preserveDetails) current.cacheState else incoming.cacheState,
                            detailHydratedAt = if (preserveDetails) current.detailHydratedAt else incoming.detailHydratedAt,
                            remoteStaleAt = if (preserveDetails) current.remoteStaleAt else incoming.remoteStaleAt,
                            episodePlaybackTemplateUrl = incoming.episodePlaybackTemplateUrl
                                ?: current.episodePlaybackTemplateUrl
                        )
                    }
                }
            }
        insertAll(remapped)
    }

    @Query("SELECT category_id, COUNT(*) as item_count FROM series WHERE provider_id = :providerId AND category_id IS NOT NULL GROUP BY category_id")
    fun getCategoryCounts(providerId: Long): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM series WHERE provider_id = :providerId")
    fun getCount(providerId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM series WHERE provider_id = :providerId AND category_id = :categoryId")
    fun getCountByCategory(providerId: Long, categoryId: Long): Flow<Int>

    @Query("UPDATE series SET is_user_protected = :isProtected WHERE provider_id = :providerId AND category_id = :categoryId")
    suspend fun updateProtectionStatus(providerId: Long, categoryId: Long, isProtected: Boolean)

    @Query("UPDATE series SET is_user_protected = :isProtected WHERE id = :id AND provider_id = :providerId")
    suspend fun updateItemProtection(id: Long, providerId: Long, isProtected: Boolean): Int

    @Query("UPDATE series SET is_user_protected = 0 WHERE provider_id = :providerId AND category_id IN (:categoryIds)")
    suspend fun clearProtectionForCategories(providerId: Long, categoryIds: List<Long>)

    @Query("UPDATE series SET is_user_protected = 0 WHERE provider_id = :providerId")
    suspend fun clearItemProtectionByProvider(providerId: Long)

    @Query("UPDATE series SET is_user_protected = CASE WHEN category_id IN (SELECT category_id FROM categories WHERE provider_id = :providerId AND type IN ('SERIES', 'VOD') AND is_user_protected = 1) THEN 1 ELSE 0 END WHERE provider_id = :providerId")
    suspend fun resetProtectionToCategoryState(providerId: Long)
}

@Dao
@RewriteQueriesToDropUnusedColumns
interface EpisodeDao {
    @Query("UPDATE episodes SET is_user_protected = :isProtected WHERE id = :id AND provider_id = :providerId")
    suspend fun updateItemProtection(id: Long, providerId: Long, isProtected: Boolean): Int

    @Query("UPDATE episodes SET is_user_protected = 0 WHERE provider_id = :providerId")
    suspend fun clearItemProtectionByProvider(providerId: Long)

    @Query("UPDATE episodes SET is_user_protected = CASE WHEN series_id IN (SELECT id FROM series WHERE provider_id = :providerId AND is_user_protected = 1) THEN 1 ELSE 0 END WHERE provider_id = :providerId")
    suspend fun resetProtectionToCategoryState(providerId: Long)

    @Query("SELECT * FROM episodes WHERE series_id = :seriesId ORDER BY season_number ASC, episode_number ASC")
    fun getBySeries(seriesId: Long): Flow<List<EpisodeBrowseEntity>>

    @Query("SELECT * FROM episodes WHERE series_id = :seriesId ORDER BY season_number ASC, episode_number ASC")
    suspend fun getBySeriesSync(seriesId: Long): List<EpisodeBrowseEntity>

    @Query("SELECT * FROM episodes WHERE series_id = :seriesId ORDER BY season_number ASC, episode_number ASC")
    suspend fun getEntitiesBySeriesSync(seriesId: Long): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE provider_id = :providerId AND episode_id = :episodeId LIMIT 1")
    suspend fun getByProviderAndEpisodeId(providerId: Long, episodeId: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE provider_id = :providerId")
    suspend fun getByProviderSync(providerId: Long): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(episode: EpisodeEntity): Long

    @Query(
        "SELECT * FROM episodes WHERE provider_id = :providerId AND series_id = :seriesId AND episode_id = :episodeId LIMIT 1"
    )
    suspend fun getByProviderSeriesAndEpisodeId(providerId: Long, seriesId: Long, episodeId: Long): EpisodeEntity?

    @Query(
        """
        SELECT COUNT(*)
        FROM episodes
        LEFT JOIN playback_history
            ON playback_history.content_id = episodes.id
           AND playback_history.content_type = 'SERIES_EPISODE'
           AND playback_history.provider_id = episodes.provider_id
        WHERE episodes.provider_id = :providerId
          AND episodes.series_id = :seriesId
          AND (
              COALESCE(playback_history.total_duration_ms, episodes.duration_seconds * 1000) <= 0
              OR COALESCE(playback_history.resume_position_ms, episodes.watch_progress) < CAST(
                  COALESCE(playback_history.total_duration_ms, episodes.duration_seconds * 1000) * :completionThreshold
                  AS INTEGER
              )
          )
        """
    )
    fun getUnwatchedCount(providerId: Long, seriesId: Long, completionThreshold: Float): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT id, episode_id AS remote_id FROM episodes WHERE provider_id = :providerId AND series_id = :seriesId")
    suspend fun getIdMappings(providerId: Long, seriesId: Long): List<RemoteIdMapping>

    @Query(
        """
        UPDATE episodes
        SET watch_progress = COALESCE((
            SELECT resume_position_ms FROM playback_history
            WHERE playback_history.content_id = episodes.id
              AND playback_history.content_type = 'SERIES_EPISODE'
              AND playback_history.provider_id = episodes.provider_id
        ), 0),
            last_watched_at = COALESCE((
                SELECT last_watched_at FROM playback_history
                WHERE playback_history.content_id = episodes.id
                  AND playback_history.content_type = 'SERIES_EPISODE'
                  AND playback_history.provider_id = episodes.provider_id
            ), 0)
        WHERE id = :id AND provider_id = :providerId
        """
    )
    suspend fun syncWatchProgressFromHistory(id: Long, providerId: Long)

    @Query(
        """
        UPDATE episodes
        SET watch_progress = COALESCE((
            SELECT resume_position_ms FROM playback_history
            WHERE playback_history.content_id = episodes.id
              AND playback_history.content_type = 'SERIES_EPISODE'
              AND playback_history.provider_id = episodes.provider_id
        ), 0),
            last_watched_at = COALESCE((
                SELECT last_watched_at FROM playback_history
                WHERE playback_history.content_id = episodes.id
                  AND playback_history.content_type = 'SERIES_EPISODE'
                  AND playback_history.provider_id = episodes.provider_id
            ), 0)
        WHERE provider_id = :providerId
        """
    )
    suspend fun syncWatchProgressFromHistoryByProvider(providerId: Long)

    @Query(
        """
        UPDATE episodes
        SET watch_progress = COALESCE((
            SELECT resume_position_ms FROM playback_history
            WHERE playback_history.content_id = episodes.id
              AND playback_history.content_type = 'SERIES_EPISODE'
              AND playback_history.provider_id = episodes.provider_id
        ), 0),
            last_watched_at = COALESCE((
                SELECT last_watched_at FROM playback_history
                WHERE playback_history.content_id = episodes.id
                  AND playback_history.content_type = 'SERIES_EPISODE'
                  AND playback_history.provider_id = episodes.provider_id
            ), 0)
        """
    )
    suspend fun syncAllWatchProgressFromHistory()

    @Query("UPDATE episodes SET watch_progress = 0, last_watched_at = 0")
    suspend fun resetAllWatchProgress()

    @Query("DELETE FROM episodes WHERE series_id = :seriesId")
    suspend fun deleteBySeries(seriesId: Long)

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM episodes WHERE series_id NOT IN (SELECT id FROM series)")
    suspend fun deleteOrphans(): Int

    @Query("""
        UPDATE episodes
        SET watch_progress = (
            SELECT resume_position_ms FROM playback_history
            WHERE playback_history.content_id = episodes.id
            AND playback_history.content_type = 'SERIES_EPISODE'
            AND playback_history.provider_id = episodes.provider_id
        )
        WHERE series_id = :seriesId AND EXISTS (
            SELECT 1 FROM playback_history
            WHERE playback_history.content_id = episodes.id
            AND playback_history.content_type = 'SERIES_EPISODE'
            AND playback_history.provider_id = episodes.provider_id
        )
    """)
    suspend fun restoreWatchProgress(seriesId: Long)

    @Transaction
    suspend fun replaceAll(seriesId: Long, providerId: Long, episodes: List<EpisodeEntity>) {
        val existingByRemoteId = getIdMappings(providerId, seriesId).associate { it.remoteId to it.id }
        val remapped = episodes
            .distinctBy { it.episodeId }
            .map { entity -> entity.copy(id = existingByRemoteId[entity.episodeId] ?: 0L) }
        deleteBySeries(seriesId)
        insertAll(remapped)
        restoreWatchProgress(seriesId)
    }
}

@Dao
interface TmdbIdentityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(identities: List<TmdbIdentityEntity>)

    @Query(
        """
        DELETE FROM tmdb_identity
        WHERE content_type = 'MOVIE'
          AND NOT EXISTS (
              SELECT 1 FROM movies
              WHERE movies.tmdb_id = tmdb_identity.tmdb_id
          )
        """
    )
    suspend fun pruneOrphanedMovieIdentities()

    @Query(
        """
        DELETE FROM tmdb_identity
        WHERE content_type = 'SERIES'
          AND NOT EXISTS (
              SELECT 1 FROM series
              WHERE series.tmdb_id = tmdb_identity.tmdb_id
          )
        """
    )
    suspend fun pruneOrphanedSeriesIdentities()
}

@Dao
abstract class VodCatalogEntryDao {
    @Query("SELECT * FROM vod_catalog_entries WHERE provider_id = :providerId AND category_id = :categoryId ORDER BY raw_page ASC, raw_index ASC")
    abstract fun observeByCategory(providerId: Long, categoryId: Long): Flow<List<VodCatalogEntryEntity>>

    @Query("SELECT COUNT(*) FROM vod_catalog_entries WHERE provider_id = :providerId AND category_id = :categoryId")
    abstract suspend fun countByCategory(providerId: Long, categoryId: Long): Int

    @Query("DELETE FROM vod_catalog_entries WHERE provider_id = :providerId AND category_id = :categoryId AND raw_page = :page")
    abstract suspend fun deletePage(providerId: Long, categoryId: Long, page: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entries: List<VodCatalogEntryEntity>)

    @Transaction
    open suspend fun replacePage(providerId: Long, categoryId: Long, page: Int, entries: List<VodCatalogEntryEntity>) {
        deletePage(providerId, categoryId, page)
        if (entries.isNotEmpty()) insertAll(entries)
    }

    @Query("DELETE FROM vod_catalog_entries WHERE provider_id = :providerId AND category_id = :categoryId")
    abstract suspend fun deleteByCategory(providerId: Long, categoryId: Long)

    @Query("DELETE FROM vod_catalog_entries WHERE provider_id = :providerId")
    abstract suspend fun deleteByProvider(providerId: Long)
}
