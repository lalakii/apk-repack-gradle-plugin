# APK Repack Gradle Plugin

[![Maven Central](https://img.shields.io/maven-central/v/cn.lalaki.repack/cn.lalaki.repack.gradle.plugin.svg?label=Maven%20Central&logo=sonatype)](https://central.sonatype.com/artifact/cn.lalaki.repack/cn.lalaki.repack.gradle.plugin)
![License: Apache-2.0 (shields.io)](https://img.shields.io/badge/License-Apache--2.0-c02041?logo=apache)

为了生成体积更小的apk文件，使用[7-Zip](https://7-zip.org/)的zip算法重新打包apk文件，并且可以通过配置参数让一些文件不要打包到apk

### 如何使用，参考：[app/build.gradle.kts 配置示例](https://github.com/lalakii/apk-repack-gradle-plugin/blob/master/sample/build.gradle.kts)
```dsl
plugins {
  id("cn.lalaki.repack") version "3.0.1-LTS"
}

repackConfig {
  sevenZip = "C:\\example\\7za.exe"
  /**
   7zip的可执行文件（Console版本），可以在：https://www.7-zip.org 下载
   对于Windows平台，通常不需要指定此参数，会自动下载依赖，
  **/

  resign = true
  // 对重新打包的apk签名（值为false时，将不会为生成的apk签名，addV1Sign ，addV2Sign也不会生效 ）

  addV1Sign = true // v1签名，android7以下需要
  addV2Sign = true // v2签名，android7~9需要（targetVersion30及以上）
  disableV3V4 = false // 禁用v3/v4签名（默认false不禁用）
  apkFile = null
  // 通常无需设置，如果插件找不到默认生成的apk文件，可以配置此参数指定某个apk文件，值可以为null

  blacklist = arrayOf("META-INF","1.example.txt","2.example.txt")
  // 打包时apk时，可以通过配置此参数排除掉某些无用的文件或文件夹，按照相对路径匹配
  // 不支持通配符，值可以为null

  quiet = false // 安静模式（默认false，将会在终端输出日志）
  disabled = false // 禁用此插件（默认false，不禁用）
}
```

配置完成后，直接生成apk，之后终端将会打印日志

```console
> Task :sample:repacking
[ APK Repacking Task Started!#3784 ]    // 这里的数字是当前进程的pid

build tool: 35.0.0-rc3
7z: C:\Users\sa\Downloads\7z2404-extra\x64\7za.exe
working directory: ..\sample\release\.tmp_ac7dc02d-c43e-44d0-b442-8a24f4e3e215
blacklist: ...
zipAligned: ok
signed: ok, v1 v2 v3 v4
apk: ..\sample-release.apk  //输出的apk文件路径

[ Done ]
```

### 注意事项

* apk没有签名或者签名失败，检查是否正确配置signingConfig

  或者也可以将resign设置为false，打包完成后手动签名

### License

[APACHE LICENSE, VERSION 2.0](https://www.apache.org/licenses/LICENSE-2.0)