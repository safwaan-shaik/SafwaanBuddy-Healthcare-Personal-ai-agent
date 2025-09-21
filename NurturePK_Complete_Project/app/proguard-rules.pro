# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Room entities and DAOs
-keep class com.nurturepk.data.** { *; }

# Keep WorkManager classes
-keep class androidx.work.** { *; }

# Keep Room classes
-keep class androidx.room.** { *; }

# Don't warn about Jetpack Compose
-dontwarn androidx.compose.**

# Keep notification classes
-keep class com.nurturepk.utils.NotificationHelper { *; }

# Keep receiver classes
-keep class com.nurturepk.receiver.** { *; }