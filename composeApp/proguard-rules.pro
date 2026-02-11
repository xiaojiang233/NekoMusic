# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationException
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.** { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }

# Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep data classes (often needed for JSON parsing if not using @Serializable strictly or reflection)
-keep class top.xiaojiang233.nekomusic.model.** { *; }

# Enum entries
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

