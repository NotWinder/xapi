plugins {
    // Apply the Java plugin for Java projects
    application
}

repositories {
    mavenCentral()  // Use Maven Central for dependencies
}

application {
    // Specify the main class of your application
    mainClass.set("org.example.App") // Adjust the class name if needed
}

dependencies {
    // Add the JSON library dependency
    implementation("org.json:json:20240303")  // Add org.json dependency

    // Add Kotlin standard library for Kotlin DSL (Gradle uses Kotlin for scripting)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")

    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    // Add JUnit for testing (optional, in case you need it)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

