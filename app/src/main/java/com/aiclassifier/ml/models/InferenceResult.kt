package com.aiclassifier.ml.models

import android.graphics.RectF

/** Result from a classifier model (ResNet, EfficientNet, Inception, MobileNet). */
data class ClassificationResult(
    val topLabels: List<LabelScore>,
    val modelType: ModelType,
    val inferenceTimeMs: Long
)

/** A single class label with its softmax confidence score. */
data class LabelScore(
    val label: String,
    val score: Float
)

/** Result from a detection model (YOLOv8, YOLOv11). */
data class DetectionResult(
    val detections: List<Detection>,
    val modelType: ModelType,
    val inferenceTimeMs: Long
)

/** A single detected object bounding box. */
data class Detection(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF   // normalised [0..1] coordinates
)
