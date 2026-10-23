plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.solstice.gmt"
    compileSdk = 36

    defaultConfig {
        // Watch Face Format v3 requires Wear OS 5.1 (API 35).
        applicationId = "com.solstice.gmt"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            // WFF packages contain no code, so there is nothing to minify.
            isMinifyEnabled = false
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
}
