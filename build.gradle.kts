plugins {
    kotlin("jvm") version "2.1.20"
    id("org.springframework.boot") version "3.3.1" // Gère automatiquement les versions des starters
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("plugin.spring") version "1.9.24" // Permet à Spring d'ouvrir les classes Kotlin (qui sont "final" par défaut)
    kotlin("plugin.jpa") version "1.9.24" // Génère des constructeurs sans argument requis par JPA
}

group = "com.magicfinance"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    // 1. Le cœur de Spring Boot Web & Data JPA
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // 2. Kotlin support pour Jackson (très important pour le JSON streaming plus tard)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // 3. Le driver de Base de données (CockroachDB utilise le driver standard PostgreSQL)
    runtimeOnly("org.postgresql:postgresql")

    // 4. Le moteur Flyway pour l'exécution des scripts au démarrage
    implementation("org.flywaydb:flyway-core")
    // Nécessaire depuis les versions récentes de Spring Boot pour le support PostgreSQL complet
    implementation("org.flywaydb:flyway-database-postgresql")

    // 5. Outils de test (Optionnel mais recommandé)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api")
    implementation("org.glassfish.jaxb:jaxb-runtime")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-security:4.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework.boot:spring-boot-starter-cache")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(18)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}