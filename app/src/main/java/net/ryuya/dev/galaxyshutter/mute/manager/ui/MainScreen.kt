package net.ryuya.dev.galaxyshutter.mute.manager.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.ryuya.dev.galaxyshutter.mute.manager.data.model.GitHubRelease

/**
 * Manager アプリのメイン画面
 * 最新リリース情報表示・インストール・アップデートボタンを提供する
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: ManagerUiState,
    onInstall: () -> Unit,
    onInstallLocalApk: (android.net.Uri) -> Unit,
    onRefresh: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Galaxy Shutter Mute Manager",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "設定")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "更新")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Shizuku 状態バナー
            ShizukuStatusBanner(shizukuState = uiState.shizukuState)

            // インストール状態カード
            InstallStatusCard(
                installedVersion = uiState.installedVersion,
                latestRelease = uiState.latestRelease,
                isFetching = uiState.isFetchingRelease
            )

            // エラー表示
            uiState.fetchError?.let { error ->
                ErrorCard(message = error)
            }
            uiState.installError?.let { error ->
                ErrorCard(message = error)
            }

            // インストール進捗カード（インストール中のみ表示）
            if (uiState.installState !is InstallState.Idle && uiState.installState !is InstallState.Done) {
                InstallProgressCard(installState = uiState.installState)
            }

            // リリースノートカード
            uiState.latestRelease?.body?.let { body ->
                if (body.isNotEmpty()) {
                    ReleaseNotesCard(body = body, tagName = uiState.latestRelease.tagName)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // インストール/アップデートボタン
            val isInstalled = uiState.installedVersion != null
            val latestVersion = uiState.latestRelease?.tagName?.removePrefix("v")
            val isUpToDate = isInstalled && latestVersion != null &&
                    uiState.installedVersion == latestVersion
            val isActionInProgress = uiState.installState !is InstallState.Idle &&
                    uiState.installState !is InstallState.Done

            Button(
                onClick = onInstall,
                enabled = !isActionInProgress && uiState.latestRelease != null &&
                        !isUpToDate && uiState.shizukuState is ShizukuState.Ready,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                AnimatedContent(targetState = isInstalled, label = "buttonLabel") { installed ->
                    Text(
                        text = when {
                            isActionInProgress -> "処理中..."
                            isUpToDate -> "最新版がインストール済み"
                            installed -> "アップデート"
                            else -> "Galaxy Shutter Mute をインストール"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val apkPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    onInstallLocalApk(uri)
                }
            }

            OutlinedButton(
                onClick = { apkPickerLauncher.launch(arrayOf("application/vnd.android.package-archive")) },
                enabled = !isActionInProgress && uiState.shizukuState is ShizukuState.Ready,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ファイルから選択してインストール", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Shizuku 状態バナー Composable */
@Composable
private fun ShizukuStatusBanner(shizukuState: ShizukuState) {
    val (icon, label, containerColor) = when (shizukuState) {
        is ShizukuState.Ready -> Triple(
            Icons.Rounded.CheckCircle,
            "Shizuku 接続済み",
            MaterialTheme.colorScheme.primaryContainer
        )
        is ShizukuState.Checking -> Triple(
            Icons.Rounded.HourglassEmpty,
            "Shizuku を確認中...",
            MaterialTheme.colorScheme.surfaceVariant
        )
        else -> Triple(
            Icons.Rounded.Warning,
            "Shizuku が必要です",
            MaterialTheme.colorScheme.errorContainer
        )
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** インストール現状カード */
@Composable
private fun InstallStatusCard(
    installedVersion: String?,
    latestRelease: GitHubRelease?,
    isFetching: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "インストール状況",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("インストール済み", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = installedVersion ?: "未インストール",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (installedVersion != null)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("最新バージョン", style = MaterialTheme.typography.bodyMedium)
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = latestRelease?.tagName ?: "取得中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** インストール進捗カード */
@Composable
private fun InstallProgressCard(installState: InstallState) {
    val progress by animateFloatAsState(
        targetValue = when (installState) {
            is InstallState.Downloading -> installState.progress
            is InstallState.Installing -> 0.8f
            is InstallState.GrantingPermission -> 0.95f
            else -> 1f
        },
        label = "progress"
    )
    val label = when (installState) {
        is InstallState.Downloading -> "ダウンロード中 ${(installState.progress * 100).toInt()}%"
        is InstallState.Installing -> "インストール中..."
        is InstallState.GrantingPermission -> "権限を付与中..."
        else -> ""
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** リリースノートカード */
@Composable
private fun ReleaseNotesCard(body: String, tagName: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "リリースノート ($tagName)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            HorizontalDivider()
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** エラーカード */
@Composable
private fun ErrorCard(message: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
