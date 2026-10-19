plugins {
    id("tally.android.library")
    id("tally.android.compose")
}

android {
    namespace = "com.robertomr.tally.feature.common"
}

dependencies {
    // Entidad Categoria (etiqueta, icono y color en CategoriaUi).
    implementation(project(":domain:expenses"))

    // Iconos de la bottom bar (TallyBottomBar); los gestiona el Compose BOM.
    implementation(libs.androidx.compose.material.icons.extended)

    // Tests JVM de las etiquetas puras (EtiquetaDia).
    testImplementation(libs.junit)
}
