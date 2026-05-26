package net.tosak.here.shared.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DmMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: DmMessageEntity)

    @Query("SELECT * FROM dm_messages WHERE friendId = :friendId ORDER BY sentAt ASC")
    fun observeForFriend(friendId: String): Flow<List<DmMessageEntity>>

    @Query(
        "SELECT * FROM dm_messages m WHERE m.sentAt = " +
            "(SELECT MAX(sentAt) FROM dm_messages WHERE friendId = m.friendId)"
    )
    fun observeLastPerFriend(): Flow<List<DmMessageEntity>>

    @Query("SELECT COUNT(*) FROM dm_messages")
    suspend fun count(): Int
}
