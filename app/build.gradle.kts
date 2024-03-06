import com.android.build.gradle.internal.lint.AndroidLintTask
import com.android.build.gradle.internal.tasks.factory.dependsOn
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.licensee)
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "it.codewiththeitalians.piedone"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        namespace = "it.codewiththeitalians.piedone"
        testNamespace = "it.codewiththeitalians.piedone.test"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    testOptions {
        emulatorControl.enable = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    lint {
        lintConfig = rootProject.file("build-config/lint.xml")
        sarifReport = true
    }

    compileOptions {
        // We likely don't reeeeally need this, but hey — shiny
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }

    applicationVariants.all {
        mergeAssetsProvider.dependsOn(tasks.named("scaryEyes"))
    }
}

licensee {
    allow("Apache-2.0")
}

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }
}

detekt {
    source = files("src/main/java", "src/main/kotlin")
    config = rootProject.files("build-config/detekt.yml")
    buildUponDefaultConfig = true
}

dependencies {
    implementation(libs.androidx.lifecycle.lifecycleRuntimeKtx)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.exifInterface)
    coreLibraryDesugaring(libs.com.android.tools.desugar)

    implementation(libs.bundles.compose)
    implementation(libs.bundles.composeUiTooling)
    implementation(libs.androidx.activity.activityCompose)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.jakes.timber.timber)

    implementation(libs.kotlinx.serialization)

    debugImplementation(libs.androidx.compose.ui.uiTooling)
}

configurations.all {
    resolutionStrategy {
        force("androidx.tracing:tracing:1.1.0")
    }
}

tasks {
    val detekt = withType<Detekt> {
        // Required for type resolution
        jvmTarget = "1.8"
        reports {
            sarif {
                required.set(true)
            }
        }
    }

    val lintReportReleaseSarifOutput = project.layout.buildDirectory.file("reports/sarif/lint-results-release.sarif")
    afterEvaluate {
        // Needs to be in afterEvaluate because it's not created yet otherwise
        named<AndroidLintTask>("lintReportRelease") {
            sarifReportOutputFile.set(lintReportReleaseSarifOutput)
        }

        val staticAnalysis by registering {
            val detektRelease by getting(Detekt::class)
            val androidLintReportRelease = named<AndroidLintTask>("lintReportRelease")

            dependsOn(detekt, detektRelease, androidLintReportRelease, lintKotlin)
        }

        register<Sync>("collectSarifReports") {
            val detektRelease by getting(Detekt::class)
            val androidLintReportRelease = named<AndroidLintTask>("lintReportRelease")

            mustRunAfter(detekt, detektRelease, androidLintReportRelease, lintKotlin, staticAnalysis)

            from(detektRelease.sarifReportFile) {
                rename { "detekt-release.sarif" }
            }
            detekt.forEach {
                from(it.sarifReportFile) {
                    rename { "detekt.sarif" }
                }
            }
            from(lintReportReleaseSarifOutput) {
                rename { "android-lint.sarif" }
            }

            into("${layout.buildDirectory}/reports/sarif")

            doLast {
                logger.info("Copied ${inputs.files.files.filter { it.exists() }} into ${outputs.files.files}")
                logger.info("Output dir contents:\n${outputs.files.files.first().listFiles()?.joinToString()}")
            }
        }
    }

    // This copies the Licensee json file to the app assets folder
    register<Copy>("scaryEyes") {
        from("${layout.buildDirectory}/reports/licensee/release/artifacts.json") {
            rename { "licences.json" }
        }
        into("$projectDir/src/main/assets")
        dependsOn("licenseeRelease")
    }
}
