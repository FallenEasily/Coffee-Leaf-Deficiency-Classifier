package com.aiclassifier.ml.models

/**
 * All supported model architectures.
 * Each entry maps to a .ptl (TorchScript) file placed in assets/models/.
 *
 * HOW TO ADD YOUR WEIGHTS
 * -----------------------
 * 1. Convert your .pth to TorchScript with the Python helper in
 *    scripts/convert_to_torchscript.py  (included in this project).
 * 2. Copy the resulting .ptl file into app/src/main/assets/models/.
 * 3. Set isAvailable = true for that entry below.
 */
enum class ModelType(
    val displayName: String,
    val assetFileName: String,   // filename inside assets/models/
    val inputSize: Int,          // square crop side length expected by the model
    val isClassifier: Boolean,   // true = classification head; false = detection (YOLO)
    var isAvailable: Boolean = false
) {
    YOLOV8(
        displayName    = "YOLOv8",
        assetFileName  = "yolov8.ptl",
        inputSize      = 640,
        isClassifier   = false
    ),
    YOLOV11(
        displayName    = "YOLOv11",
        assetFileName  = "yolov11.ptl",
        inputSize      = 640,
        isClassifier   = false
    ),
    RESNET50(
        displayName    = "ResNet-50",
        assetFileName  = "resnet50.ptl",
        inputSize      = 224,
        isClassifier   = true
    ),
    EFFICIENTNET_B5(
        displayName    = "EfficientNet-B5",
        assetFileName  = "efficientnetb5.ptl",
        inputSize      = 456,
        isClassifier   = true
    ),
    INCEPTION_V4(
        displayName    = "Inception-v4",
        assetFileName  = "inceptionv4.ptl",
        inputSize      = 299,
        isClassifier   = true
    ),
    MOBILENET_V4(
        displayName    = "MobileNet-v4",
        assetFileName  = "mobilenetv4.ptl",
        inputSize      = 224,
        isClassifier   = true
    );

    companion object {
        /** Returns the default model to use when the app first launches. */
        fun default(): ModelType = RESNET50
    }
}
