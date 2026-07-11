plugins {
    id("java")
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "ru.vtvhw.spring"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val iTextPdfVersion = "5.5.13.3"
val apachePoiVersion = "5.2.5"

val mapstructVersion = "1.5.5.Final"
val lombokMapstructBindingVersion = "0.2.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")

    // Database
    implementation("org.postgresql:postgresql")
    testImplementation("com.h2database:h2")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // MapStruct
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")

    // Экспорт
    implementation("com.itextpdf:itextpdf:$iTextPdfVersion")
    implementation("org.apache.poi:poi-ooxml:$apachePoiVersion")

    // Тесты
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("net.bytebuddy:byte-buddy-agent")
}

tasks.withType<Test> {
    useJUnitPlatform()

    jvmArgs = jvmArgs?.plus(
        listOf(
            "-javaagent:${configurations.testRuntimeClasspath.get().files.find { it.name.startsWith("byte-buddy-agent") }}"
        )
    ) ?: listOf(
        "-javaagent:${configurations.testRuntimeClasspath.get().files.find { it.name.startsWith("byte-buddy-agent") }}"
    )
}