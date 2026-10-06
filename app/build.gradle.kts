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
    }
}

dependencies {
    implementation(project(":domain:core"))
    implementation(project(":domain:expenses"))
    implementation(project(":framework:database"))
    implementation(project(":framework:ai"))
    implementation(project(":feature:common"))
    implementation(project(":feature:resumen"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
