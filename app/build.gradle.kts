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

        versionCode = 43
        versionName = "2.0.1"

        multiDexEnabled = true

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }

        resourceConfigurations += "ja"
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

    //Lib
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar"))))

    //Material
    implementation("com.google.android.material:material:1.11.0")

    //AndroidX
    implementation("androidx.preference:preference:1.2.1")

    //WalkThrough
    implementation("com.stephentuso:welcome:1.4.1")

    //ZipLib
    implementation("org.zeroturnaround:zt-zip:1.12")

    //Dhizuku
    implementation("io.github.iamr0s:Dhizuku-API:2.4")

    //Shizuku
    compileOnly("dev.rikka.hidden:stub:4.3.2")
    implementation("dev.rikka.tools.refine:runtime:4.4.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    //HiddenApiBypass
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
}