import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}


android {
    namespace = "com.rk.application"
    compileSdk = 37


    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    
    signingConfigs {
        create("release") {
            val isGITHUB_ACTION = System.getenv("GITHUB_ACTIONS") == "true"
            
            val propertiesFilePath = if (isGITHUB_ACTION) {
                "/tmp/signing.properties"
            } else {
                "/home/rohit/Android/xed-signing/signing.properties"
            }
            
            val propertiesFile = File(propertiesFilePath)
            if (propertiesFile.exists()) {
                val properties = Properties()
                properties.load(propertiesFile.inputStream())
                keyAlias = properties["keyAlias"] as String?
                keyPassword = properties["keyPassword"] as String?
                storeFile = if (isGITHUB_ACTION) {
                    File("/tmp/xed.keystore")
                } else {
                    (properties["storeFile"] as String?)?.let { File(it) }
                }
                
                storePassword = properties["storePassword"] as String?
            } else {
                println("Signing properties file not found at $propertiesFilePath")
            }
        }
        getByName("debug") {
            storeFile = file(layout.buildDirectory.dir("../testkey.keystore"))
            storePassword = "testkey"
            keyAlias = "testkey"
            keyPassword = "testkey"
        }
    }
    
    
    buildTypes {
        release{
            isMinifyEnabled = false
            isCrunchPngs = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            resValue("string","app_name","ReTerminal")
        }
        debug{
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            resValue("string","app_name","ReTerminal-Debug")
        }
    }

    
    defaultConfig {
        applicationId = "com.rk.terminal"
        minSdk = 26
        targetSdk = 37
        versionCode = 10
        versionName = "1.3.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }


    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        viewBinding = true
        compose = true
        resValues = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":core:main"))
}
