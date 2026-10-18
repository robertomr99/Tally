plugins {
    id("tally.android.library")
    id("tally.android.compose")
}

android {
    namespace = "com.robertomr.tally.feature.resumen"
}

dependencies {
    // Icono de Ajustes (Settings); lo gestiona el Compose BOM.
    implementation(libs.androidx.compose.material.icons.extended)
}
