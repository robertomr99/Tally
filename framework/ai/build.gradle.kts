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
    implementation(libs.coroutines.core)

    // MediaPipe GenAI / LLM Inference (spike local Gemma). El modelo NO va en el APK.
    implementation(libs.mediapipe.tasks.genai)

    // Harness instrumentado sin UI del spike (se ejecuta a mano con la Tab S9+).
    androidTestImplementation(libs.coroutines.core)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
}
