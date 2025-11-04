import java.io.FileInputStream
import java.util.Properties

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

    implementation(com.google.code.gson.gson)

    implementation(com.squareup.okhttp3.okhttp)
    implementation(com.squareup.okhttp3.logging.interceptor)

    // Protocol Buffers
    implementation(com.google.protobuf.protobuf.kotlin)
    implementation(androidx.datastore.datastore)

    // Navigation
    implementation(androidx.navigation.navigation.compose)

    // Compose
    implementation(androidx.compose.ui.ui)
    implementation(androidx.compose.ui.ui.graphics)
    implementation(androidx.compose.ui.ui.tooling.preview)
    // Material
    implementation(androidx.compose.material3.material3)
    implementation(androidx.compose.material.material.icons.core)
    implementation(androidx.compose.material.material.icons.extended)
    // Animations
    implementation(androidx.compose.animation.animation)
    implementation(androidx.compose.animation.animation.core)
    implementation(androidx.compose.animation.animation.graphics)

    // AndroidX
    implementation(androidx.activity.activity.compose)
    implementation(androidx.compose.runtime.runtime)
    implementation(androidx.core.core.ktx)

    // Lifecycle
    implementation(androidx.lifecycle.lifecycle.runtime.ktx)
    implementation(androidx.lifecycle.lifecycle.viewmodel.ktx)

    compileOnly(de.robv.android.xposed.api)
    ksp(com.highcapable.yukihookapi.ksp.xposed)
    implementation(com.highcapable.yukihookapi.api)

    // Optional: KavaRef (https://github.com/HighCapable/KavaRef)
    implementation(com.highcapable.kavaref.kavaref.core)
    implementation(com.highcapable.kavaref.kavaref.extension)

    // Optional: BetterAndroid (https://github.com/BetterAndroid/BetterAndroid)
    implementation(com.highcapable.betterandroid.system.extension)

    testImplementation(junit.junit)
    androidTestImplementation(androidx.test.ext.junit)
    androidTestImplementation(androidx.test.espresso.espresso.core)
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