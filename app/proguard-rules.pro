# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.hitomatito.hardwire.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.hitomatito.hardwire.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Adblib (Java classes)
-keep class com.hitomatito.hardwire.adblib.** { *; }

# App models
-keep class com.hitomatito.hardwire.data.model.** { *; }
-keep class com.hitomatito.hardwire.data.chipset.** { *; }
