# AI Classifier — Android App

Real-time image classification and object detection using on-device PyTorch Mobile models, with similar image retrieval from your Roboflow dataset.

## Features

- **Camera capture** or **gallery picker** as input
- **6 interchangeable models**: YOLOv8, YOLOv11, ResNet-50, EfficientNet-B5, Inception-v4, MobileNet-v4
- **On-device inference** via PyTorch Mobile (no GPU server needed)
- **Image retrieval**: after classifying, the app fetches similar images from your Roboflow dataset using the Roboflow Search API

---

## Setup Guide

### 1. Convert your weights to TorchScript (.ptl)

PyTorch Mobile requires TorchScript Lite files (`.ptl`), not raw `.pth` checkpoints.

```bash
pip install torch torchvision timm ultralytics

# Single model
python scripts/convert_to_torchscript.py \
    --model resnet50 \
    --weights /path/to/resnet50.pth \
    --num_classes <your_class_count> \
    --out_dir ./ptl_output

# All models at once (expects files named resnet50.pth, yolov8.pth, etc.)
python scripts/convert_to_torchscript.py \
    --model all \
    --weights_dir /path/to/weights/ \
    --num_classes <your_class_count>
```

### 2. Add .ptl files to the project

Copy the generated files into:

```
app/src/main/assets/models/
    yolov8.ptl
    yolov11.ptl
    resnet50.ptl
    efficientnetb5.ptl
    inceptionv4.ptl
    mobilenetv4.ptl
```

Only copy the models you have weights for. Set the rest as `isAvailable = false` in `ModelType.kt`.

### 3. Configure Roboflow

Open `app/build.gradle` and fill in your project details:

```groovy
buildConfigField "String", "ROBOFLOW_API_KEY",  '"your-api-key"'
buildConfigField "String", "ROBOFLOW_WORKSPACE", '"your-workspace-slug"'
buildConfigField "String", "ROBOFLOW_PROJECT",   '"your-project-slug"'
buildConfigField "String", "ROBOFLOW_VERSION",   '"1"'
```

You can find these values at https://roboflow.com → your project → Settings.

### 4. Build and run

Open the project in **Android Studio Hedgehog or newer**, sync Gradle, and run on a device or emulator with **Android 8.0+ (API 26+)**.

---

## Project Structure

```
app/src/main/java/com/aiclassifier/
├── ui/
│   ├── MainActivity.kt              — camera preview + gallery picker + model selector spinner
│   ├── ClassifierViewModel.kt       — orchestration, LiveData state management
│   └── result/
│       ├── ResultActivity.kt        — displays prediction result + similar images grid
│       └── SimilarImageAdapter.kt   — RecyclerView adapter for similar images (Glide)
├── ml/
│   ├── engine/
│   │   └── TorchInferenceEngine.kt  — loads .ptl via PyTorch Module, runs forward pass
│   └── models/
│       ├── ModelType.kt             — model registry enum (name, asset file, input size, type)
│       └── InferenceResult.kt       — result data classes for classification and detection
└── data/
    └── roboflow/
        ├── RoboflowApi.kt           — Retrofit interface, request/response data classes
        └── RoboflowRepository.kt    — API calls: class label fetching + similar image search

app/src/main/assets/models/          — .ptl model files go here
```

---

## Model Details

| Model | Type | Input Size | Asset File |
|---|---|---|---|
| YOLOv8 | Detection | 640×640 | `yolov8.ptl` |
| YOLOv11 | Detection | 640×640 | `yolov11.ptl` |
| ResNet-50 | Classification | 224×224 | `resnet50.ptl` |
| EfficientNet-B5 | Classification | 456×456 | `efficientnetb5.ptl` |
| Inception-v4 | Classification | 299×299 | `inceptionv4.ptl` |
| MobileNet-v4 | Classification | 224×224 | `mobilenetv4.ptl` |

Classification models use ImageNet normalisation (mean `[0.485, 0.456, 0.406]`, std `[0.229, 0.224, 0.225]`). YOLO models expect values in `[0, 1]` with no normalisation.

The model spinner in `MainActivity` lets users switch between all available architectures at runtime. Each switch loads a new `Module` instance, which typically takes 1–3 seconds on a modern device.

---

## Roboflow Image Retrieval

After inference, the app calls the **Roboflow Search API** (`POST /{workspace}/{project}/search`) with the predicted class name as `class_name` in the request body. It returns up to 16 matching images which are displayed in a horizontal grid on the result screen using Glide.

The search API is at project level (not version level) and requires a JSON body — not query parameters.

---

## Adding More Models

1. Add an entry to the `ModelType` enum in `ModelType.kt`.
2. Place the `.ptl` file in `app/src/main/assets/models/`.
3. Set `isAvailable = true`.
4. No other changes needed — the spinner and inference engine pick it up automatically.

---

## Notes on YOLO Output Format

The parser in `TorchInferenceEngine` expects YOLO output shape `[1, num_classes+5, num_anchors]`. If your export uses a different layout (e.g. newer YOLOv8 `.ptl` from Ultralytics may already include post-processing), adjust `parseYoloOutput()` accordingly or use the Ultralytics Android SDK directly.

---

## Dependencies

| Library | Purpose |
|---|---|
| PyTorch Mobile `2.1.0` | On-device model inference |
| CameraX `1.3.1` | Camera preview and capture |
| Retrofit `2.9.0` + Gson | Roboflow REST API |
| OkHttp `4.12.0` + Logging Interceptor | HTTP client and debug logging |
| Glide `4.16.0` | Similar image loading |
| Kotlin Coroutines `1.7.3` | Async network and inference calls |
