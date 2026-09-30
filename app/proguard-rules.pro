# Keep line numbers and annotations for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# Keep data models and Room entities/DAOs
-keep class com.example.data.** { *; }

# Retrofit 2 rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp 3 rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*

# Moshi rules
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *

# AndroidX and Compose rules
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
-dontwarn androidx.**
