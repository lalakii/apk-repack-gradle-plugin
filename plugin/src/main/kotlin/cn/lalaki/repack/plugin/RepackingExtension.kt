package cn.lalaki.repack.plugin

import java.io.File

abstract class RepackingExtension {
    /**
     * @since 禁用插件
     */
    var disabled: Boolean? = false

    /**
     * @since 7zip的可执行文件（控制台版本），可以在：https://www.7-zip.org 下载
     */
    var sevenZip: String? = null

    /**
     * @since 通常无需设置，如果插件找不到默认生成的apk文件，可以配置此参数指定某个apk文件，值可以为null
     */
    var apkFile: File? = null

    /**
     * @since 重新打包时，自定义排除掉某些无用的文件或文件夹，按文件夹、文件名称严格匹配，不支持通配符，值可以为null
     */
    var blacklist: Array<String>? = null

    /**
     * @since 对重新打包的apk签名（值为false时，addV1Sign ，addV2Sign将不生效 ）
     */
    var resign: Boolean = false

    /**
     * @since v2签名，android7~9需要
     */
    var addV2Sign: Boolean = false

    /**
     * @since v1签名，android7以下需要
     */
    var addV1Sign: Boolean = false

    /**
     * @since 禁用v3/v4签名，默认不禁用
     */
    var disableV3V4: Boolean = false

    /**
     * @since 安静模式，默认false，会在终端输出日志
     */
    var quiet: Boolean = false
}
