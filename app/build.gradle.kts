import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// ---------------------------------------------------------------------------
// Release signing
// Credentials come from environment variables (CI) or from an uncommitted
// keystore.properties file in the project root (local builds).
// Release tasks fail when credentials are missing. There is NO debug fallback.
// ---------------------------------------------------------------------------
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.isFile) f.inputStream().use { load(it) }
}

fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: keystoreProps.getProperty(prop)?.takeIf { it.isNotBlank() }

val releaseStorePath = signingValue("TIDELIO_KEYSTORE_PATH", "storeFile")
val releaseStorePassword = signingValue("ANDROID_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("ANDROID_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("ANDROID_KEY_PASSWORD", "keyPassword")

val missingSigning: List<String> = buildList {
    if (releaseStorePath == null) add("TIDELIO_KEYSTORE_PATH / storeFile")
    else if (!rootProject.file(releaseStorePath).isFile && !file(releaseStorePath).isFile) add("keystore file at $releaseStorePath")
    if (releaseStorePassword == null) add("ANDROID_KEYSTORE_PASSWORD / storePassword")
    if (releaseKeyAlias == null) add("ANDROID_KEY_ALIAS / keyAlias")
    if (releaseKeyPassword == null) add("ANDROID_KEY_PASSWORD / keyPassword")
}

android {
    namespace = "app.tidelio"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.tidelio"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeType = "PKCS12"
            if (releaseStorePath != null) {
                val candidate = rootProject.file(releaseStorePath)
                storeFile = if (candidate.isFile) candidate else file(releaseStorePath)
            }
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            // Step 1 of the spec: verify a signed, NON-minified release first.
            // R8 and resource shrinking stay off until that verification is recorded.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        // Version-update checks need network access and are not build correctness issues.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*", "/META-INF/NOTICE*")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

val verifyReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails when release signing credentials are missing (no debug fallback)."
    doLast {
        if (missingSigning.isNotEmpty()) {
            throw GradleException(
                "Release signing credentials are missing: ${missingSigning.joinToString()}. " +
                    "Provide them via environment variables or keystore.properties. " +
                    "Release builds are never signed with the debug key.",
            )
        }
    }
}

val releaseSigningGatedTasks = setOf(
    "packageRelease",
    "bundleRelease",
    "signReleaseBundle",
    "assembleRelease",
)
tasks.matching { it.name in releaseSigningGatedTasks }.configureEach {
    dependsOn(verifyReleaseSigning)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
