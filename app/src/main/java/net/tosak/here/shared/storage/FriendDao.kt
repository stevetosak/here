package net.tosak.here.shared.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(friend: FriendEntity)

    @Query("SELECT * FROM friends ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FriendEntity>>
}
