package net.tosak.here.shared.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String,
    val mark: String,
    val location: String,
    val addedAt: Long,
)
