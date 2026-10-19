plugins {
    id("tally.android.library")
    id("tally.android.compose")
    id("tally.hilt")
}

android {
    namespace = "com.robertomr.tally.feature.ajustes"
}

dependencies {
    // Casos de uso y estado del modelo local (spec descarga-modelo).
    implementation(project(":domain:expenses"))

    // Cabecera y columna centrada compartidas (spec layout-tablet-coherente).
    implementation(project(":feature:common"))

    // hiltViewModel() en Compose.
    implementation(libs.hilt.navigation.compose)

    // collectAsStateWithLifecycle.
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Iconos (arrow_back, delete...); los gestiona el Compose BOM.
    implementation(libs.androidx.compose.material.icons.extended)

    // Tests JVM del ViewModel con dobles de :test:unit + Turbine.
    testImplementation(project(":test:unit"))
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
