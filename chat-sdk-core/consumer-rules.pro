## === GSON ===
## Prevent R8 to replace instances of types that are never instantiated with null
## https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md#troubleshooting-gson
-keep,allowobfuscation,allowoptimization class *, **, **$**, **$**$**, **$**$**, **$**$**$** {
    <init>(...);
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.SerializedName <methods>;
}
