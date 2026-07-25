import java.io.FileInputStream
import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.ksp)
}

// local.properties(깃에 안 올라가는 파일)에서 keystore 정보를 읽어온다. 없으면 그냥 기본 디버그
// 서명으로 빌드된다(에러 안 남) — 이 값들은 아무도 커밋 안 하니 안전하다.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
	localProperties.load(FileInputStream(localPropertiesFile))
}
val releaseKeystorePath: String? = localProperties.getProperty("RELEASE_KEYSTORE_PATH")

android {
	namespace = "com.chan.bnote"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.chan.bnote"
		minSdk = 24
		targetSdk = 35
		versionCode = 2
		versionName = "1.1"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	signingConfigs {
		if (releaseKeystorePath != null) {
			create("release") {
				storeFile = file(releaseKeystorePath)
				storePassword = localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
				keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
				keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
			}
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			if (releaseKeystorePath != null) {
				signingConfig = signingConfigs.getByName("release")
			}
		}
		debug {
			// local.properties에 keystore 정보가 있으면, 디버그(Android Studio 실행 버튼)도
			// 같은 keystore로 서명해서 기기에 깔린 배포용 앱 위에 그냥 덮어써지게 한다.
			// 정보가 없으면 원래처럼 기본 디버그 서명을 그대로 쓴다.
			if (releaseKeystorePath != null) {
				signingConfig = signingConfigs.getByName("release")
			}
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
		viewBinding = true   // 추가: findViewById 대신 ViewBinding 사용
	}
}

dependencies {

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.splashscreen)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)

	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	ksp(libs.androidx.room.compiler)

	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)

	implementation(libs.androidx.recyclerview)
	implementation(libs.androidx.viewpager2)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.flexbox)
	implementation(libs.coil)
	implementation(libs.androidx.work.runtime.ktx)
}