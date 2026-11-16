plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.gpsalarm.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.gpsalarm.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 + resource shrinking make the app smaller and faster to start and run. Signed with the
            // debug key for now so a release build can be installed for testing; swap in a real key to ship.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // False positive: this app uses ComponentActivity and has no Fragments.
        disable += "InvalidFragmentVersionForActivityResult"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.location)
    implementation(libs.androidx.core.splashscreen)
    // Installs the ahead-of-time compiled profiles (ours and Compose's) so the first frames aren't interpreted.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.json.test)
}
