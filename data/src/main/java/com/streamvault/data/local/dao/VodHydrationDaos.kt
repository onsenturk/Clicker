package com.streamvault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.streamvault.data.local.entity.MovieCategoryHydrationEntity
import com.streamvault.data.local.entity.SeriesCategoryHydrationEntity
import com.streamvault.data.local.entity.VodCategoryHydrationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieCategoryHydrationDao {
    @Query("SELECT * FROM movie_category_hydration WHERE provider_id = :providerId AND category_id = :categoryId")
    suspend fun get(providerId: Long, categoryId: Long): MovieCategoryHydrationEntity?

    @Query("SELECT * FROM movie_category_hydration WHERE provider_id = :providerId AND category_id = :categoryId")
    fun observe(providerId: Long, categoryId: Long): Flow<MovieCategoryHydrationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: MovieCategoryHydrationEntity)

    @Query("DELETE FROM movie_category_hydration WHERE provider_id = :providerId AND category_id = :categoryId")
    suspend fun delete(providerId: Long, categoryId: Long)

    @Query("DELETE FROM movie_category_hydration WHERE provider_id = :providerId")
    suspend fun deleteByProvider(providerId: Long)
}

@Dao
interface SeriesCategoryHydrationDao {
    @Query("SELECT * FROM series_category_hydration WHERE provider_id = :providerId AND category_id = :categoryId")
    suspend fun get(providerId: Long, categoryId: Long): SeriesCategoryHydrationEntity?

    @Query("SELECT * FROM series_category_hydration WHERE provider_id = :providerId AND category_id = :categoryId")
    fun observe(providerId: Long, categoryId: Long): Flow<SeriesCategoryHydrationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SeriesCategoryHydrationEntity)

    @Query("DELETE FROM series_category_hydration WHERE provider_id = :providerId AND category_id = :categoryId")
    suspend fun delete(providerId: Long, categoryId: Long)

    @Query("DELETE FROM series_category_hydration WHERE provider_id = :providerId")
    suspend fun deleteByProvider(providerId: Long)
}

@Dao
interface VodCategoryHydrationDao {
    @Query("SELECT * FROM vod_category_hydration WHERE provider_id = :providerId AND category_id = :categoryId")
    suspend fun get(providerId: Long, categoryId: Long): VodCategoryHydrationEntity?

    @Query("SELECT * FROM vod_category_hydration WHERE provider_id = :providerId AND category_id = :categoryId")
    fun observe(providerId: Long, categoryId: Long): Flow<VodCategoryHydrationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: VodCategoryHydrationEntity)

    @Query("DELETE FROM vod_category_hydration WHERE provider_id = :providerId")
    suspend fun deleteByProvider(providerId: Long)
}
