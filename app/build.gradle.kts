import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Keys and settings come from local.properties (never uploaded to GitHub).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun localProperty(name: String, default: String = ""): String =
    localProperties.getProperty(name, default).trim()

// Text placed inside a Java String in BuildConfig, with \ and " escaped
fun javaString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "org.me.gcu.wierzbinski_kamil_s2433508"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.me.gcu.wierzbinski_kamil_s2433508"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TS_CLIENT_ID", javaString(localProperty("TS_CLIENT_ID")))
        buildConfigField("String", "TS_CLIENT_KEY", javaString(localProperty("TS_CLIENT_KEY")))

        // Google Maps key goes into the manifest (meta-data com.google.android.geo.API_KEY)
        manifestPlaceholders["MAPS_API_KEY"] = localProperty("MAPS_API_KEY")
        buildConfigField("boolean", "HAS_MAPS_KEY", localProperty("MAPS_API_KEY").isNotEmpty().toString())
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            // Development only: example XML files instead of the live feed, shorter update interval
            buildConfigField("boolean", "USE_SAMPLE_DATA",
                localProperty("USE_SAMPLE_DATA", "false").equals("true", ignoreCase = true).toString())
            buildConfigField("int", "REFRESH_MINUTES",
                (localProperty("REFRESH_MINUTES", "60").toIntOrNull() ?: 60).toString())
        }
        getByName("release") {
            // The submitted app always uses live data and updates every hour
            buildConfigField("boolean", "USE_SAMPLE_DATA", "false")
            buildConfigField("int", "REFRESH_MINUTES", "60")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.fragment)
    implementation(libs.recyclerview)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}