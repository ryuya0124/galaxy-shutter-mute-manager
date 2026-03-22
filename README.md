# Galaxy Shutter Mute Manager

<p align="center">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-blue.svg">
  <img alt="Android" src="https://img.shields.io/badge/Android-9.0%2B-green.svg">
  <img alt="OneUI" src="https://img.shields.io/badge/OneUI-1.0%20~%208.5-blue.svg">
</p>

[Galaxy Shutter Mute](https://github.com/ryuya-dev/galaxy-shutter-mute) のインストーラー兼アップデーターアプリです。

## 概要

Galaxy Shutter Mute は `targetSdkVersion=21` のため、Android 14 以降では通常の方法でインストールできません。  
このManagerアプリが代わりに以下を自動で行います：

1. GitHub Releases から最新の APK をダウンロード
2. `pm install --bypass-low-target-sdk-block` でインストール
3. `WRITE_SECURE_SETTINGS` 権限を自動付与

## 動作確認済み環境

- **OneUI 1.0 〜 OneUI 8.5** (Android 9.0 〜 Android 16)

## 必要なもの

- **[Shizuku](https://shizuku.rikka.app/)** アプリ

## インストール方法

1. [リリースページ](https://github.com/ryuya-dev/galaxy-shutter-mute-manager/releases/latest) から `galaxy-shutter-mute-manager.apk` をダウンロード
2. 通常の方法でインストール（targetSdk 制限なし）
3. アプリを起動して Shizuku の認証を許可

## 使い方

### Galaxy Shutter Mute のインストール

1. Manager アプリを起動
2. 最新リリース情報が自動で表示されます
3. 「インストール」ボタンをタップ
4. ダウンロード・インストール・権限付与が自動で完了します

### アップデート

アプリを起動すると現在インストール済みのバージョンと最新バージョンが比較されます。  
新しいバージョンがある場合は「アップデート」ボタンが表示されます。

### カスタム GitHub Releases URL の設定

設定画面（⚙️アイコン）から GitHub Releases API の URL を変更できます。  
フォークしたリポジトリや自己ホストの配布先にも対応しています。

**デフォルト URL:**
```
https://api.github.com/repos/ryuya-dev/galaxy-shutter-mute/releases/latest
```

## Galaxy Shutter Mute との連携

```
Manager                          Galaxy Shutter Mute
  │                                      │
  ├─ GitHub Releases API で最新APK取得    │
  ├─ Shizuku経由 pm install              │
  ├─ Shizuku経由 pm grant ──────────────>│ WRITE_SECURE_SETTINGS 付与
  │                                      │
  │                                      └─ Settings.System でシャッター音制御
```

| 機能 | Manager | Shutter Mute |
|---|---|---|
| インストール / アップデート | ✅ | — |
| `WRITE_SECURE_SETTINGS` 付与 | ✅ 自動 | — |
| シャッター音 ON/OFF | — | ✅ |
| GitHub Releases URL 設定 | ✅ | — |

## 技術スタック

- **Kotlin** / **Jetpack Compose** / **Material 3**
- **Shizuku API** 13.1.5
- **Retrofit2** / **OkHttp** （GitHub Releases API 通信）
- **DataStore**（設定の永続化）
- `targetSdkVersion=35`

## パッケージ名

`net.ryuya.dev.galaxyshutter.mute.manager`

## ライセンス

MIT License
