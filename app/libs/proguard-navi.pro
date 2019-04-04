# BaiduNavi SDK
-dontoptimize
-ignorewarnings
-keeppackagenames com.baidu.**
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,LocalVariable*Table,*Annotation*,Synthetic,EnclosingMethod

-dontwarn com.baidu.**
-dontwarn com.baidu.navisdk.**
-dontwarn com.baidu.navi.**

-keep class com.baidu.** { *; }
-keep interface com.baidu.** { *; }
-keep class com.baidu.**$** { *; }
-keep interface com.baidu.**$** { *; }

-keep class vi.com.gdi.** { *; }

-dontwarn com.google.protobuf.**
-keep class com.google.protobuf.** { *;}
-keep interface com.google.protobuf.** { *;}

-dontwarn com.google.android.support.v4.**
-keep class com.google.android.support.v4.** { *; }
-keep interface com.google.android.support.v4.app.** { *; }
-keep public class * extends com.google.android.support.v4.**
-keep public class * extends com.google.android.support.v4.app.Fragment

