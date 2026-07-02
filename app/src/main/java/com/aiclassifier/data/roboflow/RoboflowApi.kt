package com.aiclassifier.data.roboflow

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import retrofit2.http.POST
import retrofit2.http.Body

// ── Retrofit API interface ────────────────────────────────────────────────────

data class ImageSearchRequest(
    @SerializedName("class_name") val className: String,
    val limit: Int = 16,
    @SerializedName("fields") val fields: List<String> = listOf("id", "name", "labels", "url")
)

interface RoboflowApi {

    @GET("{workspace}/{project}/{version}")
    suspend fun getDatasetVersion(
        @Path("workspace") workspace: String,
        @Path("project")   project: String,
        @Path("version")   version: String,
        @Query("api_key")  apiKey: String
    ): DatasetVersionResponse

    @GET("{workspace}/{project}/{version}/export")
    suspend fun exportAnnotations(
        @Path("workspace") workspace: String,
        @Path("project")   project: String,
        @Path("version")   version: String,
        @Query("api_key")  apiKey: String,
        @Query("format")   format: String = "coco"
    ): ExportResponse

    @POST("{workspace}/{project}/search")
    suspend fun searchImages(
        @Path("workspace") workspace: String,
        @Path("project")   project: String,
        @Query("api_key")  apiKey: String,
        @Body              body: ImageSearchRequest
    ): ImageSearchResponse

}

// ── Response data classes ─────────────────────────────────────────────────────

data class DatasetVersionResponse(
    val version: VersionData?,
    val project: ProjectData?
)

data class ProjectData(
    // Map<className, imageCount> — the Int is an IMAGE COUNT, not a class index.
    val classes: Map<String, Int>?
)

data class VersionData(
    val id: String?,
    val name: String?,
    // For trained versions the Roboflow API returns "classes" as an ordered JSON array,
    // e.g. ["boron","calcium","healthy",...] where position == model class index.
    // Typed as List<String> so Gson parses it correctly.
    // Your previous Map<String,Int> type caused Gson to silently discard this field,
    // forcing the fallback to project.classes (image counts) with wrong ordering.
    @SerializedName("classes") val classesList: List<String>?,
    @SerializedName("splits") val splits: SplitCounts?
)

data class SplitCounts(
    val train: Int = 0,
    val valid: Int = 0,
    val test:  Int = 0
)

data class ExportResponse(val export: ExportData?)
data class ExportData(val link: String?)

data class ImageSearchResponse(
    val results: List<RoboflowImage>?,
    val total: Int?
)

data class RoboflowImage(
    val id: String,
    val name: String?,
    val labels: List<String>?,
    val url: String?
)

// ── Singleton client ──────────────────────────────────────────────────────────

object RoboflowClient {

    private const val BASE_URL = "https://api.roboflow.com/"

    val api: RoboflowApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY  // changed from BASIC
        }
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoboflowApi::class.java)
    }
}
