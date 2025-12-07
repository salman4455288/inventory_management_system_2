plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt") // Correct Kotlin DSL syntax for Kapt
}

android {
    namespace = "com.example.inventorymanagement"
    compileSdk = 34 // Checked: 34 or 35 is standard. 36 might be preview. Ensure this matches your SDK manager.

    defaultConfig {
        applicationId = "com.example.inventorymanagement"
        minSdk = 24
        targetSdk = 34 // Ensure this matches compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Usually 1.8 for Android
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Room Database
    val room_version = "2.6.1" // Use 'val' for Kotlin DSL
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    // CHANGED: Use add() instead of kapt() to resolve the reference error
    add("kapt", "androidx.room:room-compiler:$room_version")

    // External Libs
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.google.android.material:material:1.9.0")

    // Core Android Libs (from Catalog)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}