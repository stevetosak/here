package net.tosak.here.shared.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dm_messages",
    indices = [Index("friendId"), Index("sentAt")],
)
data class DmMessageEntity(
    @PrimaryKey val id: String,
    val friendId: String,
    val fromMe: Boolean,
    val text: String?,
    val imagePath: String?,
    val sentAt: Long,
)
