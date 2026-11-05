plugins {
    id("com.android.library")
}

android {
    namespace = "com.connectsdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    useLibrary("org.apache.http.legacy")
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.gms:play-services-cast-framework:22.3.1")

    implementation(files("libs/jdom-2.0.6.1.jar"))
    implementation(files("libs/lgcast-android-lib.jar"))
    implementation(files("libs/WhisperPlay.jar"))
    implementation(files("libs/AmazonFling.jar"))

    implementation("javax.jmdns:jmdns:3.4.1")
    implementation("com.yanzhenjie.apache:httpcore:4.4.9")
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    implementation("com.googlecode.plist:dd-plist:1.23")
    implementation("com.nimbusds:srp6a:2.1.0")
    implementation("net.i2p.crypto:eddsa:0.3.0")
    implementation("com.koushikdutta.async:androidasync:2.2.1")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("org.greenrobot:eventbus:3.3.1")
}