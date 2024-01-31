plugins {
    id("com.android.application")
    id("dev.rikka.tools.refine")
}

android {
    compileSdk = 34
    namespace = "com.saradabar.cpadcustomizetool"

    defaultConfig {
        minSdk = 22
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 22

        versionCode = 41
        versionName = "1.8.0"

        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.stephentuso:welcome:1.4.1")
    implementation("org.zeroturnaround:zt-zip:1.12")
    implementation("io.github.iamr0s:Dhizuku-API:2.4")
    compileOnly("dev.rikka.hidden:stub:4.3.2")
    implementation("dev.rikka.tools.refine:runtime:4.4.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
}