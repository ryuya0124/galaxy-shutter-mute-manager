package net.ryuya.dev.galaxyshutter.mute.manager.data.network

import net.ryuya.dev.galaxyshutter.mute.manager.data.model.GitHubRelease
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * GitHub Releases API を呼び出すための Retrofit インターフェース
 */
interface GitHubApiService {

    /**
     * 指定 URL から最新リリース情報を取得する
     * URL は DataStore から読み込んだ設定値を使用する
     *
     * @param url 完全な API エンドポイント URL
     * @return GitHubRelease オブジェクト
     */
    @GET
    suspend fun getLatestRelease(@Url url: String): GitHubRelease
}
