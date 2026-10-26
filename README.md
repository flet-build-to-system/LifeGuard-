# LifeGuard — Smart Phone Manager & Digital Self-Control
# حارس الحياة — إدارة ذكية ومكافحة الإدمان الرقمي

تطبيق أندرويد رائد ومتكامل لإدارة الهاتف، مكافحة إدمان وسائل التواصل والمحتوى الإباحي، تحسين العافية الرقمية، وبناء عادات إنتاجية مستدامة.

---

### ✨ أبرز المميزات (Key Features)

1. **لوحة العافية الرقمية (Digital Wellbeing Dashboard)**:
   - مؤشر عافية يومي ذكي (0-100%) مبني على مقاييس الاستخدام الحقيقية.
   - قياس دقيق لوقت الشاشة اليومي باستخدام `PACKAGE_USAGE_STATS`.
   - رسم بياني لأيام الأسبوع يوضح اتجاهات الاستخدام والتغيرات.
   - تقرير ذكي يقدم نصائح عملية مبنية على الأرقام المسجلة دون تنظير أو أحكام.

2. **درع الويب ومكافحة الإباحية (Web Shield & Anti-Pornography)**:
   - فلترة محلية آمنة لحركة DNS باستخدام `VpnService` الرسمية.
   - حجب فوري للنطاقات الإباحية محلياً عبر استجابة `NXDOMAIN`.
   - قائمة حجب مسبقة التجهيز دون الحاجة لاتصال بالإنترنت، مع إمكانية إضافة وحذف نطاقات مخصصة.
   - عداد للمحاولات المحجوبة وضمان تام بعدم تسجيل أو نقل سجل التصفح (Zero-Log).

3. **مكافحة إدمان منصات التواصل (Social Media Addiction Control)**:
   - تخصيص قيود زمنية يومية لكل تطبيق (TikTok, Instagram, YouTube, X, Snapchat, إلخ).
   - وقفة واعية للتنفس (Mindful Breathing Pause): تمرين تنفس مهدئ يسبق فتح التطبيقات المشتتة لكسر الاندفاعية التلقائية.
   - خيار الحظر الكامل بعد استنفاد الوقت وخطة التخفيض التدريجي (10% أسبوعياً).

4. **إدارة الدوافع والسلوكيات القهرية (Impulse & Habit Journal)**:
   - مبني على تقنيات العلاج المعرفي السلوكي (CBT) وخطط النوايا البديلة (Implementation Intentions).
   - تتبع الدوافع والمحفزات النفسية (ملل، توتر، سهر، مماطلة) وتسجيل الانتصارات في مقاومتها.

5. **غرفة التركيز والإنتاجية (Focus Room & Pomodoro)**:
   - جلسات تركيز عميق (25, 45, 60 دقيقة) مع مؤشر دائري أنيق.
   - خدمة واجهة أمامية (Foreground Service) لحساب الوقت بدقة مع إشعارات مباشرة وإيقاف تشغيل في الخلفية.

6. **مركز إدارة الهاتف والعتاد (Device Manager)**:
   - مواصفات حقيقية كاملة: الشركة، الموديل، إصدار النظام، مستوى التحديث الأمني، والذاكرة العشوائية RAM.
   - صحة البطارية الحقيقية: النسبة، درجة الحرارة، الجهد الكهربائي، والحالة.
   - إدارة التخزين وتنظيف الملفات المؤقتة للكاش بأمان.
   - مركز الصلاحيات والخصوصية للتحكم بالوصول وفحص الأمان عبر Play Protect.

---

### 🛠 البنية الهندسية والتقنيات (Architecture & Stack)

- **Language:** Kotlin 2.2.10
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + Clean Architecture (Data, Domain, UI)
- **Local Database:** Room 2.7.0
- **Preferences:** Jetpack DataStore Preferences
- **Concurrency:** Kotlin Coroutines & Flow
- **Network & DNS:** Android `VpnService` with local DNS UDP packet parsing and NXDOMAIN sinkholing
- **Background Operations:** Android Foreground Services with systemExempted & specialUse types
- **Design:** Dark Navy (`#0A0F1D`), Emerald Health (`#10B981`), Gold Accent (`#F59E0B`)

---

### 🚀 خطوات البناء والتشغيل (Build & Run)

1. **التحقق من الكود والبناء:**
   ```bash
   gradle assembleDebug
   ```
2. **تشغيل الاختبارات:**
   ```bash
   gradle test
   ```
