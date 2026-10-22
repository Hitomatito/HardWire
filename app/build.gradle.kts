import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val keystoreProperties = providers.fileContents(
    rootProject.layout.projectDirectory.file("keystore.properties")
).asText.orNull?.let { text ->
    Properties().apply { load(text.reader()) }
}

fun signingValue(property: String, environmentVariable: String): String? =
    keystoreProperties?.getProperty(property)?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(environmentVariable).orNull?.takeIf { it.isNotBlank() }

val releaseKeystoreFile = signingValue("storeFile", "HARDWIRE_KEYSTORE_FILE")
    ?.let { rootProject.file(it) }
val releaseStorePassword = signingValue("storePassword", "HARDWIRE_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "HARDWIRE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "HARDWIRE_KEY_PASSWORD")
val hasReleaseSigning = releaseKeystoreFile?.isFile == true &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.hitomatito.hardwire"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hitomatito.hardwire"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
