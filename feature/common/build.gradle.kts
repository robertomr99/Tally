plugins {
    id("tally.android.library")
    id("tally.android.compose")
}

android {
    namespace = "com.robertomr.tally.feature.common"
}

dependencies {
    // Iconos de la bottom bar (TallyBottomBar); los gestiona el Compose BOM.
    implementation(libs.androidx.compose.material.icons.extended)
}
