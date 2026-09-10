import java.util.Properties

plugins {
    id("com.android.application")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.diretornoouvido.app"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.diretornoouvido.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        
        buildConfigField("String", "APP_TOKEN", "\"${localProperties.getProperty("APP_TOKEN", "")}\"")
        buildConfigField("String", "BACKEND_URL", "\"https://diretor-no-ouvido-tts-464833730167.southamerica-east1.run.app\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-video:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    
    // Google Cloud TTS via HTTP
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    
    // Room para persistência local
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    
    // LiveData para reatividade
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
