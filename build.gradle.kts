plugins {
    alias(libs.plugins.kotlin.multiplatform)    apply false
    alias(libs.plugins.kotlin.android)          apply false
    alias(libs.plugins.kotlin.serialization)    apply false
    alias(libs.plugins.android.application)     apply false
    alias(libs.plugins.android.library)         apply false
    alias(libs.plugins.compose.multiplatform)   apply false
    alias(libs.plugins.compose.compiler)        apply false
    alias(libs.plugins.sqldelight)              apply false
}

/** Local/CI: ./gradlew ci */
tasks.register("ci") {
    group = "verification"
    description = "Run unit tests, Kotlin compile checks, and debug APK build"
    dependsOn(
        ":shared:testDebugUnitTest",
        ":shared:compileDebugKotlinAndroid",
        ":composeApp:compileDebugKotlinAndroid",
        ":androidApp:compileDebugKotlin",
        ":androidApp:assembleDebug"
    )
}

/** Local/CI: ./gradlew releaseAndroid -PappVersionName=1.2.0 -PappVersionCode=3 */
tasks.register("releaseAndroid") {
    group = "release"
    description = "Build release APK (set appVersionName / appVersionCode properties)"
    dependsOn(
        ":shared:testDebugUnitTest",
        ":androidApp:assembleRelease"
    )
}
