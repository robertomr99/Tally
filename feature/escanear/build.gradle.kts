plugins {
    id("tally.android.library")
    id("tally.android.compose")
    id("tally.hilt")
}

android {
    namespace = "com.robertomr.tally.feature.escanear"
}

dependencies {
    // Casos de uso de dominio (GuardarFotoCapturada) + PhotoFileStore.
    implementation(project(":domain:expenses"))

    // hiltViewModel() en Compose.
    implementation(libs.hilt.navigation.compose)

    // Launcher de permisos en runtime (rememberLauncherForActivityResult).
    implementation(libs.androidx.activity.compose)

    // LocalLifecycleOwner del binding CameraX (androidx.lifecycle.compose).
    implementation(libs.androidx.lifecycle.runtime.compose)

    // CameraX: binding de UI de la camara (solo aqui; ver spec captura-foto).
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Iconos extendidos (replay, arrow_forward, photo_camera...); los gestiona
    // el Compose BOM.
    implementation(libs.androidx.compose.material.icons.extended)

    // Tests JVM del ViewModel con dobles de :test:unit + Turbine.
    testImplementation(project(":test:unit"))
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
