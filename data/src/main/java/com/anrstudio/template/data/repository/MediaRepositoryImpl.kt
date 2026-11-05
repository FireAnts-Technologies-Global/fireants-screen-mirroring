package com.anrstudio.template.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.model.MediaItem
import com.anrstudio.template.domain.model.MediaType
import com.anrstudio.template.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaRepository {

    override suspend fun getPhotos(): DataResult<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching { queryImages() }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Error(it, "Failed to load photos") },
        )
    }

    override suspend fun getVideos(): DataResult<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching { queryVideos() }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Error(it, "Failed to load videos") },
        )
    }

    override suspend fun getAudio(): DataResult<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching { queryAudio() }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Error(it, "Failed to load audio") },
        )
    }

    override suspend fun getRecently(): DataResult<List<MediaItem>> =
        DataResult.Success(emptyList())

    override suspend fun getFavorites(): DataResult<List<MediaItem>> =
        DataResult.Success(emptyList())

    override suspend fun getPortrait(): DataResult<List<MediaItem>> =
        DataResult.Success(emptyList())

    override suspend fun toggleFavorite(id: String) {}

    private fun queryImages(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
        )
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id,
                ).toString()
                items += MediaItem(
                    id = id.toString(),
                    name = cursor.getString(nameCol) ?: "",
                    uri = uri,
                    thumbnailUri = uri,
                    type = MediaType.IMAGE,
                    duration = null,
                    dateAdded = cursor.getLong(dateCol),
                    isFavorite = false,
                )
            }
        }
        return items
    }

    private fun queryVideos(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id,
                ).toString()
                items += MediaItem(
                    id = id.toString(),
                    name = cursor.getString(nameCol) ?: "",
                    uri = uri,
                    thumbnailUri = uri,
                    type = MediaType.VIDEO,
                    duration = cursor.getLong(durationCol).takeIf { it > 0 },
                    dateAdded = cursor.getLong(dateCol),
                    isFavorite = false,
                )
            }
        }
        return items
    }

    private fun queryAudio(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id,
                ).toString()
                items += MediaItem(
                    id = id.toString(),
                    name = cursor.getString(nameCol) ?: "",
                    uri = uri,
                    thumbnailUri = "",
                    type = MediaType.AUDIO,
                    duration = cursor.getLong(durationCol).takeIf { it > 0 },
                    dateAdded = cursor.getLong(dateCol),
                    isFavorite = false,
                )
            }
        }
        return items
    }
}
