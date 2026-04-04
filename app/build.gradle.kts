plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.example.shieldnet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.shieldnet"
        minSdk = 26
        targetSdk = 35
        buildConfigField(
            "String",
            "BASE_URL",
            "\"https://ktor-backend-eda7.onrender.com/\""
        )
        buildConfigField("String", "RAZORPAY_KEY", "\"rzp_test_123456\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    kapt {
        correctErrorTypes = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("androidx.appcompat:appcompat:1.6.1")
//    implementation("com.google.dagger:hilt-android:2.51")
//    kapt("com.google.dagger:hilt-compiler:2.51")
    implementation("com.google.dagger:hilt-android:2.56.1")
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)     // ← was 2.51
    kapt("com.google.dagger:hilt-compiler:2.56.1")              // ← was 2.51
    implementation("androidx.hilt:hilt-navigation-fragment:1.0.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    // ARCore
    implementation("com.google.ar:core:1.43.0")

    // TomTom
//    implementation("com.tomtom.sdk.maps:map-display:0.3.303")
//    implementation("com.tomtom.sdk.location:location-android:0.3.303")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.razorpay:checkout:1.6.40")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    // OkHttp logging
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}