package net.tosak.here.shared.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val dao: DmMessageDao,
    @ApplicationContext private val context: Context,
) {

    fun messages(friendId: String): Flow<List<DmMessageEntity>> =
        dao.observeForFriend(friendId)

    val lastPerFriend: Flow<List<DmMessageEntity>> = dao.observeLastPerFriend()

    suspend fun sendText(friendId: String, text: String) {
        dao.insert(
            DmMessageEntity(
                id = UUID.randomUUID().toString(),
                friendId = friendId,
                fromMe = true,
                text = text,
                imagePath = null,
                sentAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun sendImage(friendId: String, sourceUri: Uri) {
        val path = copyUriToInternal(sourceUri) ?: return
        dao.insert(
            DmMessageEntity(
                id = UUID.randomUUID().toString(),
                friendId = friendId,
                fromMe = true,
                text = null,
                imagePath = path,
                sentAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun receiveText(friendId: String, text: String) {
        dao.insert(
            DmMessageEntity(
                id = UUID.randomUUID().toString(),
                friendId = friendId,
                fromMe = false,
                text = text,
                imagePath = null,
                sentAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val hour = 60 * 60 * 1000L

        val seeds = listOf(
            Triple("alex", "yo, you around?", now - 3 * day),
            Triple("alex", "ye just got back. coffee?", now - 3 * day + 8 * 60 * 1000L),
            Triple("alex", "vinoteka in 20", now - 2 * day),
            Triple("kris", "lol that was a night", now - 5 * day),
            Triple("kris", "did you see noa's post?", now - 1 * day - 4 * hour),
            Triple("mira", "thanks for yesterday", now - 6 * hour),
            Triple("mira", "anytime", now - 6 * hour + 90_000L),
            Triple("noa", "debarca tomorrow?", now - 12 * hour),
        )

        // Alternate fromMe to look like a real back-and-forth.
        seeds.forEachIndexed { i, (friendId, text, ts) ->
            dao.insert(
                DmMessageEntity(
                    id = UUID.randomUUID().toString(),
                    friendId = friendId,
                    fromMe = i % 2 == 1,
                    text = text,
                    imagePath = null,
                    sentAt = ts,
                )
            )
        }
    }

    private suspend fun copyUriToInternal(uri: Uri): String? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "chat_images").apply { mkdirs() }
        val file = File(dir, "chat_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            file.absolutePath
        }.getOrNull()
    }
}
