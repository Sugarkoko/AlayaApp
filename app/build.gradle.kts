plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
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
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // Updated
        targetCompatibility = JavaVersion.VERSION_17 // Updated
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

// app/build.gradle.kts

// ... other parts of the file ...

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(libs.play.services.location) // For FusedLocationProviderClient

    // OSMDroid
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // implementation("org.osmdroid:osmdroid-bonuspack:6.9.0") // REMOVE THIS LINE

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database")
    implementation("de.hdodenhof:circleimageview:3.1.0")
}