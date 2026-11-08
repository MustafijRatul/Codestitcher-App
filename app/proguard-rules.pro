# CodeStitcher Proguard rules

# Keep Javascript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebKit and WebChromeClient
-keepattributes JavascriptInterface
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}

# Keep custom views if any
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Missing class warnings
-dontwarn javax.annotation.**
-dontwarn okio.**

