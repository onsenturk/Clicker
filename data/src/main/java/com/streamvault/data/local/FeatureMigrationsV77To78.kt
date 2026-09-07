package com.streamvault.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object FeatureMigrationsV77To78 {
    val MIGRATION_77_78 = object : Migration(77, 78) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_series_provider_id_last_modified_name_id")
            db.execSQL(
                "CREATE INDEX index_series_provider_id_last_modified_name_id " +
                    "ON series(provider_id ASC, last_modified DESC, name ASC, id ASC)"
            )
        }
    }
}