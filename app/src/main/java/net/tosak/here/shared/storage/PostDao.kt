package net.tosak.here.shared.storage

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    /** Emits the full list whenever the table changes; expiry filtering is done in the repo. */
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PostEntity>>

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM posts WHERE expiresAt <= :now")
    suspend fun pruneExpired(now: Long)
}
