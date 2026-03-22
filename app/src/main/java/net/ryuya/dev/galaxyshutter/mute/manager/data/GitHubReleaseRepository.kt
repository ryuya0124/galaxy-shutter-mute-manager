package net.ryuya.dev.galaxyshutter.mute.manager.data

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.ryuya.dev.galaxyshutter.mute.manager.data.model.GitHubRelease
import net.ryuya.dev.galaxyshutter.mute.manager.data.network.RetrofitClient
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Releases API の呼び出しと APK ダウンロードを担うリポジトリ
 */
class GitHubReleaseRepository(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {

    /**
     * 設定された URL から最新リリース情報を取得する
     * @return 成功時は GitHubRelease、失敗時は例外をスロー
     */
    suspend fun getLatestRelease(): GitHubRelease = withContext(Dispatchers.IO) {
        val url = preferencesRepository.releasesUrl.first()
        RetrofitClient.apiService.getLatestRelease(url)
    }

    /**
     * APK をダウンロードしてキャッシュディレクトリに保存する
     *
     * @param downloadUrl APK の直接ダウンロード URL
     * @param onProgress ダウンロード進捗コールバック（0.0〜1.0）
     * @return ダウンロードした APK ファイル（失敗時は null）
     */
    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(context.cacheDir, "galaxy_shutter_mute.apk")
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.connect()

            val totalSize = connection.contentLengthLong
            var downloadedSize = 0L

            connection.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead
                        if (totalSize > 0) {
                            onProgress(downloadedSize.toFloat() / totalSize)
                        }
                    }
                }
            }
            connection.disconnect()
            destFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Galaxy Shutter Mute のインストール済みバージョン名を返す
     * @return バージョン名（未インストールの場合は null）
     */
    fun getInstalledVersion(): String? {
        return try {
            context.packageManager.getPackageInfo(
                "net.ryuya.dev.galaxyshutter.mute",
                0
            ).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
