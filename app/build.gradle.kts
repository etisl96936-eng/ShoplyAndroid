plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.shoplyandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shoplyandroid"
        minSdk = 24
        targetSdk = 35 // שיניתי ל-35 כדי להתאים ל-CompileSdk
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = false // מצוין שביטלת את זה
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1") // גרסה יציבה שלא דורשת 36
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3") // גרסה יציבה שלא דורשת 36

    // RecyclerView - להצגת הרשימה
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Glide - לטעינת תמונות
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ConstraintLayout - לעיצוב
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // בדיקות
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}