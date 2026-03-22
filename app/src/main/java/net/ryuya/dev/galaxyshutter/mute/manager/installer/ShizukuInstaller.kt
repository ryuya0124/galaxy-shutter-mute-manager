package net.ryuya.dev.galaxyshutter.mute.manager.installer

import net.ryuya.dev.galaxyshutter.mute.manager.shizuku.ShizukuHelper

/**
 * Shizuku 経由での APK インストールと権限付与を担うクラス
 *
 * Galaxy Shutter Mute はターゲット SDK が 21 のため、
 * `--bypass-low-target-sdk-block` オプションが必要
 */
object ShizukuInstaller {

    /** Galaxy Shutter Mute のパッケージ名 */
    private const val TARGET_PACKAGE = "net.ryuya.dev.galaxyshutter.mute"

    fun installApk(apkFile: java.io.File): InstallResult {
        return try {
            val newProcessMethod = rikka.shizuku.Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            
            val process = newProcessMethod.invoke(
                null,
                arrayOf("pm", "install", "--bypass-low-target-sdk-block", "-r", "-S", apkFile.length().toString()),
                null,
                null
            ) as Process
            
            // APKデータを標準入力に流し込むことで、権限の壁を越えてインストールする
            apkFile.inputStream().use { input ->
                process.outputStream.use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            
            val outputText = process.inputStream.bufferedReader().readText()
            val errorText = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            process.destroy()
            
            val errorOutput = (outputText + "\n" + errorText).trim()
            if (exitCode == 0 || outputText.contains("Success", ignoreCase = true)) {
                InstallResult.Success
            } else if (errorOutput.contains("INSTALL_FAILED_TEST_ONLY")) {
                InstallResult.Failure("テスト用APK（test-only）のためインストールがブロックされました。\nAndroid StudioでビルドしたDebug版APKなどをインストールする場合は、Manager側での -t オプション対応が必要になるか、Release版のAPKをご利用ください。")
            } else {
                InstallResult.Failure(errorOutput)
            }
        } catch (e: Exception) {
            InstallResult.Failure(e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * WRITE_SECURE_SETTINGS 権限を対象アプリに付与する
     * インストール完了後に呼び出す
     *
     * @return 付与結果（成功時 true）
     */
    fun grantWriteSecureSettings(): InstallResult {
        val command = "pm grant $TARGET_PACKAGE android.permission.WRITE_SECURE_SETTINGS"
        val (exitCode, output) = ShizukuHelper.runShellCommand(command)
        return if (exitCode == 0) {
            InstallResult.Success
        } else {
            InstallResult.Failure(output.trim())
        }
    }

    /**
     * 対象アプリのインストール済みバージョン名を取得する
     * @return バージョン名文字列（未インストールの場合は null）
     */
    fun getInstalledVersionName(): String? {
        val (exitCode, output) = ShizukuHelper.runShellCommand(
            "dumpsys package $TARGET_PACKAGE | grep versionName"
        )
        if (exitCode != 0) return null
        return output.trim()
            .removePrefix("versionName=")
            .trim()
            .takeIf { it.isNotEmpty() }
    }
}

/** インストール操作の結果を表す sealed class */
sealed class InstallResult {
    /** インストール成功 */
    object Success : InstallResult()

    /** インストール失敗（エラーメッセージ付き）*/
    data class Failure(val message: String) : InstallResult()
}
