plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.alayaapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.alayaapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Keep false for now, enable later with proper rules
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Enable ViewBinding
    viewBinding {
        enable = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // RecyclerView is used, ensure it's here (it's implicitly included via material/appcompat usually, but good to be explicit if needed)
    // implementation("androidx.recyclerview:recyclerview:1.3.2") // Example version, adjust if necessary

    // --- Add Gson dependency ---
    implementation("com.google.code.gson:gson:2.10.1") // Use the latest version as needed

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}