import org.gradle.kotlin.dsl.libs

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.nirotem.simplecall"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nirotem.easycallandanswer"
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    flavorDimensions += "version"

    productFlavors {
        create("standard") {
            dimension = "version"
            applicationIdSuffix = ".standard"
            versionNameSuffix = "-standard"
            buildConfigField("Boolean", "IS_PREMIUM", "false")
        }

        create("premium") {
            dimension = "version"
            applicationIdSuffix = ".premium"
            versionNameSuffix = "-premium"
            buildConfigField("Boolean", "IS_PREMIUM", "true")
            //manifestPlaceholders.put("appLabel", "MyApp Premium")
        }

        create("voice") {
            dimension = "version"
            applicationIdSuffix = ".voice"
            versionNameSuffix = "-voice"
            buildConfigField("Boolean", "IS_PREMIUM", "true")
            buildConfigField("Boolean", "IS_VOICE", "true")
            //manifestPlaceholders.put("appLabel", "MyApp Premium")
        }
    }


    buildTypes {
        debug {
            isMinifyEnabled = false      // שומר את ה־debug נקי
        }
        release {
            isMinifyEnabled = false // SHOULD BE TRUE FOR PRODUCTION BUILD       // מפעיל R8 ב־release
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
        viewBinding = true
        buildConfig = true
    }

    signingConfigs {
        getByName("debug") {
            // משתמשים ב־debug keystore האוטומטי ב־$HOME/.android/debug.keystore
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        create("premium") {
            storeFile = file(project.property("MY_PREMIUM_STORE_FILE") as String)
            storePassword = project.property("MY_PREMIUM_STORE_PASSWORD") as String
            keyAlias = project.property("MY_PREMIUM_KEY_ALIAS") as String
            keyPassword = project.property("MY_PREMIUM_KEY_PASSWORD") as String
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

/*apply(from = "build-premium.gradle.kts")*/

dependencies {
    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.crashlytics.buildtools)
    "premiumImplementation"(project(":lockscreen"))
    "voiceImplementation"(project(":voicerecognition"))
    implementation(project(":sharedModules"))
    implementation(project(":subscription"))
    implementation(project(":referrals"))
    implementation(project(":userengagement"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.libphonenumber)
    implementation(libs.simpleJson)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
//    implementation(libs.googlePlayCore)
    implementation(libs.firebase.firestore)
    implementation(libs.play.update)
    implementation(libs.play.update.ktx)
/*    implementation(libs.play.review)
    implementation(libs.play.review.ktx)*/

/*   if   (gradle.startParameter.taskNames.any { it.contains("Premium") || it.contains("premium") }) {
        implementation("com.github.bumptech.glide:glide:4.15.1")
        implementation("jp.wasabeef:glide-transformations:4.3.0")
    }*/
}
