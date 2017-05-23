# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/Usagi/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

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

-keep public class ufro.com.mozbiisdkandroid2.MozbiiBleWrapper
-dontwarn ufro.com.mozbiisdkandroid2.MozbiiBleWrapper
-keep public interface ufro.com.mozbiisdkandroid2.OnMozbiiBatterryListener
-dontwarn ufro.com.mozbiisdkandroid2.OnMozbiiBatterryListener
-keep public interface ufro.com.mozbiisdkandroid2.OnMozbiiClickedListener
-dontwarn ufro.com.mozbiisdkandroid2.OnMozbiiClickedListener
-keep public interface ufro.com.mozbiisdkandroid2.OnMozbiiListener
-dontwarn ufro.com.mozbiisdkandroid2.OnMozbiiListener
-keepclassmembers public class ufro.com.mozbiisdkandroid2.MozbiiBleWrapper{
    public MozbiiBleWrapper(***);
    public boolean *();
    public void *();
    public void set*(***);
    public *** *();
}
-keepclassmembers public class ufro.com.mozbiisdkandroid2.OnMozbiiBatterryListener{
    void *();
    void *(***);
}
-keepclassmembers public class ufro.com.mozbiisdkandroid2.OnMozbiiClickedListener{
    void *();
}
-keepclassmembers public class ufro.com.mozbiisdkandroid2.OnMozbiiListener{
    void *();
    void *(***);
    void *(***, ***);
}