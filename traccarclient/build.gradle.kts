plugins {
    id("com.android.library")
    autowire(libs.plugins.kotlin.android)
}

android {
    namespace = "org.traccar.client"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    lint { checkReleaseBuilds = false }

    sourceSets {
        getByName("main") {
            java.srcDirs(
                "${rootDir}/external/traccar-client-android/app/src/main/java",
                "${rootDir}/external/traccar-client-android/app/src/regular/java"
            )
            res.srcDirs(
                "${rootDir}/external/traccar-client-android/app/src/main/res"
            )
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
        // Exclude original application manifest from external source to avoid conflicts
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("com.github.judemanutd:autostarter:1.1.0")
    implementation("dev.doubledot.doki:library:0.0.1@aar")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.7.3")
}