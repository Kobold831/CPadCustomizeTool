# Keep obfuscated tables
-keepattributes LineNumberTable, SourceFile
-renamesourcefileattribute SourceFile

# Android
-keepnames class * implements android.os.Parcelable { public static final ** CREATOR; }

# AndroidX
-keep class androidx.core.app.CoreComponentFactory { *; }

# This app
-keep class com.saradabar.cpadcustomizetool.** { *; }

# Benesse
-keep class jp.co.benesse.dcha.* { *; }
-keep class android.os.BenesseExtension { *; }

# Welcome
-keepclassmembers class * extends com.stephentuso.welcome.WelcomeActivity { public static java.lang.String welcomeKey(); }

# ZT Zip
-dontwarn org.slf4j.**
-keep class * implements org.zeroturnaround.zip.extra.ZipExtraField { public <init>(); }

-dontwarn android.app.ActivityThread
