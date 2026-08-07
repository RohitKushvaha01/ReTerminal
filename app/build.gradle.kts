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
            if (propertiesFile.exists() && propertiesFile.length() > 0) {
                runCatching {
                    val properties = Properties()
                    properties.load(propertiesFile.inputStream())
                    val alias = properties["keyAlias"] as String?
                    val keyPass = properties["keyPassword"] as String?
                    val storePass = properties["storePassword"] as String?
                    
                    if (!alias.isNullOrBlank() && !keyPass.isNullOrBlank() && !storePass.isNullOrBlank()) {
                        keyAlias = alias
                        keyPassword = keyPass
                        storeFile = if (isGITHUB_ACTION) {
                            File("/tmp/xed.keystore")
                        } else {
                            (properties["storeFile"] as String?)?.let { File(it) }
                        }
                        storePassword = storePass
                    } else {
                        // Fallback to testkey
                        storeFile = file(layout.buildDirectory.dir("../testkey.keystore"))
                        storePassword = "testkey"
                        keyAlias = "testkey"
                        keyPassword = "testkey"
                        println("Signing properties are missing required fields. Using testkey fallback.")
                    }
                }.onFailure {
                    // Fallback to testkey
                    storeFile = file(layout.buildDirectory.dir("../testkey.keystore"))
                    storePassword = "testkey"
                    keyAlias = "testkey"
                    keyPassword = "testkey"
                    println("Failed to load signing properties. Using testkey fallback.")
                }
            } else {
                // Fallback to testkey
                storeFile = file(layout.buildDirectory.dir("../testkey.keystore"))
                storePassword = "testkey"
                keyAlias = "testkey"
                keyPassword = "testkey"
                println("Signing properties file not found or empty. Using testkey fallback.")
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
