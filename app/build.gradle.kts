plugins {
    id("tally.android.application")
    id("tally.android.compose")
    id("tally.hilt")
}

android {
    namespace = "com.robertomr.tally"

    defaultConfig {
        applicationId = "com.robertomr.tally"
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":domain:core"))
    implementation(project(":domain:expenses"))
    implementation(project(":framework:database"))
    implementation(project(":framework:ai"))
    implementation(project(":feature:common"))
    implementation(project(":feature:resumen"))
    implementation(project(":feature:gastos"))
    implementation(project(":feature:escanear"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)

    // E2E de navegación (NavigationTest, spec navegacion-base). El platform del
    // Compose BOM se declara aquí porque androidTestImplementation no hereda el
    // platform que el convention plugin añade a implementation.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Fuerza Espresso 3.7.0: el 3.5.0 transitivo de ui-test-junit4 falla al
    // inyectar eventos en API 35+ (InputManager.getInstance eliminado).
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
