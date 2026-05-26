package net.tosak.here.shared.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities    = [PostEntity::class, DmMessageEntity::class, FriendEntity::class],
    version     = 3,
    exportSchema = false,
)
abstract class HereDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun dmMessageDao(): DmMessageDao
    abstract fun friendDao(): FriendDao
}
