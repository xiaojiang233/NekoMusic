import org.gradle.kotlin.dsl.implementation
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.compose.webview.multiplatform)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.session)
            implementation(libs.androidx.media3.ui)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)

            // JavaFX dependencies
            val javafxVersion = "21.0.1"
            val osName = System.getProperty("os.name").lowercase()
            val platform = when {
                osName.contains("win") -> "win"
                osName.contains("mac") -> "mac"
                osName.contains("linux") -> "linux"
                else -> "linux"
            }

            listOf("base", "graphics", "controls", "swing", "media").forEach { module ->
                implementation("org.openjfx:javafx-$module:$javafxVersion:$platform")
            }

            // Global Hotkeys for SMTC (Best effort)
            // implementation("com.github.kwhat:jnativehook:2.2.2")
        }
    }
}

android {
    namespace = "top.xiaojiang233.nekomusic"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "top.xiaojiang233.nekomusic"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "top.xiaojiang233.nekomusic.MainKt"

        // 确保所有运行时依赖（包括 SPI）都被包含
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)

            packageName = "NekoMusic"
            packageVersion = "1.0.0"
            description = "第三方音乐播放器"
            copyright = "© 2026 xiaojiang233"
            vendor = "xiaojiang233"

            modules(
                "java.sql",
                "jdk.unsupported",
                "java.management",
                "java.naming",
                "java.prefs",
                "jdk.crypto.ec",
                "java.desktop",
                "java.logging"  // coroutines 需要
            )

            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                menuGroup = "NekoMusic"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                perUserInstall = true
                dirChooser = true
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
                packageName = "nekomusic"
                menuGroup = "AudioVideo"
                appCategory = "Audio"
            }

            jvmArgs += listOf(
                "-Dskiko.renderApi=OPENGL",
                "-Xmx512m",
                "-Dnekomusic.log.dir=\${user.home}/.nekomusic/logs"
            )
        }
    }
}

// Convenience tasks
tasks.register("packageWindows") {
    group = "distribution"
    description = "Build Windows MSI"
    dependsOn("packageMsi")
}

tasks.register("packageLinux") {
    group = "distribution"
    description = "Build Linux DEB + RPM"
    dependsOn("packageDeb", "packageRpm")
}
