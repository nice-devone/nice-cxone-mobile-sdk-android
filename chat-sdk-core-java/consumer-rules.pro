# Prevent false-positive unused method removal by the R8 in full mode
-keepclassmembers, allowoptimization, allowobfuscation class
    com.nice.cxonechat.api.ChatBuilderJavaInterop,
    com.nice.cxonechat.api.ChatJavaInterop,
    com.nice.cxonechat.api.ChatThreadHandlerJavaInterop,
    com.nice.cxonechat.api.ChatThreadsHandlerJavaInterop,
    com.nice.cxonechat.api.ChatActionHandlerJavaInterop,
    com.nice.cxonechat.api.ChatThreadActionHandlerJavaInterop
 {
    public static *;
}
