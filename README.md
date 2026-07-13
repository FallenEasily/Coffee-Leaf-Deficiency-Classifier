# Coffee Leaf Classifier — Android App

An app that takes a photo (camera or gallery) and classifies coffee leaf mineral deficiencies using on-device AI models.
It also shows similar images from our augmented dataset in Roboflow.

---

## How to Setup on Android Studio:

---

## 📦 Step 1: Download the Project Files

All the model files are too big for GitHub, so they're stored on Google Drive:
**`.ptl` files** → these are for **Android Studio** (ready to drop into the app)

👉 **[Download models here](https://drive.google.com/drive/folders/11r4sSaNRacGlEM297KEB-MDOTwl_Jd9O)**

---

## 🛠️ Step 2: Open the Project in Android Studio

1. Open **Android Studio** (make sure it's a recent version — Hedgehog or newer).
2. Click **File → Open**, then select this project folder.
3. Wait for Gradle to finish syncing (bottom bar will show progress). This can take a few minutes the first time.

---

## 📂 Step 3: Add the Model Files

1. From the Google Drive folder, download the **`.ptl` files**.
2. Copy them into this folder in the project:
   ```
   app/src/main/assets/models/
   ```
3. You should end up with files like:
   ```
   app/src/main/assets/models/
       yolov8.ptl
       yolov11.ptl
       resnet50.ptl
       efficientnetb5.ptl
       inceptionv4.ptl
       mobilenetv4.ptl
   ```

---

## ▶️ Step 4: Run the App

1. Connect an Android phone (with USB debugging on) **or** start an emulator.
2. Click the green **Run ▶️** button in Android Studio.
3. The app needs **Android 8.0 (API 26) or higher** to run.
4. Allow camera permission when asked — the app needs it to take photos.

---

## 🔗 Project Links

- **All Project Files** https://drive.google.com/drive/folders/1H9Sc5F682mGgR8clRmw3Kz5qA9ZvBsCG
- **Augmented dataset:** https://app.roboflow.com/coffee-leaf-nutrional-deficiency/coffee-mineral-deficiency/3