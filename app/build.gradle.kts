import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("kotlin-android")
}

val localProperties = gradleLocalProperties(rootDir, providers)

val demoVersionCode = 34
val demoVersionName = "$demoVersionCode"

android {
    namespace = "com.zoop.sdk.taponphone.sample"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        targetSdk = 35
        versionCode = demoVersionCode
        versionName = demoVersionName

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        kotlinOptions { jvmTarget = "17" }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "MARKETPLACE",
                localProperties["sandbox.MARKETPLACE"].toString()
            )
            buildConfigField("String", "SELLER", localProperties["sandbox.SELLER"].toString())
            buildConfigField("String", "API_KEY", localProperties["sandbox.API_KEY"].toString())
            buildConfigField("String", "CLIENT_ID", localProperties["ZOOP_SANDBOX_CLIENT_ID"].toString())
            buildConfigField(
                "String",
                "CLIENT_SECRET",
                localProperties["ZOOP_SANDBOX_CLIENT_SECRET"].toString()
            )

        }
        release {
            buildConfigField(
                "String",
                "MARKETPLACE",
                localProperties["release.MARKETPLACE"].toString()
            )
            buildConfigField("String", "SELLER", localProperties["release.SELLER"].toString())
            buildConfigField("String", "API_KEY", localProperties["release.API_KEY"].toString())
            buildConfigField("String", "CLIENT_ID", localProperties["release.CLIENT_ID"].toString())
            buildConfigField(
                "String",
                "CLIENT_SECRET",
                localProperties["release.CLIENT_SECRET"].toString()
            )

        }

        buildFeatures {
            viewBinding = true
            buildConfig = true
        }
    }

    dependencies {
        debugImplementation(libs.zoop.taponphone.sandbox)
        releaseImplementation(libs.zoop.taponphone.release)

        implementation(libs.bundles.androidx.lifecycle)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.lottie)

        implementation(libs.androidx.crypto)
        implementation(libs.gson)
        implementation(libs.bundles.retrofit)
        runtimeOnly(libs.kotlin.reflect)
    }
}
