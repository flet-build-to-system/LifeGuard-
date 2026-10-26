package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val titleAr: String, val titleEn: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "الرئيسية", "Dashboard", Icons.Default.Dashboard)
    object Social : Screen("social", "التواصل", "Social", Icons.Default.HourglassTop)
    object WebFilter : Screen("webfilter", "درع الويب", "Web Shield", Icons.Default.Shield)
    object Impulses : Screen("impulses", "الدوافع", "Impulses", Icons.Default.Psychology)
    object Focus : Screen("focus", "التركيز", "Focus", Icons.Default.SelfImprovement)
    object DeviceManager : Screen("device", "الجهاز", "Device", Icons.Default.PhoneAndroid)
}
