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

    /**
     * Shizuku 経由で APK をインストールする
     * `pm install --bypass-low-target-sdk-block` を使用することで
     * targetSdk=21 の APK もインストール可能にする
     *
     * @param apkPath インストールする APK のファイルシステムパス
     * @return インストール結果（成功時 true）
     */
    fun installApk(apkPath: String): InstallResult {
        val command = "pm install --bypass-low-target-sdk-block -r \"$apkPath\""
        val (exitCode, output) = ShizukuHelper.runShellCommand(command)
        return if (exitCode == 0 || output.contains("Success", ignoreCase = true)) {
            InstallResult.Success
        } else {
            InstallResult.Failure(output.trim())
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
