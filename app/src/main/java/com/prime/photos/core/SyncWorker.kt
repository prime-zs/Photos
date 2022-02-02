package com.prime.photos.core

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.work.*
import com.prime.photos.common.FileUtils
import com.prime.photos.common.Utils
import com.prime.photos.common.latLong
import com.prime.photos.common.runCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.intellij.lang.annotations.Language
import kotlinx.coroutines.withContext as using


private const val TAG = "SyncWorker"

private suspend inline fun <T> ContentResolver.request(
    uri: Uri,
    projection: Array<String>,
    bridge: (keys: String) -> Long?,
    crossinline transform: (cursor: Cursor) -> T
): List<T>? {

    // check what has been removed for store
    // retrieve all ids from MediaStore
    val keys = kotlin.run {
        //language=SQL
        val proj = arrayOf("GROUP_CONCAT(${MediaStore.MediaColumns._ID}, ''',''')")
        query(uri, proj, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            "'${cursor.getString(0)}'"
        }
    } ?: return null

    // remove deleted and get destDateModified
    val destLastModified = bridge(keys)

    // check what has changed since.
    // get lastModified date from localImageStore.
    // all those files which are above this date are either changed or added newly.
    val sLastModified = kotlin.run {
        //language=SQL
        val proj = arrayOf("MAX(${MediaStore.MediaColumns.DATE_MODIFIED})")
        query(uri, proj, null, null, null)?.use {
            it.moveToFirst()
            it.getLong(0)
        }
    }

    Log.i(TAG, "doWork: $sLastModified, $destLastModified")


    if (sLastModified == null) {
        Log.e(TAG, "doWork: $sLastModified == null")
        return null //error
    }

    if (destLastModified != null && destLastModified == sLastModified) {
        Log.i(TAG, "doWork: cache up-to date, $uri updating not required.")
        return emptyList() // no error
    }

    // get all those changed or newly added files.
    // update or insert them to local images.
    val fromDate = (destLastModified ?: 0)

    val selection = "${MediaStore.MediaColumns.DATE_MODIFIED} > $fromDate"

    return using(Dispatchers.IO) {
        query(uri, projection, selection, null, null)?.use { cursor ->
            List(cursor.count) { index ->
                cursor.moveToPosition(index)
                transform(cursor)
            }
        }
    }
}


//language=SQL
private val PHOTO_PROJECTION
    inline get() = arrayOf(
        MediaStore.Images.ImageColumns._ID, // 0
        MediaStore.Images.ImageColumns.TITLE, // 1
        MediaStore.Images.ImageColumns.DATE_ADDED, // 2
        MediaStore.Images.ImageColumns.DATE_MODIFIED, // 3
        MediaStore.Images.ImageColumns.SIZE, // 4
        MediaStore.Images.ImageColumns.MIME_TYPE, // 5
        MediaStore.Images.ImageColumns.DESCRIPTION, // 6
        MediaStore.Images.ImageColumns.ORIENTATION, // 7
        MediaStore.Images.ImageColumns.HEIGHT,// 8
        MediaStore.Images.ImageColumns.WIDTH, // 9
        MediaStore.Images.ImageColumns.DATA, // 10
        MediaStore.Images.ImageColumns.DATE_TAKEN // 11
    )

private val VIDEO_PROJECTION
    inline get() = arrayOf(
        MediaStore.Video.VideoColumns._ID, // 0
        MediaStore.Video.VideoColumns.TITLE, // 1
        MediaStore.Video.VideoColumns.DATE_ADDED, // 2
        MediaStore.Video.VideoColumns.DATE_MODIFIED, // 3
        MediaStore.Video.VideoColumns.DATE_TAKEN, // 4
        MediaStore.Video.VideoColumns.SIZE, // 5
        MediaStore.Video.VideoColumns.MIME_TYPE, // 6
        MediaStore.Video.VideoColumns.DESCRIPTION, // 7
        MediaStore.Video.VideoColumns.HEIGHT, // 8
        MediaStore.Video.VideoColumns.WIDTH, // 9
        MediaStore.Video.VideoColumns.DATA, // 10
        MediaStore.Video.VideoColumns.CATEGORY, // 11
        MediaStore.Video.VideoColumns.LANGUAGE, //12
        MediaStore.Video.VideoColumns.TAGS, //13
        MediaStore.Video.VideoColumns.DURATION, // 14
        MediaStore.Video.VideoColumns.ARTIST, //15
        MediaStore.Video.VideoColumns.ALBUM, // 16
        MediaStore.Video.VideoColumns.RESOLUTION, // 17
        //MediaStore.Video.VideoColumns.ORIENTATION, //19
    )

private val Cursor.toPhoto: Photo
    inline get() {
        val path = getString(10)
        val loc = runCatching(TAG) { ExifInterface(path).latLong }
        return Photo(
            id = getLong(0),
            title = getString(1),
            dateAdded = getLong(2) * 1000,
            dateModified = getLong(3) * 1000,
            size = getInt(4),
            mimeType = getString(5),
            desc = getString(6),
            orientation = getInt(7),
            height = getInt(8),
            width = getInt(9),
            path = path,
            parent = FileUtils.parent(path),
            dateTaken = getLong(11) * 1000,
            longitude = loc?.get(1) ?: 0.0,
            latitude = loc?.get(0) ?: 0.0
        )
    }

private fun Video(cursor: Cursor, retriever: MediaMetadataRetriever): Video {
    val path = cursor.getString(10)

    var loc: DoubleArray? = null
    var year: Int? = null
    var orientation: Int? = null

    runCatching(TAG) {
        retriever.setDataSource(path)
        loc = retriever.latLong
        year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toInt()
        orientation =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt()
    }

    return with(cursor) {
        Video(
            id = getLong(0),
            title = getString(1),
            dateAdded = getLong(2) * 1000,
            dateModified = getLong(3) * 1000,
            dateTaken = getLong(4) * 1000,
            size = getInt(5),
            mimeType = getString(6),
            desc = getString(7),
            category = getString(11) ?: MediaStore.UNKNOWN_STRING,
            language = getString(12) ?: MediaStore.UNKNOWN_STRING,
            tags = getString(13),
            duration = getInt(14),
            orientation = orientation ?: 0,
            height = getInt(8),
            width = getInt(9),
            path = getString(10),
            parent = FileUtils.parent(path),
            latitude = loc?.get(0) ?: 0.0,
            longitude = loc?.get(1) ?: 0.0,
            artist = getString(15),
            album = getString(16),
            resolution = getFloat(17),
            year = year ?: 0
        )
    }
}

class SyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params = params) {

    companion object {

        private const val IMMEDIATE_UPDATE_WORK = "one_time_work"

        fun checkForUpdates(context: Context) {
            // run on app start up for first time
            val workManager = WorkManager.getInstance(context.applicationContext)
            val work = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().build())
                .build()
            workManager.enqueueUniqueWork(
                IMMEDIATE_UPDATE_WORK,
                ExistingWorkPolicy.KEEP,
                work
            )
        }
    }


    override suspend fun doWork(): Result {
        return using(Dispatchers.IO) {
            val resolver = context.contentResolver
            // async
            val work1 = async {
                val db = GetPhotosDb(context)
                val list = resolver.request(
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection = PHOTO_PROJECTION,
                    bridge = { keys ->
                        // delete all which are not in keys.
                        val x = db._delete(keys)
                        Log.i(TAG, "doWork: deleted count $x")
                        db.lastModified()?.let { it / 1000 }
                    },
                    transform = { it.toPhoto },
                )
                if (!list.isNullOrEmpty())
                    db.insert(list)
                return@async list != null // means error
            }
            val work2 = async {
                val db = GetVideosDb(context)
                val retriever = MediaMetadataRetriever()
                val list = resolver.request(
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection = VIDEO_PROJECTION,
                    bridge = { keys ->
                        // delete all which are not in keys.
                        val x = db._delete(keys)
                        Log.i(TAG, "doWork: deleted count $x")
                        db.lastModified()?.let { it / 1000 }
                    },
                    transform = { Video(cursor = it, retriever) },
                )
                if (!list.isNullOrEmpty())
                    db.insert(list)
                return@async list != null
            }
            // check states
            val state1 = work1.await()
            val state2 = work2.await()
            if (state1 && state2) Result.success()
            else Result.failure()
        }
    }
}