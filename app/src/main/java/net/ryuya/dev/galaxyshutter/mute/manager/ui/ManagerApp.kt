package net.ryuya.dev.galaxyshutter.mute.manager.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ryuya.dev.galaxyshutter.mute.manager.ui.theme.ManagerTheme

/**
 * Manager アプリのルート Composable
 * 画面ナビゲーション（メイン ↔ 設定）を管理する
 */
@Composable
fun ManagerApp() {
    val context = LocalContext.current
    val viewModel: ManagerViewModel = viewModel(factory = ManagerViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    val releasesUrl by viewModel.releasesUrl.collectAsState()

    // 現在表示中の画面を管理する状態
    var currentScreen by remember { mutableStateOf(Screen.Main) }

    ManagerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {
                Screen.Main -> MainScreen(
                    uiState = uiState,
                    onInstall = viewModel::installOrUpdate,
                    onInstallLocalApk = viewModel::installLocalApk,
                    onRefresh = viewModel::checkShizukuAndFetch,
                    onNavigateToSettings = { currentScreen = Screen.Settings }
                )
                Screen.Settings -> SettingsScreen(
                    currentUrl = releasesUrl,
                    onSave = { url ->
                        viewModel.saveReleasesUrl(url)
                        currentScreen = Screen.Main
                        viewModel.fetchLatestRelease()
                    },
                    onBack = { currentScreen = Screen.Main }
                )
            }
        }
    }
}

/** アプリ内の画面を表す enum */
private enum class Screen {
    Main,
    Settings
}
