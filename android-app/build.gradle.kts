plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val streamDeckVersionCode = providers.gradleProperty("STREAMDECK_VERSION_CODE").orNull?.let {
    it.toIntOrNull() ?: throw GradleException("STREAMDECK_VERSION_CODE must be an integer")
} ?: 1
val streamDeckVersionName = providers.gradleProperty("STREAMDECK_VERSION_NAME").orNull
    ?.takeIf { it.isNotBlank() } ?: "1.0.0"
if (streamDeckVersionCode <= 0) throw GradleException("STREAMDECK_VERSION_CODE must be positive")
val releaseStoreFile = providers.gradleProperty("STREAMDECK_RELEASE_STORE_FILE").orNull
    ?: providers.environmentVariable("STREAMDECK_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.gradleProperty("STREAMDECK_RELEASE_STORE_PASSWORD").orNull
    ?: providers.environmentVariable("STREAMDECK_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.gradleProperty("STREAMDECK_RELEASE_KEY_ALIAS").orNull
    ?: providers.environmentVariable("STREAMDECK_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.gradleProperty("STREAMDECK_RELEASE_KEY_PASSWORD").orNull
    ?: providers.environmentVariable("STREAMDECK_RELEASE_KEY_PASSWORD").orNull
val releaseSigningValues = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
val releaseSigningConfigured = releaseSigningValues.all { !it.isNullOrBlank() }
if (releaseSigningValues.any { !it.isNullOrBlank() } && !releaseSigningConfigured) {
    throw GradleException("Release signing requires all four STREAMDECK_RELEASE_* values")
}
if (releaseSigningConfigured && !file(requireNotNull(releaseStoreFile)).isFile) {
    throw GradleException("STREAMDECK_RELEASE_STORE_FILE does not point to a readable keystore")
}

android {
    namespace = "com.streamdeck.control"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.streamdeck.control"
        minSdk = 29
        targetSdk = 35
        versionCode = streamDeckVersionCode
        versionName = streamDeckVersionName
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = if (releaseSigningConfigured) signingConfigs.getByName("release") else null
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
