# Add project specific ProGuard rules here.

-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile

# Keep all data models and repositories for Firestore serialization and reflection in Release AAB builds
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

