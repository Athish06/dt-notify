# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class *

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.notivault.**$$serializer { *; }
-keepclassmembers class com.notivault.** {
    *** Companion;
}
-keepclasseswithmembers class com.notivault.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep notification service
-keep class com.notivault.service.NotiVaultListenerService { *; }

# General
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
