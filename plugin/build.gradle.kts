import cn.lalaki.pub.BaseCentralPortalPlusExtension.PublishingType

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
    signing
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("cn.lalaki.central") version "1.2.3"
}

dependencies {
    implementation(libs.commons.io)
    compileOnly(libs.android.gradle)
}

group = "cn.lalaki.repack"
version = libs.versions.repackPlugin.get()

@Suppress("UnstableApiUsage")
gradlePlugin {
    website.set("https://github.com/lalakii/apk-repack-gradle-plugin")
    vcsUrl.set("https://github.com/lalakii/apk-repack-gradle-plugin")
    plugins {
        create("plugin") {
            id = "cn.lalaki.repack"
            displayName = "APK Repack Gradle Plugin"
            description = "APK Repack Gradle Plugin"
            tags.set(listOf("android"))
            implementationClass = "cn.lalaki.repack.plugin.RepackingPlugin"
        }
    }
}

kotlin {
    jvmToolchain(
        libs.versions.toolchain
            .get()
            .toInt(),
    )
}
val finalUrl = "https://github.com/lalakii/apk-repack-gradle-plugin"
signing {
    useGpgCmd()
}
afterEvaluate {
    tasks.withType(GenerateMavenPom::class.java) {
        if (this.name.startsWith("generatePomFile")
        ) {
            doFirst {
                pom.apply {
                    url = finalUrl
                    description = "Repackage the apk with seven zip and sign it."
                    licenses {
                        license {
                            name = "Apache-2.0"
                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    developers {
                        developer {
                            name = "lalakii"
                            email = "i@lalaki.cn"
                        }
                    }
                    organization {
                        name = "lalakii"
                        url = "https://lalaki.cn"
                    }
                    scm {
                        connection = "scm:git:$finalUrl"
                        developerConnection = "scm:git:$finalUrl"
                        url = finalUrl
                    }
                }
            }
        }
    }
}
val localMavenRepo = uri("./repo/")
centralPortalPlus {
    url = localMavenRepo
    username = System.getenv("TEMP_USER")
    password = System.getenv("TEMP_PASS")
    publishingType = PublishingType.USER_MANAGED
}
publishing {
    repositories {
        maven {
            url = localMavenRepo
        }
    }
    publications {
        create<MavenPublication>("CentralPortalPlus") {
            this.pom.withXml {
                val dep =
                    project.configurations.runtimeClasspath
                        .get()
                        .resolvedConfiguration.firstLevelModuleDependencies
                val root = asNode()
                val ds = root.appendNode("dependencies")
                dep.forEach {
                    val dn = ds.appendNode("dependency")
                    dn.appendNode("groupId", it.moduleGroup)
                    dn.appendNode("artifactId", it.moduleName)
                    dn.appendNode("version", it.moduleVersion)
                    dn.appendNode("scope", "runtime")
                }
            }
        }
    }
}
