# Add project specific ProGuard / R8 optimization rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Preserve source file and line numbers for symbolicated crash reports while hiding raw paths
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# R8 / ProGuard Optimization Passes
-optimizationpasses 5
-allowaccessmodification

# Strip verbose and debug Android Log calls in release builds to save CPU & string allocations
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Keep Room Database entities, DAOs, and generated implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.example.data.local.** { *; }

# Keep Domain & UI Immutable Models for fast reflection-free access and serialization
-keep class com.example.model.** { *; }

# Keep Foreground MusicPlaybackService & MediaButtonReceiver entry points
-keep class com.example.service.MusicPlaybackService
-keep class androidx.media.session.MediaButtonReceiver

# Kotlin Coroutines & FastServiceLoader optimizations
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-assumenosideeffects class kotlinx.coroutines.DebugKt {
    java.lang.String getDEBUG_PROPERTY_NAME();
}
