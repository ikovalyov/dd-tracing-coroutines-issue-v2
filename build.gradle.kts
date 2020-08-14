import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val agentConfig = configurations.create("runtimeAgent")

plugins {
    kotlin("jvm") version "1.3.61"
    application
}

group = "dd-coroutines-test"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val ddVersion = "0.42.0"
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("com.datadoghq:dd-trace-api:$ddVersion")
    implementation("com.datadoghq:dd-trace-ot:$ddVersion")
    implementation("com.datadoghq:dd-java-agent:$ddVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.12.1")
    agentConfig("com.datadoghq", "dd-java-agent", ddVersion)
}

application {
    mainClassName = "Main"
    /*
        To disable datadog tracing you need to have set DD_TRACE_ENABLED to false
        Service name is set via DD_SERVICE_NAME
        Agent host is DD_AGENT_HOST
        port is DD_TRACE_AGENT_PORT
        More options can be checked https://docs.datadoghq.com/tracing/setup/java/ here
     */
    agentConfig.forEach {
        applicationDefaultJvmArgs += "-javaagent:${it.absolutePath}"
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
