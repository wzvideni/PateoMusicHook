plugins {
    id("com.android.library")
    autowire(libs.plugins.kotlin.android)
    autowire(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.wzvideni.traccar.ui"
    compileSdk = property.project.android.compileSdk

    defaultConfig {
        minSdk = property.project.android.minSdk
        targetSdk = property.project.android.targetSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Use default Compose compiler extension version managed by AGP/Kotlin

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":traccarclient"))

    // Compose core
    implementation("androidx.compose.ui:ui:1.9.3")
    implementation("androidx.compose.ui:ui-graphics:1.9.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.3")
    // Material3
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    // Activity Compose
    implementation("androidx.activity:activity-compose:1.10.1")
    // Navigation (optional)
    implementation("androidx.navigation:navigation-compose:2.9.3")

    // AndroidX
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}