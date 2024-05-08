import com.lustyflix.streamverse.gradle.StreamverseExtension 
import com.android.build.gradle.BaseExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        // Shitpack repo which contains our tools and dependencies
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        // Streamverse gradle plugin which makes everything work and builds plugins
        classpath("com.github.lustyflix:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.streamverse(configuration: StreamverseExtension.() -> Unit) = extensions.getByName<StreamverseExtension>("streamverse").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lustyflix.streamverse.gradle")

    streamverse {
        // when running through github workflow, GITHUB_REPOSITORY should contain current repository name
        // you can modify it to use other git hosting services, like gitlab
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/LustyFlix/Extensions")
    }

    android {

        defaultConfig {
            minSdk = 21
            compileSdkVersion(33)
            targetSdk = 33
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8" // Required
                // Disables some unnecessary features
                freeCompilerArgs = freeCompilerArgs +
                        "-Xno-call-assertions" +
                        "-Xno-param-assertions" +
                        "-Xno-receiver-assertions"
            }
        }
    }

    dependencies {
        val apk by configurations
        val implementation by configurations

        // Stubs for all Streamverse classes
        apk("com.lustyflix:streamverse:pre-release")

        // these dependencies can include any of those which are added by the app,
        // but you dont need to include any of them if you dont need them
        // https://github.com/lustyflix/streamverse/blob/master/app/build.gradle
        implementation(kotlin("stdlib")) // adds standard kotlin features
        implementation("com.github.Blatzar:NiceHttp:0.4.4") // http library
        implementation("org.jsoup:jsoup:1.17.1") // html parser
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
        implementation("io.karn:khttp-android:0.1.2")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
