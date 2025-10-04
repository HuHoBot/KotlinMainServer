plugins {
    kotlin("jvm") version "2.2.0"
    id("io.ktor.plugin") version "3.3.0"
    application
}

group = "cn.huohuas001"
version = "0.0.2"

val kotlin = "2.2.20"
val ktor = "3.3.0"
val logback = "1.4.14"
val fastjson2 = "2.0.59"
val slf4j = "2.0.13"
val sqlite = "3.42.0.0"


application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-websockets:$ktor")

    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("org.slf4j:slf4j-api:$slf4j")

    implementation("io.ktor:ktor-server-config-yaml:$ktor")
    implementation("org.xerial:sqlite-jdbc:${sqlite}")

    implementation("com.alibaba.fastjson2:fastjson2:$fastjson2")


    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin")
    testImplementation("io.ktor:ktor-server-test-host:$ktor")
}




