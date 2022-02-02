package com.prime.photos.core

import android.content.Context
import androidx.room.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Singleton

@ActivityScoped
class Repository(@ApplicationContext context: Context) {


    private val videosDb = GetVideosDb(context)
    private val photosDb = GetPhotosDb(context)

    init {
        // launch on every time app is launched.
        SyncWorker.checkForUpdates(context)
        // TODO: add logic for auto-update
    }

    fun videos(query: String? = null) = videosDb.stream(query)

    fun photos(query: String? = null) = photosDb.stream(query)

    fun videoBuckets(query: String? = null) = videosDb.buckets(query)

    fun photoBuckets(query: String? = null) = photosDb.buckets(query)

    fun albums(query: String? = null) = { TODO() }

    val videoInfo = videosDb.info()

    val photosInfo = photosDb.info()

}