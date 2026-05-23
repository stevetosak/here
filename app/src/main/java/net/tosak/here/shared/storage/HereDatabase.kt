package net.tosak.here.shared.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities    = [PostEntity::class],
    version     = 1,
    exportSchema = false,
)
abstract class HereDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}
