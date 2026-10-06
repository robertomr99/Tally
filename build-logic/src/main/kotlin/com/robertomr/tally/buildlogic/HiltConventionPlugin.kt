package com.robertomr.tally.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class HiltConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")

            val catalog = libs
            dependencies.add("implementation", catalog.findLibrary("hilt-android").get())
            dependencies.add("ksp", catalog.findLibrary("hilt-compiler").get())
        }
    }
}
