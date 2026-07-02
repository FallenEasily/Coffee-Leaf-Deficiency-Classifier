package com.aiclassifier.data.roboflow

import android.util.Log
import com.aiclassifier.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoboflowRepository {

    private val TAG = "RoboflowRepository"
    private val api = RoboflowClient.api

    private val apiKey    = BuildConfig.ROBOFLOW_API_KEY
    private val workspace = BuildConfig.ROBOFLOW_WORKSPACE
    private val project   = BuildConfig.ROBOFLOW_PROJECT
    private val version   = BuildConfig.ROBOFLOW_VERSION

    private var cachedLabels: List<String>? = null

    /**
     * Returns the ordered list of class names matching the model's output class indices.
     *
     * Source priority:
     *  1. version.classesList — JSON array from Roboflow for trained versions.
     *     Position in array == model class index. Most reliable source.
     *  2. project.classes sorted alphabetically — fallback for untrained versions
     *     where version.classes is null. Alphabetical order matches Roboflow's default
     *     class index assignment during training.
     *
     * NOTE: project.classes values are image COUNTS, not indices. Never sort by value.
     */
    suspend fun getClassLabels(): List<String> = withContext(Dispatchers.IO) {
        cachedLabels?.let { return@withContext it }
        try {
            val response = api.getDatasetVersion(workspace, project, version, apiKey)

            val classes: List<String> = when {
                // Primary: version.classesList is an ordered array — use directly.
                !response.version?.classesList.isNullOrEmpty() -> {
                    Log.d(TAG, "Using version.classes (ordered by class index)")
                    response.version!!.classesList!!
                }
                // Fallback: sort project.classes alphabetically by name.
                !response.project?.classes.isNullOrEmpty() -> {
                    Log.w(TAG, "version.classes is null — falling back to project.classes " +
                               "sorted alphabetically. Verify this matches your model's training order!")
                    response.project!!.classes!!
                        .entries
                        .sortedBy { it.key }  // sort by class NAME, never by value (image count)
                        .map { it.key }
                }
                else -> emptyList()
            }

            Log.d(TAG, "Loaded ${classes.size} class labels: $classes")
            cachedLabels = classes
            classes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch class labels: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSimilarImages(
        className: String,
        limit: Int = 16
    ): List<RoboflowImage> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchImages(
                workspace = workspace,
                project   = project,
                apiKey    = apiKey,
                body      = ImageSearchRequest(className = className, limit = limit)
            )
            val images = response.results ?: emptyList()
            Log.d(TAG, "Got ${images.size} images, first: ${images.firstOrNull()}")
            images
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "Failed to fetch similar images for '$className': ${e.message}, body: $errorBody")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch similar images for '$className': ${e.message}")
            emptyList()
        }
    }

    fun clearCache() { cachedLabels = null }
}
