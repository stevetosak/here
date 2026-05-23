package net.tosak.here.shared.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single user-authored post stored locally until it expires.
 *
 * [kind]      — "PHOTO" or "TEXT" (mirrors PostKind name)
 * [caption]   — body text; for PHOTO this is the optional caption
 * [imagePath] — absolute path inside filesDir; null for TEXT posts
 * [expiresAt] — epoch-ms; posts are hidden/pruned after this time (createdAt + 2 h)
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val caption: String,
    val imagePath: String?,
    val lat: Double,
    val lng: Double,
    val createdAt: Long,
    val expiresAt: Long,
)
