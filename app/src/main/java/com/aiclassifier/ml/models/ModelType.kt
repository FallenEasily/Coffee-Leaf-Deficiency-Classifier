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
    INCEPTION_V4(
        displayName    = "Inception-v4",
        assetFileName  = "inceptionv4.ptl",
        inputSize      = 299,
        isClassifier   = true
    );

    companion object {
        /** Returns the default model to use when the app first launches. */
        fun default(): ModelType = INCEPTION_V4
    }
}
