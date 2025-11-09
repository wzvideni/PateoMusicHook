plugins {
    autowire(libs.plugins.android.application)
    autowire(libs.plugins.kotlin.android)
    autowire(libs.plugins.kotlin.ksp)
    autowire(libs.plugins.kotlin.compose)
    autowire(libs.plugins.google.protobuf)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.32.1"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java")
                create("kotlin")
            }
        }
    }
}

android {
    namespace = property.project.app.packageName
    compileSdk = property.project.android.compileSdk

    defaultConfig {
        applicationId = property.project.app.packageName
        minSdk = property.project.android.minSdk
        targetSdk = property.project.android.targetSdk
        versionName = property.project.app.versionName
        versionCode = property.project.app.versionCode
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    //   signingConfigs {
    //       create("wzvideni") {
//            val keystorePropertiesFile: File = rootProject.file("keystore.properties")
    //           val keystoreProperties = Properties()
    //           keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    //           keyAlias = keystoreProperties["keyAlias"] as String
//            keyPassword = keystoreProperties["keyPassword"] as String
    //           storeFile = file(keystoreProperties["storeFile"] as String)
    //           storePassword = keystoreProperties["storePassword"] as String
    //       }
    //   }
    buildTypes {
        release {
            //           signingConfig = signingConfigs.getByName("wzvideni")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    applicationVariants.all {
        outputs.map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName =
                    "PateoMusicHook_v${versionName}_${buildType.name}.apk"
                output.outputFileName = outputFileName
            }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    lint { checkReleaseBuilds = false }

    // TODO Please visit https://highcapable.github.io/YukiHookAPI/en/api/special-features/host-inject
    // TODO 请参考 https://highcapable.github.io/YukiHookAPI/zh-cn/api/special-features/host-inject
    // androidResources.additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x64")

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":traccarui"))
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    // Core libraries
    implementation("com.google.code.gson:gson:2.13.2")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("com.squareup.okhttp3:logging-interceptor:5.2.1")

    // MQTT (Eclipse Paho MQTT v3 client)
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // Protocol Buffers
    implementation("com.google.protobuf:protobuf-kotlin:4.32.1")
    implementation("androidx.datastore:datastore:1.1.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.3")

    // Compose
    implementation("androidx.compose.ui:ui:1.9.3")
    implementation("androidx.compose.ui:ui-graphics:1.9.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.3")
    // Material
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    // Animations
    implementation("androidx.compose.animation:animation:1.9.3")
    implementation("androidx.compose.animation:animation-core:1.9.3")
    implementation("androidx.compose.animation:animation-graphics:1.9.3")

    // AndroidX
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.runtime:runtime:1.9.3")
    implementation("androidx.core:core-ktx:1.17.0")
    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")

    compileOnly("de.robv.android.xposed:api:82")
    ksp("com.highcapable.yukihookapi:ksp-xposed:1.3.1")
    implementation("com.highcapable.yukihookapi:api:1.3.1")

    // Optional: KavaRef (https://github.com/HighCapable/KavaRef)
    implementation("com.highcapable.kavaref:kavaref-core:1.0.2")
    implementation("com.highcapable.kavaref:kavaref-extension:1.0.1")

    // Optional: BetterAndroid (https://github.com/BetterAndroid/BetterAndroid)
    implementation("com.highcapable.betterandroid:system-extension:1.0.3")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

tasks.named<Delete>("clean") {
    doLast {
        val moduleDir = project.projectDir
        val dirsToDelete = listOf(
            File(moduleDir, "debug"),
            File(moduleDir, "release"),
            File(moduleDir, "schemas")
        )
        dirsToDelete.forEach { dir ->
            if (dir.exists()) {
                println("Clean Project Deleting ${dir.absolutePath}")
                dir.deleteRecursively()
            }
        }
    }
}