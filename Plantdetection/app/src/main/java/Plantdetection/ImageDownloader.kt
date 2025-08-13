package com.PlantDetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

object ImageDownloader {
    private const val TAG = "ImageDownloader"

    // Image URLs - replace these with your actual image URLs
    private val imageUrls = mapOf(
        "tomato" to "https://gndbmbpyylnmkggdvesd.supabase.co/storage/v1/object/public/model-files/vegetable-images/htomato.jpg",
        "eggplant" to "https://gndbmbpyylnmkggdvesd.supabase.co/storage/v1/object/public/model-files/vegetable-images/heggplant.jpg",
        "okra" to "https://gndbmbpyylnmkggdvesd.supabase.co/storage/v1/object/public/model-files/vegetable-images/hokra.jpg",
        "bitter_gourd" to "https://gndbmbpyylnmkggdvesd.supabase.co/storage/v1/object/public/model-files/vegetable-images/hampalaya.jpg",
        "chili_pepper" to "https://gndbmbpyylnmkggdvesd.supabase.co/storage/v1/object/public/model-files/vegetable-images/hsili.jpg"
    )

    suspend fun loadImageIntoImageView(
        context: Context,
        imageView: ImageView,
        vegetableName: String,
        onError: ((Exception) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val bitmap = downloadAndCacheImage(context, vegetableName)

            withContext(Dispatchers.Main) {
                imageView.setImageBitmap(bitmap)
                Log.d(TAG, "Successfully loaded image for $vegetableName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image for $vegetableName", e)
            withContext(Dispatchers.Main) {
                onError?.invoke(e)
            }
        }
    }

    /**
     * Download image from URL and cache it locally
     */
    private suspend fun downloadAndCacheImage(context: Context, vegetableName: String): Bitmap {
        val imageUrl = imageUrls[vegetableName]
            ?: throw IllegalArgumentException("No URL found for vegetable: $vegetableName")

        val cacheFile = File(context.cacheDir, "images/$vegetableName.png")

        // Check if cached version exists and is recent (within 7 days)
        if (cacheFile.exists() && isFileRecent(cacheFile, 7)) {
            Log.d(TAG, "Loading cached image for $vegetableName")
            return BitmapFactory.decodeFile(cacheFile.absolutePath)
        }

        // Download new image
        Log.d(TAG, "Downloading image for $vegetableName from $imageUrl")
        val bitmap = downloadImageFromUrl(imageUrl)

        // Cache the image
        cacheImageToFile(bitmap, cacheFile)

        return bitmap
    }

    /**
     * Download image from URL
     */
    private suspend fun downloadImageFromUrl(urlString: String): Bitmap = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
            }

            connection.inputStream.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
                    ?: throw IOException("Failed to decode image from URL: $urlString")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Cache bitmap to file
     */
    private suspend fun cacheImageToFile(bitmap: Bitmap, file: File) = withContext(Dispatchers.IO) {
        try {
            // Create directory if it doesn't exist
            file.parentFile?.mkdirs()

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            Log.d(TAG, "Cached image to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache image to ${file.absolutePath}", e)
        }
    }

    /**
     * Check if file is recent (within specified days)
     */
    private fun isFileRecent(file: File, days: Int): Boolean {
        val daysInMillis = days * 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - file.lastModified()) < daysInMillis
    }

    /**
     * Load all vegetable images
     */
    suspend fun loadAllVegetableImages(
        context: Context,
        imageViews: Map<String, ImageView>,
        onProgress: ((String, Boolean) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        imageViews.forEach { (vegetableName, imageView) ->
            try {
                loadImageIntoImageView(context, imageView, vegetableName)
                onProgress?.invoke(vegetableName, true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image for $vegetableName", e)
                onProgress?.invoke(vegetableName, false)
            }
        }
    }

    /**
     * Clear image cache
     */
    fun clearImageCache(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "images")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                Log.d(TAG, "Cleared image cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear image cache", e)
        }
    }

    /**
     * Set default drawable images for all vegetables
     */
    fun setDefaultImages(imageViews: Map<String, ImageView>) {
        val defaultDrawables: Map<String, Int> = mapOf(
            "tomato" to R.drawable.tomato,
            "eggplant" to R.drawable.eggplant,
            "okra" to R.drawable.okra,
            "bitter_gourd" to R.drawable.bitter_gourd,
            "chili_pepper" to R.drawable.chili_pepper
        )

        imageViews.forEach { (vegetableName: String, imageView: ImageView) ->
            defaultDrawables[vegetableName]?.let { drawableId: Int ->
                imageView.setImageResource(drawableId)
                Log.d(TAG, "Set default image for $vegetableName")
            }
        }
    }
}