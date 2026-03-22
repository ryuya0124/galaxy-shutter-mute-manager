package net.ryuya.dev.galaxyshutter.mute.manager.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit クライアントのシングルトンファクトリー
 * GitHub API との通信設定を行う
 */
object RetrofitClient {

    /** ベース URL（GitHub API の動的 URL を使うため最小限の値を設定）*/
    private const val BASE_URL = "https://api.github.com/"

    /** 接続・読み込みタイムアウト（秒）*/
    private const val TIMEOUT_SECONDS = 30L

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        // GitHub API はユーザーエージェントを要求する
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "GalaxyShutterMuteManager")
                .header("Accept", "application/vnd.github+json")
                .build()
            chain.proceed(request)
        }
        .build()

    val apiService: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApiService::class.java)
    }
}
