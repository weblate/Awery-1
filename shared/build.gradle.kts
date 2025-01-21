plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.kotlin.ksp)
	alias(libs.plugins.android.library)
	alias(libs.plugins.compose)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.room)
}

room {
	schemaDirectory("${projectDir}/../schemas")
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
	jvmToolchain(17)
	applyDefaultHierarchyTemplate()
	
	compilerOptions {
		freeCompilerArgs = listOf("-Xexpect-actual-classes")
	}

	androidTarget()
	jvm("desktop")

	sourceSets {
		commonMain.dependencies {
			// Core
			implementation(projects.resources)
			implementation(projects.ext)
			implementation(compose.runtime)
			
			// Data
			implementation(libs.kotlinx.serialization.json)
			implementation(libs.androidx.room.runtime)
			
			// Ui
			implementation(compose.ui)
			implementation(compose.foundation)
			implementation(compose.components.resources)
			implementation(libs.androidx.lifecycle.viewmodel.compose)
			
			// Navigation
			implementation(libs.navigation)
			api(libs.voyager.navigator)
			api(libs.voyager.screenmodel)
			api(libs.voyager.tab.navigator)
			api(libs.voyager.transitions)

			// Adaptive layout
			implementation(libs.androidx.adaptive)
			implementation(libs.androidx.adaptive.layout)
			implementation(libs.androidx.adaptive.navigation)

			// Components
			implementation(compose.material3)
			implementation(libs.coil.compose)
		}

		androidMain.dependencies {
			implementation(libs.safeargsnext)
			implementation(libs.androidx.core)
			implementation(libs.material)
			implementation(libs.xcrash)
		}

		val desktopMain by getting {
			dependencies {
				implementation(compose.desktop.common)
			}
		}
	}
}

dependencies {
	ksp(libs.androidx.room.compiler)
}

android {
	namespace = "com.mrboomdev.awery.shared"
	compileSdk = properties["awery.sdk.target"].toString().toInt()

	defaultConfig {
		minSdk = properties["awery.sdk.min"].toString().toInt()
	}

	buildFeatures {
		buildConfig = true
	}
}

compose.resources {
	generateResClass = never
}