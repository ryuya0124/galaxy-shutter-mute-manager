package net.ryuya.dev.galaxyshutter.mute.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.ryuya.dev.galaxyshutter.mute.manager.shizuku.ShizukuHelper
import net.ryuya.dev.galaxyshutter.mute.manager.ui.ManagerApp

/**
 * Manager アプリのエントリポイント
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Shizuku 権限結果リスナーを登録する
        ShizukuHelper.addPermissionListener()

        setContent {
            ManagerApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuHelper.removePermissionListener()
    }
}
