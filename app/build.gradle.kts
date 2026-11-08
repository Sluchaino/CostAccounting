plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.costaccounting.mobile"
    compileSdk = 36
    defaultConfig {
        applicationId = "ru.costaccounting.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.4.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    testOptions { animationsDisabled = true }
}

tasks.withType<Test>().configureEach { maxHeapSize = "256m"; maxParallelForks = 1; jvmArgs("-XX:ActiveProcessorCount=1") }

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
