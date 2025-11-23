package com.project.dba_delatorre_dometita_ramirez_tan

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryHelper {
    private const val TAG = "CloudinaryHelper"

    fun initialize(context: Context) {
        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = "drcseyaoz"
            config["api_key"] = "326813912334829"
            config["api_secret"] = "-TAzMjpWbLX0CVcAMH1OrncQc0c"

            MediaManager.init(context, config)
            Log.d(TAG, "✅ Cloudinary initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize Cloudinary", e)
        }
    }

    suspend fun uploadImage(uri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            Log.d(TAG, "📤 Starting image upload to Cloudinary...")
            Log.d(TAG, "URI: $uri")

            MediaManager.get().upload(uri)
                .option("folder", "products")  // Images will be in "products" folder
                .option("resource_type", "image")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "⏳ Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        Log.d(TAG, "📊 Upload progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String ?: ""
                        Log.d(TAG, "✅ Upload successful!")
                        Log.d(TAG, "🔗 Image URL: $url")
                        continuation.resume(url)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "❌ Upload failed: ${error.description}")
                        continuation.resumeWithException(
                            Exception("Upload failed: ${error.description}")
                        )
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "⚠️ Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                Log.d(TAG, "❌ Upload cancelled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload error", e)
            continuation.resumeWithException(e)
        }
    }
    suspend fun deleteImage(imageUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (imageUrl.isEmpty() || !imageUrl.contains("cloudinary.com")) {
                    Log.w(TAG, "⚠️ Invalid Cloudinary URL, skipping delete")
                    return@withContext false
                }

                Log.d(TAG, "🗑️ Deleting image from Cloudinary...")
                Log.d(TAG, "URL: $imageUrl")

                val publicId = extractPublicId(imageUrl)

                if (publicId.isEmpty()) {
                    Log.e(TAG, "❌ Could not extract public_id from URL")
                    return@withContext false
                }

                Log.d(TAG, "📝 Public ID: $publicId")

                // Call Cloudinary Admin API to delete
                val cloudName = "drcseyaoz"
                val apiKey = "326813912334829"
                val apiSecret = "-TAzMjpWbLX0CVcAMH1OrncQc0c"

                val url = URL("https://api.cloudinary.com/v1_1/$cloudName/resources/image/upload")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "DELETE"
                connection.doOutput = true

                // Create Basic Auth header
                val auth = "$apiKey:$apiSecret"
                val encodedAuth = android.util.Base64.encodeToString(
                    auth.toByteArray(),
                    android.util.Base64.NO_WRAP
                )
                connection.setRequestProperty("Authorization", "Basic $encodedAuth")
                connection.setRequestProperty("Content-Type", "application/json")

                // Send public_ids in request body
                val jsonBody = """{"public_ids":["$publicId"]}"""
                connection.outputStream.use { it.write(jsonBody.toByteArray()) }

                val responseCode = connection.responseCode
                Log.d(TAG, "📡 Response code: $responseCode")

                if (responseCode in 200..299) {
                    Log.d(TAG, "✅ Image deleted successfully!")
                    true
                } else {
                    Log.e(TAG, "❌ Delete failed with code: $responseCode")
                    false
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Delete error", e)
                false
            }
        }
    }

    private fun extractPublicId(imageUrl: String): String {
        return try {
            // Extract the path after /upload/
            val uploadIndex = imageUrl.indexOf("/upload/")
            if (uploadIndex == -1) return ""

            val pathAfterUpload = imageUrl.substring(uploadIndex + 8) // +8 for "/upload/"

            // Remove version prefix if exists (v1234567890/)
            val pathWithoutVersion = if (pathAfterUpload.startsWith("v")) {
                pathAfterUpload.substringAfter("/")
            } else {
                pathAfterUpload
            }

            // Remove file extension
            val publicId = pathWithoutVersion.substringBeforeLast(".")

            Log.d(TAG, "📋 Extracted public_id: $publicId")
            publicId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extracting public_id", e)
            ""
        }
    }
}
