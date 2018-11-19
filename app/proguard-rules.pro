# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Android Studio.app/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# To debug, produce a mapping file
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-keepclassmembers class **.R$* {public static <fields>;}
-keep class **.R$*

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# For making SSL bumping code let us build with ProGuard
-dontwarn org.sandrop.**
-dontwarn org.sandroproxy.**
-dontwarn org.sandrob.**

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class edu.uci.calit2.anteater.client.android.activity.VisualizationActivity {
   public *;
}
