# R8 rules for the release build.
#
# Everything in this app is reached by reflection or by a generated adapter: Gson (models),
# Retrofit (interface proxies), Room (generated implementations), Glide (Generated API calls),
# Compose and the navigation graph. Without these keeps the release build starts and crashes.

# --- Crash reports keep their file names and line numbers -------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*
-keepattributes Signature,InnerClasses,EnclosingMethod

# --- Gson: models and their SerializedName fields ---------------------------------------
-keep class dev.anonymous.eilaji.network.** { *; }
-keep class dev.anonymous.eilaji.models.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn com.google.gson.**

# --- Retrofit / OkHttp -----------------------------------------------------------------
-keep interface dev.anonymous.eilaji.network.ApiService { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Room ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * { @androidx.room.* <methods>; }
-dontwarn androidx.room.paging.**

# --- Glide -----------------------------------------------------------------------------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; *; }
-dontwarn com.bumptech.glide.**

# --- Google Maps / location ------------------------------------------------------------
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.**

# --- Compose / Kotlin reflection -------------------------------------------------------
-dontwarn androidx.compose.**
-keepclassmembers class kotlin.Metadata { public <methods>; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# --- WebSocket messages decoded by Gson ------------------------------------------------
-keep class dev.anonymous.eilaji.network.websocket.** { *; }

# --- Views inflated from XML (custom views only) ---------------------------------------
-keep class dev.anonymous.eilaji.utils.ConfettiView { *; }
-keep class * extends android.view.View {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# --- Project enums and sealed results --------------------------------------------------
-keep class dev.anonymous.eilaji.storage.enums.** { *; }
-keep class dev.anonymous.eilaji.storage.AppSharedPreferences$Theme { *; }
