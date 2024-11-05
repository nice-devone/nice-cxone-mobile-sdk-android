# Prevent false-positive unused method removal by the R8 in full mode
-keepclassmembers, allowoptimization, allowobfuscation class
    com.nice.cxonechat.SocketFactoryConfiguration$Companion,
    com.nice.cxonechat.ChatBuilder$Companion,
    com.nice.cxonechat.message.OutboundMessage$Companion
 {
    public *;
}

-keepclassmembers, allowoptimization, allowobfuscation interface
    com.nice.cxonechat.SocketFactoryConfiguration,
    com.nice.cxonechat.ChatBuilder,
    com.nice.cxonechat.message.OutboundMessage
 {
    public static *;
}
