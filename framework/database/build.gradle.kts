plugins {
    id("tally.android.library")
    id("tally.hilt")
    // KSP ya lo aplica tally.hilt; se declara aquí para tener el accessor
    // tipado del bloque ksp { } con room.schemaLocation.
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.robertomr.tally.framework.database"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Los esquemas exportados (1.json, 2.json...) son assets de androidTest para
    // que MigrationTestHelper cree la BD en una version anterior.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    // Esquema exportado (exportSchema = true) a schemas/ para migraciones futuras.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Interfaces de DataSource del dominio que este módulo implementa.
    implementation(project(":domain:expenses"))

    implementation(libs.coroutines.core)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    // Orientacion EXIF del pipeline de foto (spec captura-foto).
    implementation(libs.exifinterface)

    // Unit tests JVM de la matematica pura del pipeline (PhotoPipelineMath).
    testImplementation(libs.junit)

    // Tests instrumentados sin UI (Room in-memory + fichero real en filesDir).
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
}
