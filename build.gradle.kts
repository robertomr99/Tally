plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Solo se aplica en :app y solo si existe app/google-services.json
    // (spec extraccion-nube T1: el build debe quedar verde sin credenciales).
    alias(libs.plugins.google.services) apply false
}
