# Keep obfuscated tables
-keepattributes LineNumberTable, SourceFile
-renamesourcefileattribute SourceFile

# Android
-keepnames class * implements android.os.Parcelable { public static final ** CREATOR; }
-dontwarn android.app.ActivityThread

# AndroidX
-keep class androidx.core.app.CoreComponentFactory { *; }

# This app
-keepnames class com.saradabar.cpadcustomizetool.** { *; }

# Benesse
-keep class jp.co.benesse.dcha.dchaservice.IDchaService { *; }

# Dhizuku
-keep class com.saradabar.cpadcustomizetool.data.service.DhizukuService { *; }
