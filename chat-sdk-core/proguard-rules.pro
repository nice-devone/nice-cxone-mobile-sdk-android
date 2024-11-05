## === Public APIs ===
## Keep all public APIs from being removed
-keep class java.lang.Object { *; }
-keep class kotlin.Metadata
-keep,allowoptimization @com.nice.cxonechat.Public class **, **$**, **$**$**, **$**$**, **$**$**$** {
    public <methods>;
    public <fields>;
}
-keep,allowoptimization @com.nice.cxonechat.Public interface **, **$**, **$**$**, **$**$**, **$**$**$** {
    public <methods>;
    public <fields>;
}
-keep,allowoptimization @com.nice.cxonechat.Public enum **, **$**, **$**$**, **$**$**, **$**$**$** {
    public <methods>;
    public <fields>;
}
-keep class **$DefaultImpls,**$**$DefaultImpls,**$**$**$DefaultImpls,**$**$**$**$DefaultImpls {
    *;
}
-keepclasseswithmembers,allowoptimization class **, **$**, **$**$**, **$**$**, **$**$**$** {
    @com.nice.cxonechat.Public public <methods>;
}
-keepclasseswithmembernames class com.nice.cxonechat.ChatInstanceProvider$Listener {
    public <methods>;
}

## === Intrinsics ===
## Remove intrinsic checks and trust the developer with provided values
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkNotNull(***);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
}

## === Attributes ===
## Keep vital attributes from being removed
-keepattributes Exceptions,Signature,Deprecated

## === Packaging ===
## Repackage private/internal classes to our package
-repackageclasses 'com.nice.cxonechat.internal'

## === Suppression ===
-dontwarn java.lang.invoke.StringConcatFactory

## === Serialization ===
## Kotlinx.serialization rules are not effective if the classes are not used with R8 in fullmode
-keepclassmembers class com.nice.cxonechat.internal.model.**$**, com.nice.cxonechat.api.model.**$** {
    kotlinx.serialization.KSerializer serializer();
}
-keep class com.nice.cxonechat.internal.model.**, com.nice.cxonechat.api.model.** {
       public static <1> INSTANCE;
       kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers public class **$$serializer {
    *;
}
