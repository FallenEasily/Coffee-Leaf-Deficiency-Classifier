package com.aiclassifier.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.aiclassifier.databinding.ActivityMainBinding
import com.aiclassifier.ml.models.ModelType
import com.aiclassifier.ui.result.ResultActivity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ClassifierViewModel by viewModels()

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    // ── Activity result launchers ─────────────────────────────────────────────

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val bitmap = uriToBitmap(uri) ?: return@registerForActivityResult
        launchResult(bitmap)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to capture images.", Toast.LENGTH_LONG).show()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupModelSpinner()
        setupButtons()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            permissionLauncher.launch(requiredPermissions())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private fun setupModelSpinner() {
        val modelNames = ModelType.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerModel.adapter = adapter
        binding.spinnerModel.setSelection(ModelType.values().indexOf(ModelType.default()))

        binding.spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                viewModel.selectModel(ModelType.values()[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupButtons() {
        binding.btnCapture.setOnClickListener { capturePhoto() }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: return
        capture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()
                    launchResult(bitmap)
                }
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@MainActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun launchResult(bitmap: Bitmap) {
        BitmapHolder.bitmap = bitmap
        val selectedModel = viewModel.selectedModel.value ?: ModelType.default()
        val intent = Intent(this, ResultActivity::class.java).apply {
            // Pass the selected model name so ResultActivity uses the same one
            putExtra(ResultActivity.EXTRA_MODEL_NAME, selectedModel.name)
        }
        startActivity(intent)
    }

    private fun imageProxyToBitmap(proxy: ImageProxy): Bitmap {
        val buffer = proxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) { null }
    }

    private fun allPermissionsGranted() = requiredPermissions().all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requiredPermissions(): Array<String> {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        else
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        return perms.toTypedArray()
    }
}

/** Simple singleton to pass Bitmap between activities without Parcelable limits. */
object BitmapHolder {
    var bitmap: Bitmap? = null
}
