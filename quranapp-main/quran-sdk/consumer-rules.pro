# Keep Room entities and Kotlin serialization models used by the SDK.
-keep class com.codesteem.quransdk.api.** { *; }
-keep class com.codesteem.quransdk.ui.** { *; }
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
