plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

val releaseStoreFile = providers.environmentVariable("PURSIVO_KEYSTORE_PATH")
val releaseStorePassword = providers.environmentVariable("PURSIVO_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("PURSIVO_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("PURSIVO_KEY_PASSWORD")

android {
    namespace = "io.github.arfaouiahmed1.pursivo"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.arfaouiahmed1.pursivo"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("pursivo.versionCode").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("pursivo.versionName").orNull ?: "0.1.0-alpha.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseStoreFile.isPresent) {
                signingConfig = signingConfigs.create("pursivoRelease") {
                    storeFile = file(releaseStoreFile.get())
                    storePassword = releaseStorePassword.orNull
                    keyAlias = releaseKeyAlias.orNull
                    keyPassword = releaseKeyPassword.orNull
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ai"))
    implementation(project(":core:designsystem"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.work.runtime)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
