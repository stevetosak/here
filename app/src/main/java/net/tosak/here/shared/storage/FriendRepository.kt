package net.tosak.here.shared.storage

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val dao: FriendDao,
) {

    val connectedFriends: Flow<List<FriendEntity>> = dao.observeAll()

    suspend fun addFromHandshake(nickname: String, location: String, timestamp: Long) {
        dao.insert(
            FriendEntity(
                id = nickname,
                mark = nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                location = location,
                addedAt = timestamp,
            )
        )
    }
}
