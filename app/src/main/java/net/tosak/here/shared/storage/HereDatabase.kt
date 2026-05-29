package net.tosak.here.shared.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities    = [PostEntity::class, DmMessageEntity::class, FriendEntity::class, FriendPingSettingsEntity::class],
    version     = 4,
    exportSchema = false,
)
abstract class HereDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun dmMessageDao(): DmMessageDao
    abstract fun friendDao(): FriendDao
    abstract fun friendPingSettingsDao(): FriendPingSettingsDao
}
