package net.tosak.here.shared.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.tosak.here.shared.model.PostKind
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(private val dao: PostDao) {

    /**
     * Live list of posts that have not yet expired.
     * Re-evaluated on every table change; expired entries are filtered out
     * in-process so a background prune isn't strictly required.
     */
    val activePosts: Flow<List<PostEntity>> = dao.observeAll()
        .map { all -> all.filter { it.expiresAt > System.currentTimeMillis() } }

    suspend fun savePost(
        kind: PostKind,
        caption: String,
        imagePath: String?,
        lat: Double,
        lng: Double,
    ) {
        val now = System.currentTimeMillis()
        dao.insert(
            PostEntity(
                id        = UUID.randomUUID().toString(),
                kind      = kind.name,
                caption   = caption,
                imagePath = imagePath,
                lat       = lat,
                lng       = lng,
                createdAt = now,
                expiresAt = now + 2 * 60 * 60 * 1_000L,   // 2 h
            )
        )
    }

    suspend fun deletePost(id: String) = dao.deleteById(id)

    /** Removes rows whose expiry has already passed. Call on app start to keep the DB tidy. */
    suspend fun pruneExpired() = dao.pruneExpired(System.currentTimeMillis())
}
