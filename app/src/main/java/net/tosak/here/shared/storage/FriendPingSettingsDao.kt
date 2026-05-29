package net.tosak.here.shared.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendPingSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: FriendPingSettingsEntity)

    @Query("SELECT * FROM friend_ping_settings WHERE friendId = :id")
    fun observe(id: String): Flow<FriendPingSettingsEntity?>

    @Query("SELECT * FROM friend_ping_settings WHERE friendId = :id")
    suspend fun get(id: String): FriendPingSettingsEntity?

    @Query("DELETE FROM friend_ping_settings WHERE friendId = :id")
    suspend fun deleteById(id: String)
}
