# AI Classifier — Android App

Real-time image classification and dataset image retrieval using your own PyTorch models and a Roboflow dataset.

## Features
- **Camera capture** or **gallery picker** as input
- **6 interchangeable models**: YOLOv8, YOLOv11, ResNet-50, EfficientNet-B5, Inception-v4, MobileNet-v4
- **On-device inference** via PyTorch Mobile (no GPU server needed)
- **Image retrieval**: after classifying, the app fetches visually similar images from your Roboflow dataset

---

## Setup guide

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

Only copy the models you have weights for. Mark the rest as `isAvailable = false` in `ModelType.kt`.

### 3. Configure Roboflow

Open `app/build.gradle` and fill in your project details:

```groovy
buildConfigField "String", "ROBOFLOW_API_KEY",  '"rf_xxxxxxxxxxxxxxxx"'
buildConfigField "String", "ROBOFLOW_WORKSPACE", '"your-workspace-slug"'
buildConfigField "String", "ROBOFLOW_PROJECT",   '"your-project-slug"'
buildConfigField "String", "ROBOFLOW_VERSION",   '"1"'
```

You can find these values at https://roboflow.com → your project → Settings.

**The class label order** returned by Roboflow must match the output neuron order of your models. The app sorts class names alphabetically by default — verify this matches your training setup, or edit `getClassLabels()` in `RoboflowRepository.kt`.

### 4. Build and run

Open the project in Android Studio (Hedgehog or newer), sync Gradle, and run on a device with Android 8.0+ (API 26+).

---

## Project structure

```
app/src/main/java/com/aiclassifier/
├── ui/
│   ├── MainActivity.kt          — camera preview + model selector
│   ├── ClassifierViewModel.kt   — orchestration, LiveData state
│   └── result/
│       ├── ResultActivity.kt    — shows prediction + similar images
│       └── SimilarImageAdapter.kt
├── ml/
│   ├── engine/
│   │   └── TorchInferenceEngine.kt  — loads .ptl, runs forward pass
│   └── models/
│       ├── ModelType.kt         — model registry (enum)
│       └── InferenceResult.kt   — result data classes
└── data/
    └── roboflow/
        ├── RoboflowApi.kt       — Retrofit interface + response models
        └── RoboflowRepository.kt — network calls, label caching

scripts/
└── convert_to_torchscript.py    — Python helper to produce .ptl files
```

## Swapping models at runtime

The spinner in the main screen lets users switch between all 6 architectures. The `TorchInferenceEngine` destroys the previous `Module` and loads the new one — each switch takes 1–3 seconds on a modern device.

## Adding more models

1. Add an entry to the `ModelType` enum.
2. Place the `.ptl` file in `assets/models/`.
3. Set `isAvailable = true`.
4. No other changes needed.

## Notes on YOLO output format

The parser in `TorchInferenceEngine.detectBitmap()` expects YOLO output shape `[1, num_classes+5, num_anchors]`. If your export uses a different layout (e.g. the newer YOLOv8 `.ptl` from Ultralytics may already include post-processing), adjust `parseYoloOutput()` accordingly or use the Ultralytics Android SDK directly.
