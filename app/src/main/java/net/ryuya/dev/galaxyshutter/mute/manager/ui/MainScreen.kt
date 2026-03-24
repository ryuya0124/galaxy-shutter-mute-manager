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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.ryuya.dev.galaxyshutter.mute.manager.R
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
    onGrantPermissionOnly: () -> Unit,
    onRefresh: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_manager),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding() + 16.dp))

            // Shizuku 状態バナー
            ShizukuStatusBanner(
                shizukuState = uiState.shizukuState,
                onRequestPermission = onRequestShizukuPermission
            )

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
            val isUpToDate = isInstalled && latestVersion != null && run {
                val v1Parts = uiState.installedVersion!!.split(".").map { it.toIntOrNull() ?: 0 }
                val v2Parts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
                val maxLen = maxOf(v1Parts.size, v2Parts.size)
                var result = 0
                for (i in 0 until maxLen) {
                    val p1 = v1Parts.getOrElse(i) { 0 }
                    val p2 = v2Parts.getOrElse(i) { 0 }
                    if (p1 != p2) {
                        result = p1.compareTo(p2)
                        break
                    }
                }
                result >= 0
            }
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
                            isActionInProgress -> stringResource(R.string.install_processing)
                            isUpToDate -> stringResource(R.string.install_up_to_date)
                            installed -> stringResource(R.string.install_update)
                            else -> stringResource(R.string.install_app)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            if (isInstalled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onGrantPermissionOnly,
                    enabled = !isActionInProgress && uiState.shizukuState is ShizukuState.Ready,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.VpnKey, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.grant_permission_only), style = MaterialTheme.typography.labelLarge)
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
                Text(stringResource(R.string.install_from_file), style = MaterialTheme.typography.labelLarge)
            }
            
            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 16.dp))
        }
    }
}

/** Shizuku 状態バナー Composable */
@Composable
private fun ShizukuStatusBanner(
    shizukuState: ShizukuState,
    onRequestPermission: () -> Unit
) {
    val (icon, label, containerColor) = when (shizukuState) {
        is ShizukuState.Ready -> Triple(
            Icons.Rounded.CheckCircle,
            stringResource(R.string.shizuku_ready),
            MaterialTheme.colorScheme.primaryContainer
        )
        is ShizukuState.Checking -> Triple(
            Icons.Rounded.HourglassEmpty,
            stringResource(R.string.shizuku_checking),
            MaterialTheme.colorScheme.surfaceVariant
        )
        is ShizukuState.PermissionRequired -> Triple(
            Icons.Rounded.Warning,
            stringResource(R.string.shizuku_permission_required),
            MaterialTheme.colorScheme.errorContainer
        )
        is ShizukuState.Denied -> Triple(
            Icons.Rounded.Error,
            stringResource(R.string.shizuku_denied),
            MaterialTheme.colorScheme.errorContainer
        )
        else -> Triple(
            Icons.Rounded.Warning,
            stringResource(R.string.shizuku_not_running),
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
            if (shizukuState is ShizukuState.PermissionRequired || shizukuState is ShizukuState.Denied) {
                TextButton(
                    onClick = onRequestPermission,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(stringResource(R.string.shizuku_grant_permission), fontWeight = FontWeight.Bold)
                }
            }
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
                text = stringResource(R.string.install_status),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.installed_version), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = installedVersion ?: stringResource(R.string.not_installed),
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
                Text(stringResource(R.string.latest_version), style = MaterialTheme.typography.bodyMedium)
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = latestRelease?.tagName ?: stringResource(R.string.fetching),
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
        is InstallState.Downloading -> stringResource(R.string.downloading, (installState.progress * 100).toInt())
        is InstallState.Installing -> stringResource(R.string.installing)
        is InstallState.GrantingPermission -> stringResource(R.string.granting_permission)
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
                text = stringResource(R.string.release_notes, tagName),
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
