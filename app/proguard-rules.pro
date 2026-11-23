# Loaded by LSPosed from META-INF/xposed/java_init.list rather than by app code.
-keep class com.cyzah.circular.hook.runtime.ModuleMain { public <init>(); public *; }
-keep class * extends io.github.libxposed.api.XposedModule { public <init>(); public *; }
# Android components are also rooted by the merged manifest.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod,Signature
# libxposed is compileOnly and provided by the framework in SystemUI.
-dontwarn io.github.libxposed.api.**