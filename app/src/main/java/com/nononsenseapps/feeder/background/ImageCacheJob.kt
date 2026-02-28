package com.nononsenseapps.feeder.background

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.blob.blobFile
import com.nononsenseapps.feeder.blob.blobInputStream
import com.nononsenseapps.feeder.db.room.FeedItemDao
import com.nononsenseapps.feeder.util.FilePathProvider
import com.nononsenseapps.feeder.util.findAllImageUrlsInHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.instance

class ImageCacheJob(
    private val context: Context,
    override val params: JobParameters,
) : BackgroundJob,
    DIAware {
    override val di: DI by closestDI(context)

    override val jobId: Int = params.jobId

    private val imageLoader: ImageLoader by instance()
    private val repository: Repository by instance()
    private val feedItemDao: FeedItemDao by instance()
    private val filePathProvider: FilePathProvider by instance()

    override suspend fun doWork() {
        val startTime = System.currentTimeMillis()
        try {
            Log.i(LOG_TAG, "Starting image pre-caching")

            val thumbnailUrls = mutableSetOf<String>()
            val blobUrls = mutableSetOf<String>()

            // 1. Thumbnails and enclosure images from DB
            withContext(Dispatchers.IO) {
                thumbnailUrls.addAll(repository.getAllItemImageUrls())
            }
            Log.i(LOG_TAG, "Found ${thumbnailUrls.size} thumbnail/enclosure URLs")

            // 2. All images embedded in article HTML blobs
            val itemIds = withContext(Dispatchers.IO) {
                feedItemDao.getAllFeedItemIds()
            }

            Log.i(LOG_TAG, "Scanning ${itemIds.size} article blobs for images")

            withContext(Dispatchers.IO) {
                for (id in itemIds) {
                    try {
                        val file = blobFile(id, filePathProvider.articleDir)
                        if (file.isFile) {
                            val html =
                                blobInputStream(id, filePathProvider.articleDir)
                                    .bufferedReader()
                                    .use { it.readText() }
                            blobUrls.addAll(findAllImageUrlsInHtml(html))
                        }
                    } catch (e: Exception) {
                        // Skip items with missing or corrupted blobs
                    }
                }
            }

            val allImageUrls = mutableSetOf<String>()
            allImageUrls.addAll(thumbnailUrls)
            allImageUrls.addAll(blobUrls)
            Log.i(LOG_TAG, "Found ${thumbnailUrls.size} thumbnails, ${blobUrls.size} from blobs = ${allImageUrls.size} unique")

            var successCount = 0
            var failCount = 0

            for (url in allImageUrls) {
                if (!currentCoroutineContext().isActive) {
                    Log.i(LOG_TAG, "Image cache job cancelled")
                    break
                }
                try {
                    val request =
                        ImageRequest
                            .Builder(context)
                            .data(url)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .build()
                    val result = imageLoader.execute(request)
                    if (result is SuccessResult) {
                        successCount++
                        Log.d(LOG_TAG, "Cached: $url (source: ${result.dataSource})")
                    } else {
                        failCount++
                        val throwable = (result as? ErrorResult)?.throwable
                        Log.w(LOG_TAG, "Failed: $url ${throwable?.message ?: ""}")
                    }
                } catch (e: Exception) {
                    failCount++
                    Log.w(LOG_TAG, "Error caching: $url", e)
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.i(LOG_TAG, "Completed: $successCount ok, $failCount failed, ${elapsed}ms elapsed")
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.e(LOG_TAG, "Error during image pre-caching after ${elapsed}ms", e)
        }
    }

    companion object {
        const val LOG_TAG = "FEEDER_IMAGECACHE"
    }
}

fun runOnceImageCache(di: DI) {
    val repository: Repository by di.instance()
    val context: Application by di.instance()
    val jobScheduler: JobScheduler? = context.getSystemService()

    if (jobScheduler == null) {
        Log.e(ImageCacheJob.LOG_TAG, "JobScheduler not available")
        return
    }

    val componentName = ComponentName(context, FeederJobService::class.java)
    val wifiOnly = repository.loadImageOnlyOnWifi.value
    val networkType =
        if (wifiOnly) {
            JobInfo.NETWORK_TYPE_UNMETERED
        } else {
            JobInfo.NETWORK_TYPE_ANY
        }

    Log.i(ImageCacheJob.LOG_TAG, "Scheduling image cache job: networkType=${if (wifiOnly) "UNMETERED" else "ANY"}")

    jobScheduler.schedule(
        JobInfo
            .Builder(BackgroundJobId.IMAGE_CACHE.jobId, componentName)
            .setRequiredNetworkType(networkType)
            .build(),
    )
}
