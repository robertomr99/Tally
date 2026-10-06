plugins {
    `kotlin-dsl`
}

group = "com.robertomr.tally.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "tally.android.application"
            implementationClass = "com.robertomr.tally.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "tally.android.library"
            implementationClass = "com.robertomr.tally.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "tally.android.compose"
            implementationClass = "com.robertomr.tally.buildlogic.AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "tally.hilt"
            implementationClass = "com.robertomr.tally.buildlogic.HiltConventionPlugin"
        }
    }
}
