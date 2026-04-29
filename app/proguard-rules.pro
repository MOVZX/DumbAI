# ============================================================================
# Dibella ProGuard Rules
# ============================================================================

# ---------------------------------------------------------------------------
# Hilt / Dagger
# ---------------------------------------------------------------------------
-keep class **.*_MembersInjector { *; }
-keep class **.*_Factory { *; }
-keep class **.*_Bind* { *; }
-keep class **.*Binding { *; }
-keep class **.*Component { *; }
-keep class hilt.** { *; }
-keep class dagger.** { *; }
-keep class androidx.hilt.** { *; }

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase {
    *** get*();
}
-keep @androidx.room.Entity class * { *; }
-keep class androidx.room.** { *; }

# ---------------------------------------------------------------------------
# Moshi
# ---------------------------------------------------------------------------
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep class **.*JsonAdapter { *; }
-keep,allowobfuscation @interface com.squareup.moshi.*
-keep @com.squareup.moshi.JsonClass class *
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------------------------------------------------------------
# Retrofit
# ---------------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# ---------------------------------------------------------------------------
# OkHttp
# ---------------------------------------------------------------------------
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ---------------------------------------------------------------------------
# Coil
# ---------------------------------------------------------------------------
-keep class coil3.** { *; }
-dontwarn coil3.**
-dontwarn com.google.accompanist.**

# ---------------------------------------------------------------------------
# DataStore
# ---------------------------------------------------------------------------
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ---------------------------------------------------------------------------
# Media3 / ExoPlayer
# ---------------------------------------------------------------------------
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn androidx.media3.**
-dontwarn com.google.android.exoplayer2.**

# ---------------------------------------------------------------------------
# Jetpack Compose / Navigation
# ---------------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.activity.** { *; }
-keepattributes InnerClasses,EnclosingMethod
-keepclassmembers class **$$SyntheticClass {
    public static <methods>;
}

# ---------------------------------------------------------------------------
# MPV native bindings
# ---------------------------------------------------------------------------
-keep class is.xyz.mpv.** { *; }

# ---------------------------------------------------------------------------
# Dibella Models and API
# ---------------------------------------------------------------------------
-keep class org.movzx.dibella.model.** { *; }
-keep interface org.movzx.dibella.api.** { *; }

# ---------------------------------------------------------------------------
# General
# ---------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers class **.R$* {
    public static <fields>;
}
