plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.fyp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fyp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true  // ✅ ViewBinding enabled
        dataBinding = true  // ✅ DataBinding enabled
    }
}

repositories {
    google()          // ✅ Required for Google libraries
    mavenCentral()    // ✅ Required for most libraries
    maven { url = uri("https://jitpack.io") }  // ✅ Required for SceneView dependencies
}

dependencies {
    // ✅ Core AndroidX Dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")

    // ✅ Glide for Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.androidx.camera.video)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // ✅ CameraX Dependencies (1.1.0)
    implementation("androidx.camera:camera-core:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-view:1.1.0")
    implementation("androidx.camera:camera-extensions:1.1.0")

    // ✅ Firebase Dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    // ✅ Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // ✅ ML Kit Selfie Segmentation (latest beta)
    implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta4")

    // ✅ ML Kit Pose Detection
    implementation("com.google.mlkit:pose-detection:18.0.0-beta3")
    implementation("com.google.mlkit:pose-detection-accurate:18.0.0-beta3")
    implementation ("com.google.mlkit:object-detection:16.2.8")


    // For depth processing
    implementation ("org.tensorflow:tensorflow-lite:2.8.0")
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.8.0")

    // ✅ ARCore (Augmented Reality)
    implementation("com.google.ar:core:1.40.0")
    implementation("com.google.ar.sceneform.ux:sceneform-ux:1.17.1")
    implementation ("com.google.ar.sceneform:core:1.17.1")
    //implementation("io.github.sceneview:sceneview:1.20.0")

    // ✅ Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ✅ Cloudinary for Image Uploads
    implementation("com.cloudinary:cloudinary-android:2.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // ✅ Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.0")

}




