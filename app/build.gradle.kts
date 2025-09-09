plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")

}


android {
    namespace = "com.example.appbanhang"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.appbanhang"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.1.0"

        // Thông tin hiển thị/trace build
        buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")

    }
    buildFeatures {
        buildConfig = true}

//    // Tách môi trường để trỏ tới đúng google-services.json
//    flavorDimensions += "env"
//    productFlavors {
//        create("dev") {
//            dimension = "env"
//            // Quan trọng: applicationId phải khớp package_name trong google-services.json DEV
//            applicationIdSuffix = ".dev"
//            versionNameSuffix = "-dev"
//
//            // Bật emulator ở DEV
//            buildConfigField("Boolean", "USE_FIREBASE_EMULATORS", "true")
//        }
//        create("prod") {
//            dimension = "env"
//            buildConfigField("Boolean", "USE_FIREBASE_EMULATORS", "false")
//        }
//    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // Storage nếu cần:
    // implementation("com.google.firebase:firebase-storage")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
