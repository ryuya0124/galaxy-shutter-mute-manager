package net.ryuya.dev.galaxyshutter.mute.manager.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Manager アプリ用 Shizuku ヘルパー
 * インストール・権限付与操作を Shizuku のシェル権限で実行する
 */
object ShizukuHelper {

    private const val REQUEST_CODE_SHIZUKU = 2001

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, _ -> /* ViewModel で状態を再評価 */ }

    fun addPermissionListener() {
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    fun removePermissionListener() {
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    /** Shizuku サービスが動作中かを返す */
    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }

    /** 本アプリが Shizuku 権限を持っているかを返す */
    fun hasShizukuPermission(): Boolean {
        if (Shizuku.isPreV11()) return false
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    /** Shizuku 権限をリクエストする */
    fun requestShizukuPermission() {
        if (!Shizuku.isPreV11() && !Shizuku.shouldShowRequestPermissionRationale()) {
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        }
    }

    /**
     * Shizuku 経由でシェルコマンドを実行する
     * @return コマンドの標準出力文字列（失敗時は null）
     */
    fun runShellCommand(command: String): Pair<Int, String> {
        return try {
            val process = Shizuku.newProcess(
                arrayOf("sh", "-c", command),
                null,
                null
            )
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            process.destroy()
            Pair(exitCode, output)
        } catch (e: Exception) {
            Pair(-1, e.message ?: "")
        }
    }
}
