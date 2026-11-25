import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    jacoco
}

android {
    namespace = "cl.duoc.rinconsur.probadores"
    compileSdk = 37

    defaultConfig {
        applicationId = "cl.duoc.rinconsur.probadores"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Firma de la versión de distribución. Las claves NO van en el repositorio:
    // se leen de keystore.properties (ignorado por git) en la raíz del proyecto.
    val propiedadesFirma = Properties().apply {
        val archivo = rootProject.file("keystore.properties")
        if (archivo.exists()) archivo.inputStream().use { load(it) }
    }
    signingConfigs {
        if (propiedadesFirma.isNotEmpty()) {
            create("distribucion") {
                storeFile = rootProject.file(propiedadesFirma.getProperty("storeFile"))
                storePassword = propiedadesFirma.getProperty("storePassword")
                keyAlias = propiedadesFirma.getProperty("keyAlias")
                keyPassword = propiedadesFirma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Cobertura de las pruebas unitarias con JaCoCo:
            // ./gradlew createDebugUnitTestCoverageReport
            enableUnitTestCoverage = true
        }
        release {
            signingConfigs.findByName("distribucion")?.let { signingConfig = it }
            // Sin ofuscar: Retrofit y Gson leen los DTO por reflexión.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)

    // Room: base de datos local (entidades, DAOs y relaciones). KSP genera el código de los DAOs.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit: cliente HTTP hacia el API Gateway de los microservicios (JSON con Gson).
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// =====================================================================
//  Cobertura de la LÓGICA con JaCoCo (pauta: 80 % o más)
//  ./gradlew coberturaLogica  → app/build/reports/jacoco/coberturaLogica/html/index.html
//  ./gradlew verificarCobertura  → falla si la cobertura de líneas baja de 80 %
//
//  Lógica = reglas de negocio (model), utilidades, repositorios remotos (Retrofit:
//  microservicios y API externa), cuentas remotas y el ViewModel de la API externa.
//  No se cuentan: pantallas Compose, navegación, tema, estados de formulario,
//  entidades/DAOs de Room (código generado) ni clases que solo envuelven APIs de
//  Android (notificaciones, vibración, imágenes).
// =====================================================================
val clasesDeLogica = fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
    include(
        "cl/duoc/rinconsur/probadores/model/**",
        "cl/duoc/rinconsur/probadores/utils/**",
        "cl/duoc/rinconsur/probadores/data/remote/**",
        "cl/duoc/rinconsur/probadores/data/externa/**",
        "cl/duoc/rinconsur/probadores/data/cuentas/CuentasRemotas*",
        "cl/duoc/rinconsur/probadores/viewmodel/Clima*"
    )
    exclude(
        "cl/duoc/rinconsur/probadores/model/FormularioEstados*",
        "cl/duoc/rinconsur/probadores/model/LoginUiState*",
        "cl/duoc/rinconsur/probadores/model/LoginErrores*",
        "cl/duoc/rinconsur/probadores/model/*UiState*",
        "cl/duoc/rinconsur/probadores/model/*Errores*",
        "cl/duoc/rinconsur/probadores/model/PasoLogin*",
        "cl/duoc/rinconsur/probadores/model/MotivoPreferente*",
        "cl/duoc/rinconsur/probadores/utils/Notificaciones*",
        "cl/duoc/rinconsur/probadores/utils/Vibracion*",
        "cl/duoc/rinconsur/probadores/utils/ImagenUtils*"
    )
}
val datosDeCobertura = layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")

tasks.register<JacocoReport>("coberturaLogica") {
    group = "verification"
    description = "Reporte de cobertura de la lógica de la app (pruebas unitarias)."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(clasesDeLogica)
    sourceDirectories.setFrom("src/main/java")
    executionData.setFrom(datosDeCobertura)
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("verificarCobertura") {
    group = "verification"
    description = "Falla si la cobertura de líneas de la lógica es menor a 80 %."
    dependsOn("coberturaLogica")
    classDirectories.setFrom(clasesDeLogica)
    sourceDirectories.setFrom("src/main/java")
    executionData.setFrom(datosDeCobertura)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
