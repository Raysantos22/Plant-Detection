package com.PlantDetection

import android.content.Context
import android.util.Log
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Duration.Companion.hours

object SupabaseModelDownloader {
    private const val BUCKET_NAME = "model-files"
    private const val MODEL_FILE_NAME = "yolov11v8.tflite"
    private const val LABELS_FILE_NAME = "labels1.txt"
    private const val TAG = "SupabaseDownloader"

    suspend fun downloadModelFiles(
        context: Context,
        onProgress: (Int, String) -> Unit = { _, _ -> },
        onComplete: (String, String) -> Unit,
        onError: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, MODEL_FILE_NAME)
            val labelsFile = File(context.filesDir, LABELS_FILE_NAME)

            // Check if files already exist and are recent (optional)
            if (areFilesUpToDate(modelFile, labelsFile)) {
                withContext(Dispatchers.Main) {
                    onComplete(modelFile.absolutePath, labelsFile.absolutePath)
                }
                return@withContext
            }

            withContext(Dispatchers.Main) {
                onProgress(0, "Starting download...")
            }

            // Get download URLs from Supabase
            val modelUrl = getDownloadUrl(MODEL_FILE_NAME)
            val labelsUrl = getDownloadUrl(LABELS_FILE_NAME)

            withContext(Dispatchers.Main) {
                onProgress(10, "Got download URLs...")
            }

            // Download model file
            withContext(Dispatchers.Main) {
                onProgress(20, "Downloading model file...")
            }
            downloadFileFromUrl(modelUrl, modelFile) { progress ->
                val adjustedProgress = 20 + (progress * 0.6).toInt() // 20-80%
                runBlocking {
                    withContext(Dispatchers.Main) {
                        onProgress(adjustedProgress, "Downloading model... $progress%")
                    }
                }
            }

            // Verify model file was downloaded correctly
            if (!modelFile.exists() || modelFile.length() == 0L) {
                throw IOException("Model file download failed or file is empty")
            }

            // Download labels file
            withContext(Dispatchers.Main) {
                onProgress(80, "Downloading labels file...")
            }
            downloadFileFromUrl(labelsUrl, labelsFile) { progress ->
                val adjustedProgress = 80 + (progress * 0.15).toInt() // 80-95%
                runBlocking {
                    withContext(Dispatchers.Main) {
                        onProgress(adjustedProgress, "Downloading labels... $progress%")
                    }
                }
            }

            // Verify labels file was downloaded correctly
            if (!labelsFile.exists() || labelsFile.length() == 0L) {
                throw IOException("Labels file download failed or file is empty")
            }

            withContext(Dispatchers.Main) {
                onProgress(100, "Download complete!")
                onComplete(modelFile.absolutePath, labelsFile.absolutePath)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }

    private suspend fun getDownloadUrl(fileName: String): String {
        // Use public URL directly (make sure your bucket is public)
//        return "https://snsfnvculzdquloevznj.supabase.co/storage/v1/object/public/$BUCKET_NAME/$fileName"
        return "https://gndbmbpyylnmkggdvesd.supabase.co/storage/v1/object/public/$BUCKET_NAME/$fileName"

    }
    private suspend fun downloadFileFromUrl(
        urlString: String,
        file: File,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Downloading from: $urlString")
        Log.d(TAG, "Downloading to: ${file.absolutePath}")

        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
            }

            val fileLength = connection.contentLength
            Log.d(TAG, "File size: $fileLength bytes")

            // Ensure parent directory exists
            file.parentFile?.mkdirs()

            FileOutputStream(file).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8192) // 8KB buffer
                    var total = 0
                    var count: Int

                    while (input.read(buffer).also { count = it } != -1) {
                        total += count
                        output.write(buffer, 0, count)

                        if (fileLength > 0) {
                            val progress = (total * 100 / fileLength).coerceIn(0, 100)
                            onProgress(progress)
                        }
                    }

                    // Force flush and sync
                    output.flush()
                    output.fd.sync()
                }
            }

            Log.d(TAG, "Successfully downloaded ${file.name}")
            Log.d(TAG, "File size after download: ${file.length()} bytes")
            Log.d(TAG, "File exists: ${file.exists()}")

        } finally {
            connection.disconnect()
        }
    }

    private fun areFilesUpToDate(modelFile: File, labelsFile: File): Boolean {
        // Check if files exist and were modified within the last 24 hours
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return modelFile.exists() && labelsFile.exists() &&
                modelFile.lastModified() > oneDayAgo &&
                labelsFile.lastModified() > oneDayAgo &&
                modelFile.length() > 0 && // Ensure files are not empty
                labelsFile.length() > 0
    }

    // Method to force re-download (useful for testing)
    suspend fun forceDownload(
        context: Context,
        onProgress: (Int, String) -> Unit = { _, _ -> },
        onComplete: (String, String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Delete existing files
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)
        val labelsFile = File(context.filesDir, LABELS_FILE_NAME)
        modelFile.delete()
        labelsFile.delete()

        // Download fresh copies
        downloadModelFiles(context, onProgress, onComplete, onError)
    }
}