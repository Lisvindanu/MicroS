plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("io.micronaut.application") version "4.5.3"
    id("com.gradleup.shadow") version "8.3.6"
    // Comment out test-resources plugin karena butuh Docker
    // id("io.micronaut.test-resources") version "4.5.3"
    id("io.micronaut.aot") version "4.5.3"
}

version = "0.1"
group = "com.anaphygon.streaming"

val kotlinVersion=project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    // KSP (Kotlin Symbol Processing) annotations
    ksp("io.micronaut.data:micronaut-data-processor")
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.openapi:micronaut-openapi")
    ksp("io.micronaut.security:micronaut-security-annotations")
    ksp("io.micronaut.validation:micronaut-validation-processor")

    // Core Micronaut dependencies
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut:micronaut-management")

    // Database & JPA (MySQL + Hibernate)
    implementation("io.micronaut.data:micronaut-data-hibernate-jpa")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    runtimeOnly("com.mysql:mysql-connector-j")

    // Database migrations (Flyway) - disabled untuk development awal
    implementation("io.micronaut.flyway:micronaut-flyway")
    runtimeOnly("org.flywaydb:flyway-mysql")

    // Messaging - comment out untuk development awal
    // implementation("io.micronaut.kafka:micronaut-kafka")

    // Kotlin support
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Monitoring & Metrics
    implementation("io.micronaut.micrometer:micronaut-micrometer-core")
    implementation("io.micronaut.micrometer:micronaut-micrometer-registry-prometheus")

    // Caching - comment out untuk development awal
    // implementation("io.micronaut.redis:micronaut-redis-lettuce")

    // Security
    implementation("io.micronaut.security:micronaut-security-jwt")
    implementation("io.micronaut.security:micronaut-security-oauth2")

    // Distributed tracing - comment out untuk development awal
    // implementation("io.micronaut.tracing:micronaut-tracing-jaeger")

    // Validation
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.validation:jakarta.validation-api")

    // Views (Thymeleaf) - optional untuk web interface
    implementation("io.micronaut.views:micronaut-views-fieldset")
    implementation("io.micronaut.views:micronaut-views-thymeleaf")

    // OpenAPI documentation
    compileOnly("io.micronaut.openapi:micronaut-openapi-annotations")

    // Logging
    runtimeOnly("ch.qos.logback:logback-classic")

    // Testing - comment out Docker-dependent tests
    testImplementation("org.mockito:mockito-core")
    // testImplementation("org.testcontainers:junit-jupiter")
    // testImplementation("org.testcontainers:kafka")
    // testImplementation("org.testcontainers:mysql")
    // testImplementation("org.testcontainers:testcontainers")

    // AOT (Ahead of Time) compilation support
    aotPlugins(platform("io.micronaut.platform:micronaut-platform:4.8.2"))
    aotPlugins("io.micronaut.security:micronaut-security-aot")
}

application {
    mainClass = "com.anaphygon.streaming.ApplicationKt"
}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
}

graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.anaphygon.streaming.*")
    }

    // Comment out testResources karena butuh Docker
    // testResources {
    //     sharedServer = true
    // }

    aot {
        // AOT optimization settings for native compilation
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
        configurationProperties.put("micronaut.security.jwks.enabled","false")
        configurationProperties.put("micronaut.security.openid-configuration.enabled","false")
    }
}

// Docker native image configuration - keep for production deployment
tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}