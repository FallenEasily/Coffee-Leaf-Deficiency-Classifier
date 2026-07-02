package com.aiclassifier.ml.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.aiclassifier.ml.models.*
import org.pytorch.IValue
/**import org.pytorch.LiteModuleLoader**/
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Wraps PyTorch Mobile (LiteModuleLoader) to run TorchScript (.ptl) models on device.
 *
 * Usage:
 *   val engine = TorchInferenceEngine(context)
 *   engine.loadModel(ModelType.RESNET50)
 *   val result = engine.classifyBitmap(bitmap, labels)
 */
class TorchInferenceEngine(private val context: Context) {

    private var module: Module? = null
    private var currentModelType: ModelType? = null

    // ImageNet normalisation constants (used by ResNet, EfficientNet, Inception, MobileNet).
    // YOLO models expect values in [0,1] — we skip normalisation for those.
    private val IMAGENET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val IMAGENET_STD  = floatArrayOf(0.229f, 0.224f, 0.225f)

    /** Load (or swap) the on-device model. Copies the asset to internal storage first. */
    fun loadModel(modelType: ModelType) {
        if (currentModelType == modelType && module != null) return

        module?.destroy()
        module = null

        val modelFile = assetToFile(modelType.assetFileName)
        module = Module.load(modelFile.absolutePath)
        currentModelType = modelType
    }

    /**
     * Run classification inference. Returns top-5 (or fewer) label-score pairs.
     * Caller must provide the class labels list matching the model's output dimension.
     */
    fun classifyBitmap(bitmap: Bitmap, labels: List<String>): ClassificationResult {
        val model = requireNotNull(module) { "Model not loaded — call loadModel() first." }
        val mType = requireNotNull(currentModelType)

        val size = mType.inputSize
        val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else bitmap
        val resized = Bitmap.createScaledBitmap(softwareBitmap, size, size, true)

        val inputTensor = if (mType.isClassifier) {
            // Normalise with ImageNet stats
            TensorImageUtils.bitmapToFloat32Tensor(resized, IMAGENET_MEAN, IMAGENET_STD)
        } else {
            // YOLO: normalise to [0,1] — same helper but with mean=0, std=1
            TensorImageUtils.bitmapToFloat32Tensor(
                resized,
                floatArrayOf(0f, 0f, 0f),
                floatArrayOf(1f, 1f, 1f)
            )
        }

        val start = System.currentTimeMillis()
        val output = model.forward(IValue.from(inputTensor)).toTensor()
        val elapsed = System.currentTimeMillis() - start

        val scores = output.dataAsFloatArray
        val softmax = softmax(scores)

        val top5 = softmax
            .mapIndexed { i, score -> LabelScore(labels.getOrElse(i) { "class_$i" }, score) }
            .sortedByDescending { it.score }
            .take(5)

        return ClassificationResult(top5, mType, elapsed)
    }

    /**
     * Run YOLO detection inference.
     * Returns detections after confidence + NMS filtering.
     *
     * The raw YOLO output format expected here is [1, num_classes+5, num_anchors].
     * Adjust parseYoloOutput() if your exported model uses a different layout.
     */
    fun detectBitmap(bitmap: Bitmap, labels: List<String>, confThresh: Float = 0.25f): DetectionResult {
        val model = requireNotNull(module) { "Model not loaded — call loadModel() first." }
        val mType = requireNotNull(currentModelType)

        val size = mType.inputSize
        val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else bitmap
        val resized = Bitmap.createScaledBitmap(softwareBitmap, size, size, true)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resized,
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(1f, 1f, 1f)
        )

        val start = System.currentTimeMillis()
        val output = model.forward(IValue.from(inputTensor)).toTensor()
        val elapsed = System.currentTimeMillis() - start

        val detections = parseYoloOutput(output.dataAsFloatArray, labels, confThresh, size)

        return DetectionResult(nonMaxSuppression(detections, 0.45f), mType, elapsed)
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun softmax(raw: FloatArray): FloatArray {
        val max = raw.max()!!
        val exp = raw.map { Math.exp((it - max).toDouble()).toFloat() }
        val sum = exp.sum()
        return exp.map { it / sum }.toFloatArray()
    }

    /**
     * Parse YOLO output tensor.
     * Expected shape: [batch=1, 85 (or num_classes+5), num_anchors]
     * Each anchor: [cx, cy, w, h, obj_conf, class_0_prob, class_1_prob, ...]
     */
    private fun parseYoloOutput(
        raw: FloatArray,
        labels: List<String>,
        confThresh: Float,
        imageSize: Int
    ): List<Detection> {
        val numClasses = labels.size
        val numFeatures = numClasses + 5
        val numAnchors = raw.size / numFeatures
        val results = mutableListOf<Detection>()

        for (i in 0 until numAnchors) {
            val offset = i * numFeatures
            val objConf = sigmoid(raw[offset + 4])
            if (objConf < confThresh) continue

            val cx = raw[offset]     / imageSize
            val cy = raw[offset + 1] / imageSize
            val bw = raw[offset + 2] / imageSize
            val bh = raw[offset + 3] / imageSize

            val left   = (cx - bw / 2f).coerceIn(0f, 1f)
            val top    = (cy - bh / 2f).coerceIn(0f, 1f)
            val right  = (cx + bw / 2f).coerceIn(0f, 1f)
            val bottom = (cy + bh / 2f).coerceIn(0f, 1f)

            var bestClass = 0
            var bestScore = 0f
            for (c in 0 until numClasses) {
                val score = sigmoid(raw[offset + 5 + c]) * objConf
                if (score > bestScore) { bestScore = score; bestClass = c }
            }

            if (bestScore >= confThresh) {
                results.add(Detection(
                    label       = labels.getOrElse(bestClass) { "class_$bestClass" },
                    confidence  = bestScore,
                    boundingBox = RectF(left, top, right, bottom)
                ))
            }
        }
        return results
    }

    private fun sigmoid(x: Float) = (1f / (1f + Math.exp(-x.toDouble()))).toFloat()

    /** Greedy NMS — removes boxes with IoU > iouThresh against higher-confidence boxes. */
    private fun nonMaxSuppression(detections: List<Detection>, iouThresh: Float): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<Detection>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeFirst()
            kept.add(best)
            sorted.removeAll { iou(it.boundingBox, best.boundingBox) > iouThresh }
        }
        return kept
    }

    private fun iou(a: RectF, b: RectF): Float {
        val ix = maxOf(a.left, b.left)
        val iy = maxOf(a.top, b.top)
        val ix2 = minOf(a.right, b.right)
        val iy2 = minOf(a.bottom, b.bottom)
        if (ix2 <= ix || iy2 <= iy) return 0f
        val inter = (ix2 - ix) * (iy2 - iy)
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        return inter / (areaA + areaB - inter)
    }

    /** Copies an asset file to internal storage (required for LiteModuleLoader). */
    private fun assetToFile(assetName: String): File {
        val outFile = File(context.filesDir, assetName)
        if (outFile.exists() && outFile.length() > 0) return outFile
        context.assets.open("models/$assetName").use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        }
        return outFile
    }

    fun destroy() {
        module?.destroy()
        module = null
    }
}
