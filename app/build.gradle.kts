plugins {
    id("tally.android.application")
    id("tally.android.compose")
    id("tally.hilt")
}

// Plugin google-services CONDICIONAL (spec extraccion-nube T1): solo se aplica
// si existe app/google-services.json (prerrequisito humano, U1 del spec). Sin
// el fichero el build queda en verde y ExtraccionNubeRealTest se salta; NO se
// inventa ningun google-services.json ni clave. Ver docs/specs/extraccion-nube.md.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.robertomr.tally"

    defaultConfig {
        applicationId = "com.robertomr.tally"
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()
        // Runner estandar: ExtraccionNubeRealTest obtiene TextEngine via
        // EntryPointAccessors (ver su KDoc; R-NUBE6 del spec - HiltTestApplication
        // exige HiltAndroidRule en todos los tests y rompia NavigationTest/
        // CapturaFlujoTest). El runner NO cambia respecto a captura-foto/ocr-mlkit.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Debug token FIJO de App Check para el test real de nube (extraccion-nube
        // T7): AGP desinstala la app al terminar cada connectedDebugAndroidTest y
        // firebase-appcheck-debug 20.0.0 guarda su token generado solo en datos de
        // la app, asi que cada ejecucion generaria un token nuevo que habria que
        // volver a registrar a mano en la consola. Con el token fijo (mismo patron
        // que app/google-services.json: fichero LOCAL fuera de VCS, lo aporta el
        // humano tras registrarlo en Firebase Console > App Check > Manage debug
        // tokens) la ejecucion es determinista. Si el fichero no existe, el test
        // recurre al factory estandar y se comporta como antes.
        val ficheroTokenAppCheck = file("appcheck-debug-token.txt")
        if (ficheroTokenAppCheck.exists()) {
            testInstrumentationRunnerArguments["appcheckDebugToken"] =
                ficheroTokenAppCheck.readText(Charsets.UTF_8).trim()
        }
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
    implementation(project(":feature:ajustes"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)

    // E2E de navegación (NavigationTest, spec navegacion-base). El platform del
    // Compose BOM se declara aquí porque androidTestImplementation no hereda el
    // platform que el convention plugin añade a implementation.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Fuerza Espresso 3.7.0: el 3.5.0 transitivo de ui-test-junit4 falla al
    // inyectar eventos en API 35+ (InputManager.getInstance eliminado).
    androidTestImplementation(libs.androidx.test.espresso.core)
    // Stub del selector de fotos del sistema en GaleriaFlujoTest (spec galeria-escaneo).
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // WorkManager en el E2E de descarga-modelo (comprobar que no se encola trabajo).
    androidTestImplementation(libs.androidx.work.runtime.ktx)
    // App Check debug provider para el test real de nube (extraccion-nube T7):
    // Firebase AI Logic exige App Check enforced; sin provider de produccion
    // (pendiente futuro). Sin claves en el repo: el debug token es un fichero
    // local gitignoreado (app/appcheck-debug-token.txt) que se registra a mano en
    // la consola (Manage debug tokens). Versiones via BoM de framework:ai.
    androidTestImplementation(platform(libs.firebase.bom))
    androidTestImplementation(libs.firebase.appcheck)
    androidTestImplementation(libs.firebase.appcheck.debug)
    // El provider de debug de App Check debe vivir TAMBIEN en el APK de la app
    // (variante debug): firebase-appcheck-debug 20.0.0 registra su componente
    // Firebase (DebugAppCheckProvider) via el manifest de la libreria, y el
    // descubrimiento de componentes solo lee el paquete de la app; si la
    // libreria esta solo en el APK de test, DebugAppCheckProviderFactory.create()
    // devuelve null ("No AppCheckProvider installed") y Gemini rechaza la
    // peticion sin token. Solo se instala en tests (ExtraccionNubeRealTest);
    // sin claves ni secretos en el APK (el debug token se genera en el
    // dispositivo y se registra a mano en la consola de Firebase).
    debugImplementation(platform(libs.firebase.bom))
    debugImplementation(libs.firebase.appcheck.debug)
    androidTestImplementation(libs.junit)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
