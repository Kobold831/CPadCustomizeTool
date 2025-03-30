-keepnames class * implements android.os.Parcelable { public static final ** CREATOR; }
-keep class com.saradabar.** { *; }
-keep class androidx.core.app.CoreComponentFactory { *; }
-keep class jp.co.benesse.dcha.dchaservice.IDchaService
-keep class jp.co.benesse.dcha.dchautilservice.IDchaUtilService
-keep class org.zeroturnaround.** { *; }
-dontwarn org.slf4j.impl.**
-keepclassmembers class * extends com.stephentuso.welcome.WelcomeActivity {
    public static java.lang.String welcomeKey();
}
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile
