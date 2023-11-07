plugins {
    id("com.android.application")
}

android {
    compileSdk = 33
    namespace = "com.saradabar.cpadcustomizetool"

    defaultConfig {
        minSdk = 22
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 22

        versionCode = 40
        versionName = "1.7.0"

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
            isMinifyEnabled = true
            isShrinkResources = true
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
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.stephentuso:welcome:1.4.1")
    implementation("org.zeroturnaround:zt-zip:1.12")
}