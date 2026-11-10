package com.hyper.castwave.data.iptv.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hyper.castwave.data.iptv.database.dao.ChannelDao
import com.hyper.castwave.data.iptv.model.TVChannel
import com.hyper.castwave.data.iptv.model.TVChannelGroup
import com.hyper.castwave.data.iptv.model.TVChannelSource

@Database(
    entities = [
        TVChannel::class,
        TVChannelGroup::class,
        TVChannelSource::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TVChannelDatabase : RoomDatabase() {

    companion object {
        const val DATABASE_TV_CHANNEL_NAME = "db_tv_channel"

        /**
         * Migration from version 1 to version 2
         * Adds TVChannelSource table and sourceId columns
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create TVChannelSource table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tb_tv_channel_source (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sourceUrl TEXT NOT NULL,
                        sourceType TEXT NOT NULL,
                        importDate INTEGER NOT NULL,
                        channelCount INTEGER NOT NULL,
                        groupCount INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        lastUpdateTime INTEGER NOT NULL,
                        description TEXT NOT NULL
                    )
                """.trimIndent()
                )

                // Add sourceId column to tb_tv_channel (default 0 for existing records)
                database.execSQL("ALTER TABLE tb_tv_channel ADD COLUMN sourceId INTEGER NOT NULL DEFAULT 0")

                // Add sourceId column to tb_tv_channel_group (default 0 for existing records)
                database.execSQL("ALTER TABLE tb_tv_channel_group ADD COLUMN sourceId INTEGER NOT NULL DEFAULT 0")

                // Create indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tb_tv_channel_sourceId ON tb_tv_channel(sourceId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tb_tv_channel_groupId ON tb_tv_channel(groupId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tb_tv_channel_isFavorite ON tb_tv_channel(isFavorite)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tb_tv_channel_group_sourceId ON tb_tv_channel_group(sourceId)")
            }
        }
    }

    abstract fun channelDao(): ChannelDao
}