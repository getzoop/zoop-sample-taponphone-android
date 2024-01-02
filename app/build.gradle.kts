import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("kotlin-android")
}

val localProperties = gradleLocalProperties(rootDir)

android {
    namespace = "com.zoop.sdk.taponphone.sample"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "MARKETPLACE", localProperties["debug.MARKETPLACE"].toString())
            buildConfigField("String", "SELLER", localProperties["debug.SELLER"].toString())
            buildConfigField("String", "API_KEY", localProperties["debug.API_KEY"].toString())
            buildConfigField("String","SCOPE", localProperties["debug.SCOPE"].toString())
            buildConfigField("String","CLIENT_ID", localProperties["debug.CLIENT_ID"].toString())
            buildConfigField("String","CLIENT_SECRET", localProperties["debug.CLIENT_SECRET"].toString())
        }
        release {
            buildConfigField("String", "MARKETPLACE", localProperties["release.MARKETPLACE"].toString())
            buildConfigField("String", "SELLER", localProperties["release.SELLER"].toString())
            buildConfigField("String", "API_KEY", localProperties["release.API_KEY"].toString())
            buildConfigField("String", "SCOPE", localProperties["release.SCOPE"].toString())
            buildConfigField("String", "CLIENT_ID", localProperties["release.CLIENT_ID"].toString())
            buildConfigField("String", "CLIENT_SECRET", localProperties["release.CLIENT_SECRET"].toString())
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    debugImplementation(libs.zoop.taponphone.debug)
    releaseImplementation(libs.zoop.taponphone.release)

    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.crypto)
    implementation(libs.gson)
    implementation(libs.bundles.retrofit)
    runtimeOnly(libs.kotlin.reflect)
}
