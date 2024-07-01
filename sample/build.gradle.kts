plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.repackPlugin)
}

android {
    namespace = "cn.lalaki.demo999"
    compileSdk = 35
    defaultConfig {
        applicationId = "cn.lalaki.demo999"
        minSdk = 22
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 29
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        create("release") {
            storeFile = file("D:\\imoe.jks")
            keyAlias = System.getenv("MY_PRIVATE_EMAIL")
            storePassword = System.getenv("mystorepass")
            keyPassword = System.getenv("mystorepass2")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs["release"]
        }
    }
    buildToolsVersion = "35.0.0"
}

kotlin {
    jvmToolchain(
        libs.versions.toolchain
            .get()
            .toInt(),
    )
}

dependencies {
    implementation(libs.androidx.appcompat)
}

repackConfig {
    blacklist =
        arrayOf(
            "DebugProbesKt.bin",
            "kotlin-tooling-metadata.json",
            "assets\\dexopt",
            "META-INF",
            "DebugProbesKt.bin",
            "kotlin-tooling-metadata.json",
            "kotlin",
        )
    disabled = false
    quiet = false
    resign = true
    addV1Sign = true
    addV2Sign = true
    disableV3V4 = true
    apkFile = null
}
