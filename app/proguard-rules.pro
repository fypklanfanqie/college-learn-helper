# 知微数学 proguard rules（release 混淆在阶段 6 完善）
-keepattributes *Annotation*

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.zhiwei.math.** { kotlinx.serialization.KSerializer serializer(...); }

# RapidOCR / OpenCV (JNI)
-keep class com.benjaminwan.ocrlibrary.** { *; }
-keep class org.opencv.** { *; }
