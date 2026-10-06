package com.robertomr.tally.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            plugins.withId("com.android.application") {
                extensions.configure(ApplicationExtension::class.java) {
                    buildFeatures.compose = true
                }
            }
            plugins.withId("com.android.library") {
                extensions.configure(LibraryExtension::class.java) {
                    buildFeatures.compose = true
                }
            }

            val catalog = libs
            dependencies.add("implementation", dependencies.platform(catalog.findLibrary("androidx-compose-bom").get()))
            dependencies.add("implementation", catalog.findLibrary("androidx-compose-ui").get())
            dependencies.add("implementation", catalog.findLibrary("androidx-compose-ui-graphics").get())
            dependencies.add("implementation", catalog.findLibrary("androidx-compose-ui-tooling-preview").get())
            dependencies.add("implementation", catalog.findLibrary("androidx-compose-material3").get())
            dependencies.add("debugImplementation", catalog.findLibrary("androidx-compose-ui-tooling").get())
        }
    }
}
