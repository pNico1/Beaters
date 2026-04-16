// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Nota: en AGP 9.x el soporte Kotlin viene integrado en el plugin android.application,
// así que NO se aplica org.jetbrains.kotlin.android por separado.
plugins {
    alias(libs.plugins.android.application) apply false
}
