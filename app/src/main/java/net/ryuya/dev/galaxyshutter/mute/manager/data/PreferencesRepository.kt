package net.ryuya.dev.galaxyshutter.mute.manager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore のプロパティ委譲（スレッドセーフなシングルトン）*/
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "manager_settings")

/**
 * アプリ設定を DataStore で永続化するリポジトリ
 *
 * 保存する設定:
 * - GitHub Releases API の URL（カスタム可能）
 */
class PreferencesRepository(private val context: Context) {

    companion object {
        /** GitHub Releases API URL の設定キー */
        private val GITHUB_RELEASES_URL_KEY = stringPreferencesKey("github_releases_url")

        /**
         * デフォルトの GitHub Releases API URL
         * リリース後に実際のリポジトリ URL に変更すること
         */
        const val DEFAULT_RELEASES_URL =
            "https://api.github.com/repos/ryuya0124/galaxy-shutter-mute/releases/latest"
    }

    /** 現在設定されている GitHub Releases URL の Flow */
    val releasesUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GITHUB_RELEASES_URL_KEY] ?: DEFAULT_RELEASES_URL
    }

    /**
     * GitHub Releases URL を保存する
     * @param url 新しい GitHub Releases API URL
     */
    suspend fun saveReleasesUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[GITHUB_RELEASES_URL_KEY] = url
        }
    }
}
