# Compose Desktop
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Skiko
-keep class org.jetbrains.skiko.** { *; }
-dontwarn org.jetbrains.skiko.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.** { *; }

# Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# Data models
-keep class top.xiaojiang233.nekomusic.model.** { *; }

# App entry
-keep class top.xiaojiang233.nekomusic.MainKt{ *; }

# Swing/AWT (often needed for interop)
-keep class java.awt.** { *; }
-keep class javax.swing.** { *; }

# Coroutines (Java logging dependency often missed)
-keep class java.util.logging.** { *; }

