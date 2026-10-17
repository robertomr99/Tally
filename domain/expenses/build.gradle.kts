plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // Flow/suspend forman parte de la API pública del dominio.
    api(libs.coroutines.core)
    // TicketExtraidoParser (spec extraccion-nube): DTOs internos serializables
    // para parsear la respuesta del modelo (tolerante en el contenedor, estricto
    // en el schema). Kotlin puro, sin Android.
    implementation(libs.kotlinx.serialization.json)
    // @Inject constructor de TicketRepository (AGENTS.md) + runtime Dagger para
    // las fábricas generadas (dagger.internal.Factory).
    implementation(libs.javax.inject)
    implementation(libs.dagger)
    // Genera las fábricas Dagger de los @Inject constructor del módulo
    // (Hilt solo ve fábricas generadas en el módulo que compila la clase).
    ksp(libs.dagger.compiler)

    // Tests unitarios del repositorio con los dobles compartidos de :test:unit.
    testImplementation(project(":test:unit"))
    testImplementation(libs.junit)
}
