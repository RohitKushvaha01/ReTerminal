import java.util.Properties
import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.net.URI
import java.security.DigestInputStream
import java.security.MessageDigest

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.rk.application"
    compileSdk = 36

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
        disable += "ExpiredTargetSdkVersion"
    }

    signingConfigs {
        create("release") {
            storeFile = file("testkey.keystore")
            storePassword = "testkey"
            keyAlias = "testkey"
            keyPassword = "testkey"
        }
        getByName("debug") {
            storeFile = file("testkey.keystore")
            storePassword = "testkey"
            keyAlias = "testkey"
            keyPassword = "testkey"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isCrunchPngs = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            resValue("string", "app_name", "AndLinux")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            resValue("string", "app_name", "AndLinux")
        }
    }

    defaultConfig {
        applicationId = "com.term.andlinux"
        minSdk = 26
        targetSdk = 28
        versionCode = 11
        versionName = "1.4.1"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "store"

    productFlavors {
        create("Fdroid") {
            dimension = "store"
            targetSdk = 28
        }

        create("PlayStore") {
            dimension = "store"
            targetSdk = 35
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

val prepareAndLinuxSources = tasks.register<org.gradle.api.tasks.Exec>("prepareAndLinuxSources") {
    workingDir = rootProject.projectDir
    commandLine("python3", ".github/scripts/prepare_andlinux.py")
}

fun downloadFile(localUrl: String, remoteUrl: String, expectedChecksum: String) {
    val digest = MessageDigest.getInstance("SHA-256")
    val file = File(projectDir, localUrl)

    if (file.exists()) {
        val buffer = ByteArray(8192)
        FileInputStream(file).use { input ->
            while (true) {
                val readBytes = input.read(buffer)
                if (readBytes < 0) break
                digest.update(buffer, 0, readBytes)
            }
        }
        var checksum = BigInteger(1, digest.digest()).toString(16)
        while (checksum.length < 64) checksum = "0$checksum"
        if (checksum == expectedChecksum) return

        logger.warn("Deleting old local file with wrong hash: $localUrl: expected: $expectedChecksum, actual: $checksum")
        file.delete()
    }

    logger.quiet("Downloading $remoteUrl ...")
    file.parentFile.mkdirs()
    val out = BufferedOutputStream(FileOutputStream(file))
    val connection = URI(remoteUrl).toURL().openConnection()
    val digestStream = DigestInputStream(connection.inputStream, digest)
    digestStream.transferTo(out)
    out.close()

    var checksum = BigInteger(1, digest.digest()).toString(16)
    while (checksum.length < 64) checksum = "0$checksum"
    if (checksum != expectedChecksum) {
        file.delete()
        throw GradleException("Wrong checksum for $remoteUrl:\n Expected: $expectedChecksum\n Actual:   $checksum")
    }
}

tasks.register("downloadPrebuilt") {
    doLast {
        val prootTag = "proot-2025.01.15-r2"
        val prootVersion = "5.1.107-66"
        var prootUrl = "https://github.com/termux-play-store/termux-packages/releases/download/${prootTag}/libproot-loader-ARCH-${prootVersion}.so"

        downloadFile("src/main/jniLibs/armeabi-v7a/libproot-loader.so", prootUrl.replace("ARCH", "arm"), "eb1d64e9ef875039534ce7a8eeffa61bbc4c0ae5722cb48c9112816b43646a3e")
        downloadFile("src/main/jniLibs/arm64-v8a/libproot-loader.so", prootUrl.replace("ARCH", "aarch64"), "8814b72f760cd26afe5350a1468cabb6622b4871064947733fcd9cd06f1c8cb8")
        downloadFile("src/main/jniLibs/x86_64/libproot-loader.so", prootUrl.replace("ARCH", "x86_64"), "1a52cc9cc5fdecbf4235659ffeac8c51e4fefd7c75cc205f52d4884a3a0a0ba1")
        prootUrl = "https://github.com/termux-play-store/termux-packages/releases/download/${prootTag}/libproot-loader32-ARCH-${prootVersion}.so"
        downloadFile("src/main/jniLibs/arm64-v8a/libproot-loader32.so", prootUrl.replace("ARCH", "aarch64"), "ff56a5e3a37104f6778420d912e3edf31395c15d1528d28f0eb7d13a64481b99")
        downloadFile("src/main/jniLibs/x86_64/libproot-loader32.so", prootUrl.replace("ARCH", "x86_64"), "5460a597e473f57f0d33405891e35ca24709173ca0a38805d395e3544ab8b1b4")
    }
}

afterEvaluate {
    listOf(
        "preFdroidReleaseBuild",
        "preFdroidDebugBuild",
        "prePlayStoreReleaseBuild",
        "prePlayStoreDebugBuild"
    ).forEach { taskName ->
        tasks.findByName(taskName)?.dependsOn(prepareAndLinuxSources)
    }

    android.applicationVariants.all { variant ->
        variant.javaCompileProvider.dependsOn("downloadPrebuilt")
        true
    }
}

dependencies {
    implementation(project(":core:main"))
}
