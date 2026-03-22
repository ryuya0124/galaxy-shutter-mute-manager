package net.ryuya.dev.galaxyshutter.mute.manager.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ryuya.dev.galaxyshutter.mute.manager.data.GitHubReleaseRepository
import net.ryuya.dev.galaxyshutter.mute.manager.data.PreferencesRepository
import net.ryuya.dev.galaxyshutter.mute.manager.data.model.GitHubRelease
import net.ryuya.dev.galaxyshutter.mute.manager.installer.InstallResult
import net.ryuya.dev.galaxyshutter.mute.manager.installer.ShizukuInstaller
import net.ryuya.dev.galaxyshutter.mute.manager.shizuku.ShizukuHelper
import rikka.shizuku.Shizuku

/**
 * Manager アプリのメイン画面 ViewModel
 * インストール・アップデート・設定保存を管理する
 */
class ManagerViewModel(private val context: Context) : ViewModel() {

    private val preferencesRepository = PreferencesRepository(context)
    private val releaseRepository = GitHubReleaseRepository(context, preferencesRepository)

    /** 画面 UI 状態 */
    private val _uiState = MutableStateFlow(ManagerUiState())
    val uiState = _uiState.asStateFlow()

    /** 設定 URL の Flow（DataStore から）*/
    val releasesUrl = preferencesRepository.releasesUrl.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PreferencesRepository.DEFAULT_RELEASES_URL
    )

    /** Shizuku 権限結果コールバック */
    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                checkShizukuAndFetch()
            } else {
                _uiState.update { it.copy(shizukuState = ShizukuState.Denied) }
            }
        }

    init {
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        checkShizukuAndFetch()
    }

    override fun onCleared() {
        super.onCleared()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    /**
     * Shizuku 状態を確認してから最新リリース情報を取得する
     */
    fun checkShizukuAndFetch() {
        viewModelScope.launch {
            val shizukuState = when {
                !ShizukuHelper.isShizukuAvailable() -> ShizukuState.NotRunning
                !ShizukuHelper.hasShizukuPermission() -> ShizukuState.PermissionRequired
                else -> ShizukuState.Ready
            }
            _uiState.update { it.copy(shizukuState = shizukuState) }
            if (shizukuState == ShizukuState.Ready) {
                fetchLatestRelease()
            }
        }
    }

    /** Shizuku 権限をリクエストする */
    fun requestShizukuPermission() {
        ShizukuHelper.requestShizukuPermission()
    }

    /**
     * GitHub Releases API から最新リリース情報を取得する
     */
    fun fetchLatestRelease() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingRelease = true, fetchError = null) }
            try {
                val release = releaseRepository.getLatestRelease()
                val installedVersion = releaseRepository.getInstalledVersion()
                _uiState.update {
                    it.copy(
                        latestRelease = release,
                        installedVersion = installedVersion,
                        isFetchingRelease = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingRelease = false,
                        fetchError = "リリース情報の取得に失敗しました:\n${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * APK をダウンロードして Shizuku 経由でインストールし、
     * WRITE_SECURE_SETTINGS 権限を付与する
     */
    fun installOrUpdate() {
        val release = _uiState.value.latestRelease ?: return
        val apkAsset = release.assets.firstOrNull { it.isApk } ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    installState = InstallState.Downloading(0f),
                    installError = null
                )
            }

            // APK ダウンロード
            val apkFile = releaseRepository.downloadApk(apkAsset.downloadUrl) { progress ->
                _uiState.update { it.copy(installState = InstallState.Downloading(progress)) }
            }

            if (apkFile == null) {
                _uiState.update {
                    it.copy(
                        installState = InstallState.Idle,
                        installError = "APK のダウンロードに失敗しました"
                    )
                }
                return@launch
            }

            // インストール
            _uiState.update { it.copy(installState = InstallState.Installing) }
            when (val installResult = ShizukuInstaller.installApk(apkFile.absolutePath)) {
                is InstallResult.Success -> {
                    // WRITE_SECURE_SETTINGS 権限付与
                    _uiState.update { it.copy(installState = InstallState.GrantingPermission) }
                    when (val grantResult = ShizukuInstaller.grantWriteSecureSettings()) {
                        is InstallResult.Success -> {
                            val newInstalledVersion = releaseRepository.getInstalledVersion()
                            _uiState.update {
                                it.copy(
                                    installState = InstallState.Done,
                                    installedVersion = newInstalledVersion
                                )
                            }
                        }
                        is InstallResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    installState = InstallState.Idle,
                                    installError = "権限付与に失敗しました: ${grantResult.message}"
                                )
                            }
                        }
                    }
                    apkFile.delete()
                }
                is InstallResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            installState = InstallState.Idle,
                            installError = "インストールに失敗しました: ${installResult.message}"
                        )
                    }
                }
            }
        }
    }

    /** GitHub Releases URL を保存する */
    fun saveReleasesUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.saveReleasesUrl(url)
        }
    }

    /**
     * ローカルで選択された APK (Uri) をコピーしてインストールする
     */
    fun installLocalApk(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    installState = InstallState.Installing,
                    installError = null
                )
            }
            try {
                val tempApk = java.io.File(context.cacheDir, "temp_local_install.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempApk.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("ファイルを開けませんでした")

                when (val installResult = ShizukuInstaller.installApk(tempApk.absolutePath)) {
                    is InstallResult.Success -> {
                        _uiState.update { it.copy(installState = InstallState.GrantingPermission) }
                        when (val grantResult = ShizukuInstaller.grantWriteSecureSettings()) {
                            is InstallResult.Success -> {
                                _uiState.update { it.copy(installState = InstallState.Done) }
                            }
                            is InstallResult.Failure -> {
                                _uiState.update {
                                    it.copy(
                                        installState = InstallState.Idle,
                                        installError = "権限付与に失敗しました: ${grantResult.message}"
                                    )
                                }
                            }
                        }
                        tempApk.delete()
                    }
                    is InstallResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                installState = InstallState.Idle,
                                installError = "インストールに失敗しました: ${installResult.message}"
                            )
                        }
                        tempApk.delete()
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        installState = InstallState.Idle,
                        installError = "処理中にエラーが発生しました: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /** ViewModel ファクトリー */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManagerViewModel(context) as T
        }
    }
}

/** Manager 画面の全体 UI 状態 */
data class ManagerUiState(
    val shizukuState: ShizukuState = ShizukuState.Checking,
    val latestRelease: GitHubRelease? = null,
    val installedVersion: String? = null,
    val isFetchingRelease: Boolean = false,
    val fetchError: String? = null,
    val installState: InstallState = InstallState.Idle,
    val installError: String? = null
)

/** Shizuku の準備状態 */
sealed class ShizukuState {
    object Checking : ShizukuState()
    object Ready : ShizukuState()
    object NotRunning : ShizukuState()
    object PermissionRequired : ShizukuState()
    object Denied : ShizukuState()
}

/** インストール処理の進行状態 */
sealed class InstallState {
    object Idle : InstallState()
    data class Downloading(val progress: Float) : InstallState()
    object Installing : InstallState()
    object GrantingPermission : InstallState()
    object Done : InstallState()
}
