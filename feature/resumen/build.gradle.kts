plugins {
    id("tally.android.library")
    id("tally.android.compose")
    id("tally.hilt")
}

android {
    namespace = "com.robertomr.tally.feature.resumen"
}

dependencies {
    // Casos de uso del período y entidades (spec periodo-selector).
    implementation(project(":domain:expenses"))

    // Etiqueta, icono y color de categoría compartidos (spec grafica-donut, D2).
    implementation(project(":feature:common"))

    // hiltViewModel() en Compose.
    implementation(libs.hilt.navigation.compose)

    // collectAsStateWithLifecycle.
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Iconos (Settings, Check); los gestiona el Compose BOM.
    implementation(libs.androidx.compose.material.icons.extended)

    // Tests JVM del ViewModel con dobles de :test:unit + Turbine.
    testImplementation(project(":test:unit"))
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
