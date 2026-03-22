package net.ryuya.dev.galaxyshutter.mute.manager.data.model

import com.google.gson.annotations.SerializedName

/**
 * GitHub Releases API のレスポンスをマッピングするデータクラス
 * エンドポイント: GET /repos/{owner}/{repo}/releases/latest
 */
data class GitHubRelease(
    /** タグ名（バージョン文字列）例: "v1.0.0" */
    @SerializedName("tag_name") val tagName: String,
    /** リリース名 */
    @SerializedName("name") val name: String,
    /** リリースノート本文（Markdown）*/
    @SerializedName("body") val body: String?,
    /** 公開日時（ISO 8601 形式）*/
    @SerializedName("published_at") val publishedAt: String,
    /** リリースに添付されたアセット一覧 */
    @SerializedName("assets") val assets: List<GitHubAsset>
)

/**
 * GitHub リリースアセット（添付ファイル）のデータクラス
 */
data class GitHubAsset(
    /** ファイル名 */
    @SerializedName("name") val name: String,
    /** ダウンロード URL */
    @SerializedName("browser_download_url") val downloadUrl: String,
    /** ファイルサイズ（バイト）*/
    @SerializedName("size") val size: Long,
    /** MIME タイプ */
    @SerializedName("content_type") val contentType: String
) {
    /** APK ファイルかどうかを判別する */
    val isApk: Boolean
        get() = name.endsWith(".apk", ignoreCase = true)
}
