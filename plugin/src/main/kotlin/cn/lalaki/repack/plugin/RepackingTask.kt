package cn.lalaki.repack.plugin

import com.android.builder.model.SigningConfig
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.util.UUID
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

abstract class RepackingTask : DefaultTask() {
    @get:Optional
    @get:Input
    abstract val disabled: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val quiet: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val resign: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val addV1Sign: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val addV2Sign: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val disableV3V4: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val blacklist: Property<Array<String>>

    @get:Optional
    @get:Input
    abstract val sevenZip: Property<String?>

    @get:Optional
    @get:Input
    abstract val apkFile: Property<File?>

    @get:Optional
    @get:Input
    abstract val outputFile: Property<File?>

    @get:Optional
    @get:Input
    abstract val sdkHome: Property<File?>

    @get:Optional
    @get:Input
    abstract val signConfig: Property<SigningConfig?>

    @get:Optional
    @get:Input
    abstract val minSdk: Property<String?>
    private val builder = ProcessBuilder()
    private val sevenZipUrl = "https://www.7-zip.org/"
    private val sevenZipMirror =
        "https://repo1.maven.org/maven2/cn/lalaki/AndResGuard/SevenZip/1.3.8-beta01/SevenZip-1.3.8-beta01-windows-x86_64.exe"
    private var resultCode = -1

    @TaskAction
    fun main() {
        sPrintln("[ APK Repacking Task Started!#${ProcessHandle.current().pid()} ]\n")
        val sdkHome = sdkHome.orNull
        if (sdkHome == null || !sdkHome.exists()) error("missing sdk directory!")
        // find build tools
        val toolHome = File(sdkHome.absolutePath, "build-tools")
        var buildTool: File? = null
        var zipAlign: File? = null
        var apkSigner: File? = null
        val isWin = System.getenv("OS").contains("windows", ignoreCase = true)
        var alignName = "zipalign"
        var signName = "apksigner"
        if (isWin) {
            alignName = "zipalign.exe"
            signName = "apksigner.bat"
        }
        if (toolHome.exists()) {
            val buildTools = toolHome.listFiles()
            if (buildTools != null) {
                for (tool in buildTools.reversed()) {
                    if (tool.isDirectory) {
                        buildTool = tool
                        val checkZipalign = File(buildTool, alignName)
                        if (checkZipalign.exists()) {
                            zipAlign = checkZipalign
                        }
                        val checkApkSigner = File(buildTool, signName)
                        if (checkApkSigner.exists()) {
                            apkSigner = checkApkSigner
                        }
                    }
                    if (zipAlign != null && apkSigner != null) {
                        break
                    }
                }
            }
        }
        if (buildTool == null || !buildTool.exists()) error("missing build tool!")
        sPrintln("build tool: ${buildTool.name}")
        // check output apk
        val customOutput = apkFile.orNull
        var output = outputFile.orNull
        if (customOutput != null && customOutput.exists()) {
            output = customOutput
        }
        if (output == null || !output.exists()) error("missing output apk!")
        // del old sig, if exists
        val v4Sig = File("${output.absolutePath}.idsig")
        if (v4Sig.exists()) {
            v4Sig.delete()
        }
        // check 7z binary
        var a7z = sevenZip.orNull
        if (a7z == null || a7z.startsWith("http")) {
            val auto7z = Path(project.gradle.gradleUserHomeDir.absolutePath, "caches\\7za.exe")
            var currentMirror = sevenZipMirror
            if (a7z?.startsWith("http") == true) {
                currentMirror = a7z
            }
            if (!auto7z.isRegularFile()) {
                FileUtils.copyURLToFile(
                    URI
                        .create(
                            currentMirror,
                        ).toURL(),
                    auto7z.toFile(),
                )
            }
            a7z = auto7z.absolutePathString()
        }
        val a7zExe = Path(a7z)
        if (!a7zExe.isRegularFile()) error("missing 7-zip tool! please download: $sevenZipUrl")
        val a7zString = a7zExe.absolutePathString()
        sPrintln("7z: $a7zString")
        // set build dir
        val buildDir = File(output.parent, ".tmp_${UUID.randomUUID()}")
        val wdArr = buildDir.canonicalPath.split(File.separator)
        if (wdArr.isNotEmpty()) {
            var tempPath = ""
            for ((i, child) in wdArr.withIndex()) {
                if (i > wdArr.size - 4) {
                    tempPath += File.separator + child
                }
            }
            sPrintln("working directory: ..$tempPath")
        }
        // set temp apk
        val a7zApk = File(output.parent, "${buildDir.name}.apk")
        // unpack
        unPackProc(a7zString, output, buildDir)
        // apply blacklist
        val blacklist = this.blacklist.orNull
        if (blacklist != null) {
            deleteBlackListFile(buildDir, blacklist)
        }
        // repack
        val repackPath = buildDir.absolutePath + File.separator
        repackProc(a7zString, a7zApk, repackPath)
        // add resources.arsc(stored)
        val arscFile = File("${repackPath}resources.arsc")
        if (arscFile.isFile()) {
            resultCode =
                builder
                    .command(
                        a7zString,
                        "a",
                        "-bd",
                        "-mtm-",
                        "-tzip",
                        a7zApk.absolutePath,
                        "-mx0",
                        arscFile.canonicalPath,
                    ).start()
                    .waitFor()
            if (resultCode != 0) error("add resources.arsc failed!")
        }
        // backup old apk
        if (output.exists()) {
            output.copyTo(File(output.parent, "backup.apk"), true)
        }
        // zipAlign
        val alignExe = zipAlign?.absolutePath ?: error("missing zipalign tool!")
        resultCode =
            builder
                .command(
                    alignExe,
                    "-f",
                    "4",
                    a7zApk.absolutePath,
                    output.absolutePath,
                ).start()
                .waitFor()
        // clean up temp files in the working directory
        if (a7zApk.exists()) {
            a7zApk.delete()
        }
        cleanBuildDir(buildDir)
        if (resultCode != 0) error("zipAlign failed!")
        sPrintln("zipAligned: ok")
        // sign & verify
        val minSdkVersion = minSdk.getOrElse("21")
        val signConfig = this.signConfig.orNull
        if (signConfig != null && resign.orNull == true) {
            val storeFile = signConfig.storeFile
            if (storeFile != null && storeFile.exists()) {
                val signBat = apkSigner?.absolutePath ?: error("missing sign tool!")
                resultCode =
                    builder
                        .command(
                            signBat,
                            "sign",
                            "--min-sdk-version",
                            minSdkVersion,
                            "--v1-signer-name",
                            "_",
                            "--v1-signing-enabled",
                            addV1Sign.getOrElse(false).toString(),
                            "--v2-signing-enabled",
                            addV2Sign.getOrElse(false).toString(),
                            "--v3-signing-enabled",
                            (!disableV3V4.getOrElse(true)).toString(),
                            "--v4-signing-enabled",
                            (!disableV3V4.getOrElse(true)).toString(),
                            "--ks",
                            storeFile.absolutePath,
                            "--ks-key-alias",
                            signConfig.keyAlias,
                            "--ks-pass",
                            "pass:${signConfig.storePassword}",
                            "--key-pass",
                            "pass:${signConfig.keyPassword}",
                            output.absolutePath,
                        ).start()
                        .waitFor()
                if (resultCode != 0) error("sign failed!")
                val verCmd = arrayListOf(apkSigner.absolutePath, "verify", "-v")
                if (v4Sig.exists()) {
                    verCmd.add("-v4-signature-file")
                    verCmd.add(v4Sig.absolutePath)
                }
                verCmd.add(output.absolutePath)
                val verifyProc = builder.command(verCmd).start()
                resultCode = verifyProc.waitFor()
                if (resultCode != 0) error("verify failed!")
                val reader = verifyProc.inputReader()
                var schemes = ""
                val regex = Regex("(v\\S*) ")
                reader.readLines().forEach {
                    if (it.contains("true", ignoreCase = true)) {
                        val data = regex.find(it)
                        if (data != null) {
                            schemes = "$schemes${data.value}"
                        }
                    }
                }
                reader.close()
                sPrintln("signed: ok, $schemes")
            }
        } else {
            sPrintln("missing signConfig or no need sign, skip")
        }
        if (disableV3V4.get() && !addV2Sign.get() && minSdkVersion.toString().toInt() < 30) {
            unPackProc(a7zString, output, buildDir)
            repackProc(a7zString, output, repackPath)
            sPrintln("info: v1 signature only, compressed again.")
            cleanBuildDir(buildDir)
        }
        sPrintln("apk: ${output.absolutePath}")
        sPrintln("\n[ Done ]")
    }

    private fun cleanBuildDir(buildDir: File) {
        if (buildDir.exists()) {
            buildDir.deleteRecursively()
        }
    }

    private fun unPackProc(
        a7zString: String,
        output: File,
        buildDir: File,
    ) {
        resultCode =
            builder
                .command(a7zString, "x", output.absolutePath, "-o${buildDir.absolutePath}", "-y")
                .start()
                .waitFor()
        if (resultCode != 0 || buildDir.listFiles() == null) error("unpack failed!")
    }

    @OptIn(ExperimentalPathApi::class)
    private fun deleteBlackListFile(
        file: File,
        array: Array<String>,
    ) {
        val rootDir = file.absolutePath
        for (blackItem in array) {
            val deleteItem = Path(rootDir, blackItem)
            if (deleteItem.exists()) {
                if (deleteItem.isDirectory()) {
                    deleteItem.deleteRecursively()
                } else if (deleteItem.isRegularFile()) {
                    deleteItem.deleteExisting()
                }
                sPrintln(
                    "blacklist: remove the file or directory - ${
                        deleteItem.absolutePathString().replace(rootDir, "")
                    }",
                )
            }
        }
    }

    private fun repackProc(
        a7zString: String,
        a7zApk: File,
        repackPath: String,
    ) {
        resultCode =
            builder
                .command(
                    a7zString,
                    "a",
                    "-bd",
                    "-mtm-",
                    "-tzip",
                    a7zApk.absolutePath,
                    "-mfb258",
                    "-mpass=15",
                    "-mx9",
                    "$repackPath*",
                ).start()
                .waitFor()
        if (resultCode != 0) error("repack failed!")
    }

    private fun sPrintln(msg: String) {
        if (!quiet.getOrElse(false)) println(msg)
    }
}
