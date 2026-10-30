buildscript {
    // Keep vulnerable transitive build-tool dependencies on their patched releases.
    configurations.getByName("classpath").resolutionStrategy.force(
        "io.netty:netty-buffer:4.1.139.Final",
        "io.netty:netty-codec:4.1.139.Final",
        "io.netty:netty-codec-http:4.1.139.Final",
        "io.netty:netty-codec-http2:4.1.139.Final",
        "io.netty:netty-codec-socks:4.1.139.Final",
        "io.netty:netty-common:4.1.139.Final",
        "io.netty:netty-handler:4.1.139.Final",
        "io.netty:netty-handler-proxy:4.1.139.Final",
        "io.netty:netty-resolver:4.1.139.Final",
        "io.netty:netty-transport:4.1.139.Final",
        "io.netty:netty-transport-native-unix-common:4.1.139.Final",
        "org.bouncycastle:bcpkix-jdk18on:1.85",
        "org.bouncycastle:bcprov-jdk18on:1.85",
        "org.bouncycastle:bcutil-jdk18on:1.85",
        "com.google.protobuf:protobuf-java:3.25.5",
        "com.google.protobuf:protobuf-kotlin:3.25.5",
        "io.opentelemetry:opentelemetry-api:1.67.0",
        "io.opentelemetry:opentelemetry-context:1.67.0",
        "org.apache.commons:commons-lang3:3.18.0",
        "org.apache.httpcomponents:httpclient:4.5.14",
        "org.bitbucket.b_c:jose4j:0.9.6",
        "org.jdom:jdom2:2.0.6.1",
    )
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
