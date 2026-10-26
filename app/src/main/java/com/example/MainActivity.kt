package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.BreathingExerciseOverlay
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.devicemanager.DeviceManagerScreen
import com.example.ui.focus.FocusScreen
import com.example.ui.impulses.ImpulseJournalScreen
import com.example.ui.navigation.Screen
import com.example.ui.social.SocialControlScreen
import com.example.ui.theme.LifeGuardTheme
import com.example.ui.webfilter.WebFilterScreen

class MainActivity : ComponentActivity() {

    private var pendingInterventionApp by mutableStateOf<String?>(null)
    private var pendingDelaySeconds by mutableIntStateOf(10)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleInterventionIntent(intent)

        setContent {
            LifeGuardTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("main_navigation_bar")
                        ) {
                            val items = listOf(
                                Screen.Dashboard,
                                Screen.Social,
                                Screen.WebFilter,
                                Screen.Impulses,
                                Screen.Focus,
                                Screen.DeviceManager
                            )
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.titleAr) },
                                    label = { Text(screen.titleAr) },
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    modifier = Modifier.testTag("nav_item_${screen.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            Screen.Dashboard -> DashboardScreen(
                                onNavigateToSocial = { currentScreen = Screen.Social },
                                onNavigateToWebFilter = { currentScreen = Screen.WebFilter },
                                onNavigateToFocus = { currentScreen = Screen.Focus }
                            )
                            Screen.Social -> SocialControlScreen(
                                onTriggerBreathingDemo = { appName ->
                                    pendingInterventionApp = appName
                                    pendingDelaySeconds = 10
                                }
                            )
                            Screen.WebFilter -> WebFilterScreen()
                            Screen.Impulses -> ImpulseJournalScreen(
                                onTriggerBreathingDemo = { behavior ->
                                    pendingInterventionApp = behavior
                                    pendingDelaySeconds = 10
                                }
                            )
                            Screen.Focus -> FocusScreen()
                            Screen.DeviceManager -> DeviceManagerScreen()
                        }
                    }

                    // Mindful Breathing Fullscreen Overlay if an app was intercepted or demo was requested
                    pendingInterventionApp?.let { appName ->
                        BreathingExerciseOverlay(
                            targetAppName = appName,
                            delaySeconds = pendingDelaySeconds,
                            onDelayCompleted = {
                                pendingInterventionApp = null
                            },
                            onCancel = {
                                pendingInterventionApp = null
                                // Return safely to home if launched externally
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    startActivity(homeIntent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleInterventionIntent(intent)
    }

    private fun handleInterventionIntent(intent: Intent?) {
        if (intent?.getStringExtra("NAVIGATE_TO") == "INTERVENTION") {
            pendingInterventionApp = intent.getStringExtra("APP_NAME") ?: "هذا التطبيق"
            pendingDelaySeconds = intent.getIntExtra("DELAY_SECONDS", 10)
        }
    }
}
