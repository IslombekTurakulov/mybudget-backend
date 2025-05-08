plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

group = "ru.iuturakulov.mybudgetbackend"
version = System.getenv("IMAGE_TAG") ?: "1.0.0"
application {
    mainClass.set("ru.iuturakulov.mybudgetbackend.ApplicationKt")
    project.setProperty("mainClassName", "ru.iuturakulov.mybudgetbackend.ApplicationKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.call.id)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.request.validation)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    implementation(libs.flyway.core)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.kotlin.datetime)

    implementation(libs.postgresql)
    implementation(libs.hikari)

    implementation(libs.bcrypt)
    implementation(libs.kotlinx.datetime)
    implementation(libs.commons.email)
    implementation(libs.valiktor.core)
    implementation(libs.commons.io)

    implementation(libs.ktor.swagger.ui)
    implementation(libs.swagger.parser)

    implementation(libs.koin.ktor)
    implementation(libs.koin.core)
    implementation(libs.koin.logger)

    // CSV‑экспорт
    implementation("com.opencsv:opencsv:5.9")
    // PDF‑экспорт (iText 7)
    implementation("com.itextpdf:itext7-core:8.0.0")
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.google.zxing:javase:3.5.1")

    implementation("org.jfree:jfreechart:1.5.0")

    // firebase cloud messaging
    // Google Auth for FCM
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    // Ktor HTTP client for FCM
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
}
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.create("stage") {
    dependsOn("installDist")
}

tasks.shadowJar {
    manifest {
        attributes(
            "Main-Class" to "ru.iuturakulov.mybudgetbackend.ApplicationKt",
            "Implementation-Title" to "MyBudget Backend",
            "Implementation-Version" to project.version
        )
    }
    mergeServiceFiles()
    archiveBaseName.set("MyBudget-backend")
    archiveClassifier.set("all")
    archiveVersion.set("")
    
    from(project.sourceSets.main.get().output)
    from(project.sourceSets.main.get().resources)
    
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
}

tasks.named("jar") {
    enabled = false
}
tasks.named("build") {
    dependsOn("shadowJar")
}
