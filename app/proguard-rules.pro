# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# kotlinx.serialization keep rules
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep class kotlinx.serialization.** { *; }

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepnames class <2>$$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep JSON data classes in ModelsAndStorage.kt
-keep class com.example.Point { *; }
-keep class com.example.Point$Companion { *; }
-keep class com.example.Point$$serializer { *; }
-keep class com.example.SketchStroke { *; }
-keep class com.example.SketchStroke$Companion { *; }
-keep class com.example.SketchStroke$$serializer { *; }
-keep class com.example.SketchPaperType { *; }
-keep class com.example.ExportFormat { *; }
-keep class com.example.EntryDraft { *; }
-keep class com.example.EntryDraft$Companion { *; }
-keep class com.example.EntryDraft$$serializer { *; }
-keep class com.example.DayEntry { *; }
-keep class com.example.DayEntry$Companion { *; }
-keep class com.example.DayEntry$$serializer { *; }
-keep class com.example.TimeCapsule { *; }
-keep class com.example.TimeCapsule$Companion { *; }
-keep class com.example.TimeCapsule$$serializer { *; }
-keep class com.example.CapsuleDraft { *; }
-keep class com.example.CapsuleDraft$Companion { *; }
-keep class com.example.CapsuleDraft$$serializer { *; }
-keep class com.example.ReminderConfig { *; }
-keep class com.example.ReminderConfig$Companion { *; }
-keep class com.example.ReminderConfig$$serializer { *; }
-keep class com.example.UserSettings { *; }
-keep class com.example.UserSettings$Companion { *; }
-keep class com.example.UserSettings$$serializer { *; }
-keep class com.example.BackupPayload { *; }
-keep class com.example.BackupPayload$Companion { *; }
-keep class com.example.BackupPayload$$serializer { *; }
