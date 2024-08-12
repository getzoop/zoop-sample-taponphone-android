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
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "MARKETPLACE", localProperties["staging.MARKETPLACE"].toString())
            buildConfigField("String", "SELLER", localProperties["staging.SELLER"].toString())
            buildConfigField("String", "API_KEY", localProperties["staging.API_KEY"].toString())
            buildConfigField("String","CLIENT_ID", localProperties["mypinpad.NUBANK_STAGE_CLIENT_ID"].toString())
            buildConfigField("String","CLIENT_SECRET", localProperties["mypinpad.NUBANK_STAGE_CLIENT_SECRET"].toString())
        }
        release {
            buildConfigField("String", "MARKETPLACE", localProperties["production.MARKETPLACE"].toString())
            buildConfigField("String", "SELLER", localProperties["production.SELLER"].toString())
            buildConfigField("String", "API_KEY", localProperties["production.API_KEY"].toString())
            buildConfigField("String","CLIENT_ID", localProperties["mypinpad.NUBANK_PROD_CLIENT_ID"].toString())
            buildConfigField("String","CLIENT_SECRET", localProperties["mypinpad.NUBANK_PROD_CLIENT_SECRET"].toString())
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
//    debugImplementation(libs.zoop.taponphone.debug)
//    releaseImplementation(libs.zoop.taponphone.release)

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
    implementation(libs.lottie)

    implementation(mapOf("name" to "plugin-staging-nubank-debug", "ext" to "aar"))

}
