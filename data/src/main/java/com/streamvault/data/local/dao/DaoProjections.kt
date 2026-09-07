package com.streamvault.data.local.dao

import androidx.room.ColumnInfo

data class RemoteIdMapping(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "remote_id") val remoteId: Long
)

data class SeriesRemoteIdMapping(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "remote_id") val remoteId: String
)

data class TmdbIdMapping(
    @ColumnInfo(name = "tmdb_id") val tmdbId: Long
)

data class FavoriteGroupConstraint(
    @ColumnInfo(name = "provider_id") val providerId: Long,
    @ColumnInfo(name = "content_type") val contentType: String
)

data class ProviderEpgSourceWithDetails(
    val id: Long,
    @ColumnInfo(name = "provider_id") val providerId: Long,
    @ColumnInfo(name = "epg_source_id") val epgSourceId: Long,
    val priority: Int,
    val enabled: Boolean,
    @ColumnInfo(name = "epg_source_name") val epgSourceName: String,
    @ColumnInfo(name = "epg_source_url") val epgSourceUrl: String
)

data class EpgResolutionStatRow(
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "match_type") val matchType: String?,
    val cnt: Int
)
