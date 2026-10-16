plugins {
    id("tally.android.library")
    id("tally.hilt")
}

android {
    namespace = "com.robertomr.tally.framework.ai"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // Contratos de dominio que este modulo implementa (OcrEngine de ocr-mlkit).
    implementation(project(":domain:expenses"))

    implementation(libs.coroutines.core)

    // MediaPipe GenAI / LLM Inference (spike local Gemma). El modelo NO va en el APK.
    implementation(libs.mediapipe.tasks.genai)

    // ML Kit Text Recognition v2 latin bundled (modelo en el APK; OCR on-device
    // de ocr-mlkit, ADR 0002 etapa 1). Sin Play Services ni red.
    implementation(libs.mlkit.text.recognition)

    // Harness instrumentado sin UI del spike (se ejecuta a mano con la Tab S9+).
    androidTestImplementation(libs.coroutines.core)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
}
