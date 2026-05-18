import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.sahmfood.pos.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sahmfood.pos.android"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = project.findProperty("appVersionName") as String? ?: "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JvmTarget.JVM_17.target
    }

    buildFeatures {
        compose = true
    }

    lint {
        lintConfig = file("${rootProject.projectDir}/config/android-lint.xml")
        disable += "NullSafeMutableLiveData"
        abortOnError = true
        warningsAsErrors = false
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
