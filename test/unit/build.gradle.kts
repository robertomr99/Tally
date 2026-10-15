plugins {
    alias(libs.plugins.kotlin.jvm)
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
    // Los dobles implementan interfaces de :domain:expenses (spec persistencia-room).
    api(project(":domain:expenses"))
    api(libs.coroutines.test)
    api(libs.turbine)
    // CoroutineTestRule extiende TestWatcher de JUnit (AGENTS.md); los modulos
    // de test que usan la regla necesitan JUnit en su classpath de compilacion.
    api(libs.junit)
    testImplementation(libs.junit)
}
