# Kotlin / Compose / Retrofit 向け ProGuard ルール

# Shizuku
-keep class rikka.shizuku.** { *; }

# Retrofit + Gson（GitHub API レスポンスのフィールドを保持）
-keepattributes Signature
-keepattributes *Annotation*
-keep class net.ryuya.dev.galaxyshutter.mute.manager.data.model.** { *; }
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
