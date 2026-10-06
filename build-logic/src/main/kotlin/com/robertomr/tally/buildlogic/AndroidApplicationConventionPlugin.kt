package com.robertomr.tally.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        val catalog = libs
        extensions.configure(ApplicationExtension::class.java) {
            compileSdk = catalog.versionInt("compileSdk")
            defaultConfig {
                minSdk = catalog.versionInt("minSdk")
                targetSdk = catalog.versionInt("targetSdk")
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }
}
