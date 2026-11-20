buildscript {
    // Constrain transitive plugin dependencies to patched releases.
    dependencies {
        constraints {
            classpath("io.netty:netty-buffer:4.2.19.Final")
            classpath("io.netty:netty-codec:4.2.19.Final")
            classpath("io.netty:netty-codec-http:4.2.19.Final")
            classpath("io.netty:netty-codec-http2:4.2.19.Final")
            classpath("io.netty:netty-codec-socks:4.2.19.Final")
            classpath("io.netty:netty-common:4.2.19.Final")
            classpath("io.netty:netty-handler:4.2.19.Final")
            classpath("io.netty:netty-handler-proxy:4.2.19.Final")
            classpath("io.netty:netty-resolver:4.2.19.Final")
            classpath("io.netty:netty-transport:4.2.19.Final")
            classpath("io.netty:netty-transport-native-unix-common:4.2.19.Final")
            classpath("org.bouncycastle:bcpkix-jdk18on:1.86")
            classpath("org.bouncycastle:bcprov-jdk18on:1.86")
            classpath("org.bouncycastle:bcutil-jdk18on:1.86")
            classpath("com.google.guava:guava:33.7.2-jre")
            classpath("com.google.protobuf:protobuf-java:4.36.2")
            classpath("com.google.protobuf:protobuf-kotlin:4.36.2")
            classpath("io.opentelemetry:opentelemetry-api:1.67.0")
            classpath("io.opentelemetry:opentelemetry-context:1.67.0")
            classpath("org.apache.commons:commons-lang3:3.21.0")
            classpath("org.apache.httpcomponents:httpclient:4.5.14")
            classpath("org.bitbucket.b_c:jose4j:0.9.7")
            classpath("org.jdom:jdom2:2.0.6.1")
        }
    }
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
}

allprojects {
    configurations.configureEach {
        resolutionStrategy.force(
            "org.bouncycastle:bcpkix-jdk18on:1.86",
            "org.bouncycastle:bcprov-jdk18on:1.86",
            "org.bouncycastle:bcutil-jdk18on:1.86",
            "com.google.guava:guava:33.7.2-jre",
            "io.opentelemetry:opentelemetry-api:1.67.0",
            "io.opentelemetry:opentelemetry-context:1.67.0",
            "org.apache.commons:commons-lang3:3.21.0",
            "org.apache.httpcomponents:httpclient:4.5.14",
        )
    }
}
