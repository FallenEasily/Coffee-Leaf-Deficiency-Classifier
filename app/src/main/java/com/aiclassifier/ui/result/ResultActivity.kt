package com.aiclassifier.ui.result

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.aiclassifier.databinding.ActivityResultBinding
import com.aiclassifier.ml.models.ClassificationResult
import com.aiclassifier.ml.models.DetectionResult
import com.aiclassifier.ml.models.ModelType
import com.aiclassifier.ui.BitmapHolder
import com.aiclassifier.ui.ClassifierViewModel
import com.aiclassifier.ui.UiState

class ResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODEL_NAME = "extra_model_name"
    }

    private lateinit var binding: ActivityResultBinding
    private val viewModel: ClassifierViewModel by viewModels()
    private lateinit var similarImagesAdapter: SimilarImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Result"

        setupRecyclerView()
        observeViewModel()

        val bitmap = BitmapHolder.bitmap
        if (bitmap == null) {
            Toast.makeText(this, "No image to classify.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Restore the model the user selected in MainActivity
        val modelName = intent.getStringExtra(EXTRA_MODEL_NAME)
        val modelType = modelName?.let {
            runCatching { ModelType.valueOf(it) }.getOrNull()
        } ?: ModelType.default()
        viewModel.selectModel(modelType)

        binding.imagePreview.setImageBitmap(bitmap)
        viewModel.runInference(bitmap)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        similarImagesAdapter = SimilarImageAdapter()
        binding.rvSimilarImages.apply {
            layoutManager = GridLayoutManager(this@ResultActivity, 4)
            adapter = similarImagesAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Idle -> { /* do nothing */ }

                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.cardResult.visibility  = View.GONE
                    binding.cardSimilar.visibility = View.GONE
                }

                is UiState.ClassifySuccess -> {
                    binding.progressBar.visibility = View.GONE
                    showClassificationResult(state.result)
                    showSimilarImages(state.similarImages.mapNotNull { it.url })
                }

                is UiState.DetectSuccess -> {
                    binding.progressBar.visibility = View.GONE
                    showDetectionResult(state.result)
                    showSimilarImages(state.similarImages.mapNotNull { it.url })
                }

                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.cardResult.visibility  = View.VISIBLE
                    binding.tvPrimaryLabel.text    = "Error"
                    binding.tvDetails.text         = state.message
                    binding.cardSimilar.visibility = View.GONE
                }
            }
        }
    }

    private fun showClassificationResult(result: ClassificationResult) {
        binding.cardResult.visibility = View.VISIBLE
        val top = result.topLabels.firstOrNull()
        binding.tvPrimaryLabel.text = top?.label ?: "Unknown"
        binding.tvConfidence.text   = top?.let { "%.1f%%".format(it.score * 100) } ?: ""
        binding.tvModel.text        = "Model: ${result.modelType.displayName}"
        binding.tvInference.text    = "Inference: ${result.inferenceTimeMs} ms"

        val details = result.topLabels.joinToString("\n") { ls ->
            "%-20s  %.1f%%".format(ls.label, ls.score * 100)
        }
        binding.tvDetails.text = details
    }

    private fun showDetectionResult(result: DetectionResult) {
        binding.cardResult.visibility = View.VISIBLE
        val top = result.detections.firstOrNull()
        binding.tvPrimaryLabel.text = top?.label ?: "Nothing detected"
        binding.tvConfidence.text   = top?.let { "%.1f%%".format(it.confidence * 100) } ?: ""
        binding.tvModel.text        = "Model: ${result.modelType.displayName}"
        binding.tvInference.text    = "Inference: ${result.inferenceTimeMs} ms"

        val details = result.detections.take(5).joinToString("\n") { d ->
            "%-20s  %.1f%%".format(d.label, d.confidence * 100)
        }
        binding.tvDetails.text = if (details.isEmpty()) "No detections above threshold." else details
    }

    private fun showSimilarImages(urls: List<String>) {
        if (urls.isEmpty()) {
            binding.cardSimilar.visibility = View.GONE
            return
        }
        binding.cardSimilar.visibility = View.VISIBLE
        similarImagesAdapter.submitList(urls)
    }
}
