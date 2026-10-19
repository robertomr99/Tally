plugins {
    id("tally.android.library")
    id("tally.android.compose")
    id("tally.hilt")
}

android {
    namespace = "com.robertomr.tally.feature.gastos"
}

dependencies {
    // Casos de uso del listado y entidades (spec listado-gastos).
    implementation(project(":domain:expenses"))

    // PeriodoSelector, FilaTicket/CabeceraDia y formato compartidos.
    implementation(project(":feature:common"))

    // hiltViewModel() en Compose.
    implementation(libs.hilt.navigation.compose)

    // collectAsStateWithLifecycle.
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Iconos (receipt_long); los gestiona el Compose BOM.
    implementation(libs.androidx.compose.material.icons.extended)

    // Tests JVM del ViewModel con dobles de :test:unit + Turbine.
    testImplementation(project(":test:unit"))
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
