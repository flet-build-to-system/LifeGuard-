package com.example.domain.model

data class DeviceSpecs(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatch: String,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val totalInternalStorageBytes: Long,
    val freeInternalStorageBytes: Long
)

data class BatterySpecs(
    val percentage: Int,
    val isCharging: Boolean,
    val temperatureCelsius: Float,
    val voltageMv: Int,
    val healthState: String
)

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val usageTimeTodayMinutes: Long = 0
)

enum class ProtectionProfile(val titleAr: String, val titleEn: String, val descriptionAr: String) {
    GENTLE(
        "حماية لطيفة (تذكيرات ووعي)",
        "Gentle (Reminders & Awareness)",
        "تنبيهات لطيفة ووقفات للتنفس قبل فتح التطبيقات المشتتة دون قيود قسرية."
    ),
    BALANCED(
        "حماية متوازنة (جداول وحدود)",
        "Balanced (Limits & Schedules)",
        "حدود زمنية يومية مع جداول تركيز أثناء الدراسة والعمل والنوم وتمرين التنفس."
    ),
    STRICT(
        "حماية صارمة (انضباط ذاتي عالٍ)",
        "Strict (High Discipline)",
        "حظر فوري للمواقع الإباحية، قفل التطبيقات بعد انتهاء الوقت، وفترة تهدئة إلزامية."
    )
}
