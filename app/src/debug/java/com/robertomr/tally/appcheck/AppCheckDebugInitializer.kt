package com.robertomr.tally.appcheck

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Instala el provider de debug de App Check SOLO en la variante debug de `:app`
 * (spec procesado-ui T9): sin un [AppCheckProviderFactory] instalado, la app
 * normal no puede llamar a Gemini (Firebase AI Logic exige App Check enforced) y
 * el flujo fisico de extraccion en la nube fallaba con "Firebase App Check token
 * is invalid". Hasta ahora solo lo instalaba `ExtraccionNubeRealTest`, asi que la
 * extraccion real solo funcionaba bajo instrumentacion, nunca en la app.
 *
 * Se declara como ContentProvider (manifest de `src/debug`) para no anadir codigo
 * de debug de Firebase al codigo comun de `src/main` (que no puede referenciar una
 * dependencia `debugImplementation`) ni romper Hilt. El provider se inicializa
 * antes de cualquier Activity, por lo que el provider esta listo antes de la
 * primera extraccion.
 *
 * El debug token lo genera el propio provider en el dispositivo y hay que
 * registrarlo a mano en Firebase Console (Security > App Check > Apps > Manage
 * debug tokens); aparece en logcat como "Enter this debug secret into the allow
 * list". Sin provider de produccion (Play Integrity), pendiente futuro: esto es
 * solo debug y nunca viaja en el APK de release.
 */
class AppCheckDebugInitializer : ContentProvider() {

    override fun onCreate(): Boolean {
        val appContext = context ?: return true
        runCatching {
            val app = FirebaseApp.initializeApp(appContext) ?: return true
            FirebaseAppCheck.getInstance(app)
                .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
