# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Firebase — data classes con @PropertyName
-keepclassmembers class com.lchat.app.data.** {
    @com.google.firebase.firestore.PropertyName <fields>;
    <init>();
    <fields>;
}
-keep class com.lchat.app.data.** { *; }

# Firebase Auth / Firestore / Storage / Functions
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Coil
-keep class coil.** { *; }

# Compose
-keep class androidx.compose.** { *; }

# Mantener info de debug para stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile