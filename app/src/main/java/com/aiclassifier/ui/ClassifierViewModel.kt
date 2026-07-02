package com.aiclassifier.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aiclassifier.data.roboflow.RoboflowImage
import com.aiclassifier.data.roboflow.RoboflowRepository
import com.aiclassifier.ml.engine.TorchInferenceEngine
import com.aiclassifier.ml.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class ClassifySuccess(
        val result: ClassificationResult,
        val similarImages: List<RoboflowImage>
    ) : UiState()
    data class DetectSuccess(
        val result: DetectionResult,
        val similarImages: List<RoboflowImage>
    ) : UiState()
    data class Error(val message: String) : UiState()
}

class ClassifierViewModel(application: Application) : AndroidViewModel(application) {

    private val engine     = TorchInferenceEngine(application)
    private val repository = RoboflowRepository()

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    private val _selectedModel = MutableLiveData(ModelType.default())
    val selectedModel: LiveData<ModelType> = _selectedModel

    // ── Public API ────────────────────────────────────────────────────────────

    fun selectModel(modelType: ModelType) {
        _selectedModel.value = modelType
    }

    /**
     * Run inference on [bitmap] using the currently selected model.
     * Automatically routes to classify() or detect() based on ModelType.isClassifier.
     */
    fun runInference(bitmap: Bitmap) {
        val modelType = _selectedModel.value ?: ModelType.default()
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val labels = repository.getClassLabels()
                if (labels.isEmpty()) {
                    _uiState.value = UiState.Error(
                        "Could not load class labels from Roboflow. " +
                        "Check your API key and project settings in build.gradle."
                    )
                    return@launch
                }

                withContext(Dispatchers.Default) {
                    engine.loadModel(modelType)
                }

                if (modelType.isClassifier) {
                    val result = withContext(Dispatchers.Default) {
                        engine.classifyBitmap(bitmap, labels)
                    }
                    val topClass = result.topLabels.firstOrNull()?.label ?: ""
                    val similar  = repository.getSimilarImages(topClass, limit = 16)
                    _uiState.value = UiState.ClassifySuccess(result, similar)
                } else {
                    val result = withContext(Dispatchers.Default) {
                        engine.detectBitmap(bitmap, labels)
                    }
                    // Use the highest-confidence detection's class for retrieval
                    val topClass = result.detections.firstOrNull()?.label ?: ""
                    val similar  = if (topClass.isNotEmpty())
                        repository.getSimilarImages(topClass, limit = 16)
                    else emptyList()
                    _uiState.value = UiState.DetectSuccess(result, similar)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Inference failed: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.destroy()
    }
}
