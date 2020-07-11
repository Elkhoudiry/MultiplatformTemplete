plugins {
    id("com.android.library")
    id("kotlin-native-cocoapods")
    kotlin("multiplatform")
    kotlin("plugin.serialization") version  "1.3.72"
}

android {
    compileSdkVersion(30)
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
        setMultiDexEnabled(true)
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
    }
    packagingOptions {
        pickFirst("META-INF/kotlinx-serialization-runtime.kotlin_module")
        pickFirst("META-INF/AL2.0")
        pickFirst("META-INF/LGPL2.1")
        pickFirst("androidsupportmultidexversion.txt")
    }
}

kotlin {

    cocoapods {
        summary = "some_summary"
        homepage = "www.github.com"
    }

    android()

    js {
        val main by compilations.getting {
            kotlinOptions {
                moduleKind = "commonjs"
            }
        }
        browser {
        }
        nodejs {
        }
    }

    val iOSTarget: (String, org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.() -> Unit) -> org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget =
            if (System.getenv("SDK_NAME")?.startsWith("iphoneos") == true)
                ::iosArm64
            else
                ::iosX64

    val iosBuild = iOSTarget("ios") {
        binaries {

        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions.freeCompilerArgs += listOf(
                "-Xuse-experimental=kotlin.Experimental",
                "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-Xuse-experimental=kotlinx.serialization.ImplicitReflectionSerializer"
        )
    }

    sourceSets["commonMain"].dependencies {
        api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:0.20.0")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:1.3.7")
    }

    sourceSets["androidMain"].dependencies {
        api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
        implementation("org.jetbrains.kotlin:kotlin-stdlib")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.7")
    }

    sourceSets["jsMain"].dependencies {
        api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.20.0")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.7")
    }

    sourceSets["iosMain"].dependencies {
        api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:0.20.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.3.7")
    }

//    sourceSets {
//        configure(listOf(iosBuild)) {
//            compilations.getByName("main") {
//                source(sourceSets.get("iosMain"))
//                val firebasefirestore by cinterops.creating {
//                    packageName("cocoapods.FirebaseFirestore")
//                    defFile(file("$projectDir/src/iosMain/c_interop/FirebaseFirestore.def"))
//                    includeDirs(
//                        "$projectDir/../native/iosApp/Pods/FirebaseFirestore/Firestore/Source/Public",
//                        "$projectDir/../native/iosApp/Pods/FirebaseCore/Firebase/Core/Public"
//                    )
//                    compilerOpts("-F$projectDir/src/iosMain/c_interop/modules/FirebaseFirestore")
//                }
//            }
//        }
//    }
}


val packForXcode by tasks.creating(Sync::class) {
    val targetDir = File(buildDir, "xcode-frameworks")

    /// selecting the right configuration for the iOS
    /// framework depending on the environment
    /// variables set by Xcode build
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val framework = kotlin.targets
            .getByName<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>("ios")
            .binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)

    from({ framework.outputDirectory })
    into(targetDir)

    /// generate a helpful ./gradlew wrapper with embedded Java path
    doLast {
        val gradlew = File(targetDir, "gradlew")
        gradlew.writeText(
                "#!/bin/bash\n"
                        + "export 'JAVA_HOME=${System.getProperty("java.home")}'\n"
                        + "cd '${rootProject.rootDir}'\n"
                        + "./gradlew \$@\n"
        )
        gradlew.setExecutable(true)
    }
}

tasks.getByName("build").dependsOn(packForXcode)