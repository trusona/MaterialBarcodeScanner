## notes: https://www.guardsquare.com/en/proguard/manual/examples#library
##
-forceprocessing
-target 7

-dontnote org.apache.http.params.**
-dontnote org.apache.http.conn.**
-dontnote android.net.http.**
-dontnote com.google.android.gms.common.internal.safeparcel.SafeParcelable

-keep class com.edwardvanraak.materialbarcodescanner.** { *; }
-keep interface com.edwardvanraak.materialbarcodescanner.** { *; }
-keep enum com.edwardvanraak.materialbarcodescanner.** { *; }

-keepattributes LineNumberTable
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes *Annotation*
-keepattributes SourceFile
-keepattributes Deprecated
-keepattributes Exceptions
-keepattributes Signature

##
## http://greenrobot.org/eventbus/documentation/proguard
##
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}

-keep enum org.greenrobot.eventbus.ThreadMode { *; }
-dontnote org.greenrobot.eventbus.ThreadMode

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}