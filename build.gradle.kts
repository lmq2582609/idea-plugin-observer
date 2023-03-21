plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.5.2"
}

group = "com.china"
version = "1.3"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2020.2")
    //type.set("IC") // Target IDE Platform
    type.set("IU") // Target IDE Platform
    plugins.set(listOf("java"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

//    sinceBuild表示支持最低版本
//    untilBuild为空表示支持未来所有版本
    patchPluginXml {
        sinceBuild.set("202")
        untilBuild.set("")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
