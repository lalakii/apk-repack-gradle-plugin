package cn.lalaki.repack.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class RepackingPlugin : Plugin<Project> {
    data class BuildInfo(
        val buildType: String,
        val signingConfig: com.android.builder.model.SigningConfig,
        val outputFile: File,
    )

    private val buildInfo = mutableListOf<BuildInfo>()

    override fun apply(project: Project) {
        with(project) {
            val repack = project.extensions.create("repackConfig", RepackingExtension::class.java)
            afterEvaluate {
                with(project.tasks.register("repacking", RepackingTask::class.java)) {
                    val appExt = project.extensions.getByType(AppExtension::class.java)
                    appExt.applicationVariants.all { variant ->
                        buildInfo.add(
                            BuildInfo(
                                variant.buildType.name,
                                variant.signingConfig,
                                variant.outputs.single().outputFile,
                            ),
                        )
                    }
                    var variantBuildInfo: BuildInfo? = null
                    project.tasks.configureEach { cfg ->
                        if (variantBuildInfo == null) {
                            buildInfo.forEach { info ->
                                if (cfg.name.equals(
                                        "assemble${info.buildType}",
                                        ignoreCase = true,
                                    )
                                ) {
                                    variantBuildInfo = info
                                    cfg.finalizedBy(this)
                                }
                            }
                        }
                    }
                    configure { cfg ->
                        with(cfg) {
                            var minSdkVersion = appExt.defaultConfig.minSdk.toString()
                            if (minSdkVersion.isEmpty()) {
                                minSdkVersion =
                                    appExt.defaultConfig.minSdkVersion
                                        ?.apiLevel
                                        .toString()
                            }
                            val finalVariantBuildInfo = variantBuildInfo
                            if (finalVariantBuildInfo != null) {
                                signConfig.set(finalVariantBuildInfo.signingConfig)
                                outputFile.set(finalVariantBuildInfo.outputFile)
                            }
                            sdkHome.set(appExt.sdkDirectory)
                            sevenZip.set(repack.sevenZip)
                            apkFile.set(repack.apkFile)
                            minSdk.set(minSdkVersion)
                            resign.set(repack.resign)
                            addV1Sign.set(repack.addV1Sign)
                            addV2Sign.set(repack.addV2Sign)
                            disableV3V4.set(repack.disableV3V4)
                            blacklist.set(repack.blacklist)
                            quiet.set(repack.quiet)
                            disabled.set(repack.disabled)
                            cfg.isEnabled = !disabled.getOrElse(false)
                        }
                    }
                }
            }
        }
    }
}
